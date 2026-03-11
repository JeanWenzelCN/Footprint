package com.footprint.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.footprint.data.model.Mood
import com.footprint.data.model.TransportType
import kotlin.math.abs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationTrackingService : Service(), AMapLocationListener {

    private var locationClient: AMapLocationClient? = null
    private var locationOption: AMapLocationClientOption? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: com.footprint.data.repository.FootprintRepository
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notificationManager: NotificationManager
    private var _notificationUpdateJob: Job? = null

    // Rate limiting for IO / UI updates
    private var _lastSaveTime: Long = 0
    private var _lastNotifyTime: Long = 0

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking_channel"
        const val ACTION_START_TRACKING = "com.footprint.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.footprint.STOP_TRACKING"
        const val ACTION_PAUSE_TRACKING = "com.footprint.PAUSE_TRACKING"
        const val ACTION_RESUME_TRACKING = "com.footprint.RESUME_TRACKING"

        // Thresholds
        private const val MAX_SPEED_THRESHOLD_MS =
                50.0f // 50 m/s = 180 km/h (Limit for driving/train, rejects teleport)
        private const val MIN_DISTANCE_THRESHOLD_M = 0.5f // Capture even very short movements
        private const val MIN_VALID_LATLNG = 0.1 // Reject 0.0 or near 0.0

        private val _sharedIsTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _sharedIsTracking.asStateFlow()

        private val _sharedIsPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _sharedIsPaused.asStateFlow()

        private val _sharedCurrentLocation = MutableStateFlow<AMapLocation?>(null)
        val currentLocation: StateFlow<AMapLocation?> = _sharedCurrentLocation.asStateFlow()

        private val _totalDistanceTraveled = MutableStateFlow(0.0f)
        val totalDistance: StateFlow<Float> = _totalDistanceTraveled.asStateFlow()

        private var _accumulatedDurationMs: Long = 0
        private var _lastResumeTime: Long = 0

        val totalDurationMs: Long
            get() = _accumulatedDurationMs + if (_sharedIsPaused.value || !_sharedIsTracking.value) 0L else (System.currentTimeMillis() - _lastResumeTime)

        private var _sessionStartTime: Long = 0 // Keep for legacy if needed, but we use resume logic now
        val sessionStartTime: Long
            get() = _sessionStartTime

        private var _lastLocation: AMapLocation? = null

        private val _sharedTrackingPath = MutableStateFlow<List<AMapLocation>>(emptyList())
        val trackingPath: StateFlow<List<AMapLocation>> = _sharedTrackingPath.asStateFlow()

        private val _locationError = MutableStateFlow<String?>(null)
        val locationError: StateFlow<String?> = _locationError.asStateFlow()

        fun clearError() {
            _locationError.value = null
        }

        fun startTracking(context: Context) {
            val intent =
                    Intent(context, LocationTrackingService::class.java).apply {
                        action = ACTION_START_TRACKING
                    }
            context.startService(intent)
        }

        fun stopTracking(context: Context) {
            val intent =
                    Intent(context, LocationTrackingService::class.java).apply {
                        action = ACTION_STOP_TRACKING
                    }
            context.startService(intent)
        }

        fun pauseTracking(context: Context) {
            val intent =
                    Intent(context, LocationTrackingService::class.java).apply {
                        action = ACTION_PAUSE_TRACKING
                    }
            context.startService(intent)
        }

        fun resumeTracking(context: Context) {
            val intent =
                    Intent(context, LocationTrackingService::class.java).apply {
                        action = ACTION_RESUME_TRACKING
                    }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("FootprintLoc", "Service onCreate")
        repository = (application as com.footprint.FootprintApplication).repository
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()

        // Recover persistent state if needed
        val prefs = getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_tracking", false)) {
            Log.d("FootprintLoc", "Recovering tracking state from prefs")
            _sharedIsTracking.value = true
            _totalDistanceTraveled.value = prefs.getFloat("total_distance", 0.0f)
            _sessionStartTime = prefs.getLong("session_start", System.currentTimeMillis())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(CHANNEL_ID, "足迹记录", NotificationManager.IMPORTANCE_LOW)
                            .apply {
                                description = "实时显示步数、距离和位置"
                                setShowBadge(false)
                                enableVibration(false)
                                setSound(null, null)
                            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initLocationClient() {
        if (locationClient != null) return
        try {
            Log.d("FootprintLoc", "Initializing AMap SDK")
            locationClient = AMapLocationClient(applicationContext)
            locationClient?.setLocationListener(this)

            locationOption =
                    AMapLocationClientOption().apply {
                        locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                        interval = 2000L // 2s一次
                        isNeedAddress = true
                        isMockEnable = false
                        isLocationCacheEnable = true
                        isOnceLocation = false
                        isSensorEnable = true
                        isGpsFirst = true
                    }
            locationClient?.setLocationOption(locationOption)
        } catch (e: Exception) {
            Log.e("FootprintLoc", "SDK初始化失败: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == null) {
            // System restart (START_STICKY)
            if (_sharedIsTracking.value) {
                Log.d("FootprintLoc", "Restoring tracking after system kill")
                resumeTrackingFlow()
            }
            return START_STICKY
        }

        when (intent.action) {
            ACTION_START_TRACKING -> {
                _sharedIsPaused.value = false
                _totalDistanceTraveled.value = 0.0f
                _lastLocation = null
                _sessionStartTime = System.currentTimeMillis()
                _lastResumeTime = _sessionStartTime
                _accumulatedDurationMs = 0
                _sharedTrackingPath.value = emptyList()

                getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_tracking", true)
                        .putBoolean("is_paused", false)
                        .putFloat("total_distance", 0.0f)
                        .putLong("session_start", _sessionStartTime)
                        .apply()

                resumeTrackingFlow()
                Log.d("FootprintLoc", "定位服务已启动, Session start: $_sessionStartTime")
            }
            ACTION_PAUSE_TRACKING -> {
                if (_sharedIsTracking.value && !_sharedIsPaused.value) {
                    _sharedIsPaused.value = true
                    _accumulatedDurationMs += (System.currentTimeMillis() - _lastResumeTime)
                    
                    getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_paused", true)
                        .apply()
                        
                    updateNotificationImmediately(
                        _totalDistanceTraveled.value.toInt(),
                        0f,
                        _sharedCurrentLocation.value?.address ?: "已暂停记录"
                    )
                    Log.d("FootprintLoc", "定位服务已暂停")
                }
            }
            ACTION_RESUME_TRACKING -> {
                if (_sharedIsTracking.value && _sharedIsPaused.value) {
                    _sharedIsPaused.value = false
                    _lastResumeTime = System.currentTimeMillis()
                    
                    getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_paused", false)
                        .apply()
                        
                    Log.d("FootprintLoc", "定位服务已恢复")
                }
            }
            // ...
            ACTION_STOP_TRACKING -> {
                locationClient?.stopLocation()
                _sharedIsTracking.value = false
                _notificationUpdateJob?.cancel()

                // Clear persistence
                getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_tracking", false)
                        .apply()

                // Release WakeLock
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
                wakeLock = null

                serviceScope.launch {
                    withContext(NonCancellable) {
                        saveTrackingSessionAsFootprint()
                        locationClient?.disableBackgroundLocation(true)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onLocationChanged(location: AMapLocation?) {
        if (location != null) {
            if (location.errorCode == 0) {
                // Filter 0,0 and Low Accuracy
                if (abs(location.latitude) > MIN_VALID_LATLNG &&
                                abs(location.longitude) > MIN_VALID_LATLNG &&
                                location.accuracy < 500
                ) {
                    val clonedLocation = location.clone()
                    _sharedCurrentLocation.value = clonedLocation
                    _locationError.value = null

                    if (_sharedIsTracking.value && !_sharedIsPaused.value) {
                        var isValidPoint = false
                        val now = System.currentTimeMillis()

                        _lastLocation?.let { lastLoc ->
                            val distance = location.distanceTo(lastLoc)
                            val timeDeltaMs = location.time - lastLoc.time
                            val timeDeltaSec = timeDeltaMs / 1000.0

                            if (timeDeltaSec > 0) {
                                val speed = distance / timeDeltaSec
                                if (speed > MAX_SPEED_THRESHOLD_MS) {
                                    Log.w(
                                            "FootprintLoc",
                                            "Ignored glitch: $distance m in $timeDeltaSec s ($speed m/s)"
                                    )
                                } else if (distance < MIN_DISTANCE_THRESHOLD_M) {
                                    // Too close
                                } else {
                                    isValidPoint = true
                                    _totalDistanceTraveled.value += distance.toFloat()

                                    // Update persistence every 10 meters to avoid overkill but keep
                                    // current
                                    if (_totalDistanceTraveled.value % 10 < 1.0) {
                                        getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
                                                .edit()
                                                .putFloat(
                                                        "total_distance",
                                                        _totalDistanceTraveled.value
                                                )
                                                .apply()
                                    }
                                }
                            }
                        }
                                ?: run {
                                    // First point
                                    isValidPoint = true
                                }

                        if (isValidPoint) {
                            // Rate limit DB saves to once per 2 seconds
                            if (now - _lastSaveTime > 2000) {
                                _lastSaveTime = now
                                serviceScope.launch {
                                    try {
                                        val app =
                                                applicationContext as
                                                        com.footprint.FootprintApplication
                                        app.repository.saveTrackPoint(clonedLocation, _sessionStartTime)
                                    } catch (e: Exception) {
                                        Log.e("FootprintLoc", "Failed to save point: ${e.message}")
                                    }
                                }
                            }

                            // Update Real-time Path
                            _sharedTrackingPath.value = _sharedTrackingPath.value + clonedLocation
                            _lastLocation = clonedLocation

                            // Adaptive Interval
                            updateAdaptiveInterval(location.speed)

                            // Rate limit notification updates to 5s (the background timer also
                            // handles this)
                            if (now - _lastNotifyTime > 5000) {
                                _lastNotifyTime = now
                                updateNotificationImmediately(
                                        _totalDistanceTraveled.value.toInt(),
                                        location.speed,
                                        location.address ?: ""
                                )
                            }
                        }
                    }
                    Log.d(
                            "FootprintLoc",
                            "Location update success: ${location.latitude}, ${location.longitude}, acc: ${location.accuracy}"
                    )
                }
            } else {
                Log.e(
                        "FootprintLoc",
                        "Location Error: ${location.errorCode} - ${location.errorInfo}"
                )
                if (location.errorCode == 7 || location.errorCode == 12) {
                    val userMsg =
                            when (location.errorCode) {
                                7 -> "Key鉴权失败：请检查高德后台包名"
                                12 -> "缺少定位权限：请在设置中授予"
                                else -> "未知定位错误: ${location.errorCode}"
                            }
                    _locationError.value = userMsg
                }
            }
        }
    }

    private fun buildNotification(
            distanceMeters: Int,
            speedMs: Float,
            address: String
    ): Notification {

        val remoteViews =
                android.widget.RemoteViews(
                        packageName,
                        com.footprint.R.layout.notification_tracking
                )

        // Update stats
        val distanceKm = distanceMeters / 1000.0
        val speedKmh = speedMs * 3.6f
        val duration = formatDuration(totalDurationMs)

        remoteViews.setTextViewText(
                com.footprint.R.id.notification_distance,
                "%.3f km".format(distanceKm)
        )
        remoteViews.setTextViewText(
                com.footprint.R.id.notification_speed,
                "%.1f km/h".format(speedKmh)
        )
        remoteViews.setTextViewText(com.footprint.R.id.notification_time, duration)
        remoteViews.setTextViewText(
                com.footprint.R.id.notification_address,
                if (_sharedIsPaused.value) "记录已暂停: $address" else address.ifEmpty { "正在记录轨迹..." }
        )

        val stopIntent =
                Intent(this, LocationTrackingService::class.java).apply {
                    action = ACTION_STOP_TRACKING
                }
        val stopPendingIntent =
                PendingIntent.getService(
                        this,
                        0,
                        stopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val mainIntent = Intent(this, com.footprint.MainActivity::class.java)
        val mainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        mainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setCustomContentView(remoteViews)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(mainPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止记录", stopPendingIntent)

        if (_sharedIsPaused.value) {
            val resumeIntent = Intent(this, LocationTrackingService::class.java).apply { action = ACTION_RESUME_TRACKING }
            val resumePI = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "继续", resumePI)
        } else {
            val pauseIntent = Intent(this, LocationTrackingService::class.java).apply { action = ACTION_PAUSE_TRACKING }
            val pausePI = PendingIntent.getService(this, 3, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "暂停", pausePI)
        }

        return builder.build()
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60))
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    private fun resumeTrackingFlow() {
        // Acquire WakeLock
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock =
                    powerManager.newWakeLock(
                                    PowerManager.PARTIAL_WAKE_LOCK,
                                    "Footprint:TrackingWakeLock"
                            )
                            .apply { acquire() }
        }

        // Start Foreground
        if (ActivityCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notification = buildNotification(_totalDistanceTraveled.value.toInt(), 0f, "")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }

        initLocationClient()
        locationClient?.startLocation()
        _sharedIsTracking.value = true

        // Recover points from database to restore the trace line
        serviceScope.launch {
            try {
                val points =
                        repository.getTrackPointsOnce(_sessionStartTime, System.currentTimeMillis())
                _sharedTrackingPath.value =
                        points.map {
                            AMapLocation("").apply {
                                latitude = it.latitude
                                longitude = it.longitude
                                time = it.timestamp
                            }
                        }
                Log.d("FootprintLoc", "Recovered ${_sharedTrackingPath.value.size} points from DB")
            } catch (e: Exception) {
                Log.e("FootprintLoc", "Failed to recover points: ${e.message}")
            }
        }

        startNotificationUpdates()
    }

    private fun updateAdaptiveInterval(speedMs: Float) {
        val speedKmh = speedMs * 3.6f
        val newInterval =
                when {
                    speedKmh > 30 -> 2000L // 快速移动 (开车/公交): 2s
                    speedKmh > 5 -> 3000L // 正常跑步/骑行: 3s
                    speedKmh > 0.5 -> 4000L // 走路: 4s
                    else -> 10000L // 静止: 10s
                }

        locationOption?.let { currentOption ->
            if (currentOption.interval != newInterval) {
                currentOption.interval = newInterval
                locationClient?.setLocationOption(currentOption)
                Log.d(
                        "FootprintLoc",
                        "Adaptive interval updated to: $newInterval ms (Speed: $speedKmh km/h)"
                )
            }
        }
    }

    private fun startNotificationUpdates() {
        _notificationUpdateJob?.cancel()
        _notificationUpdateJob =
                serviceScope.launch {
                    while (isActive && _sharedIsTracking.value) {
                        delay(2000) // 每2秒更新一次时间，降低负载
                        updateNotificationImmediately(
                                _totalDistanceTraveled.value.toInt(),
                                _sharedCurrentLocation.value?.speed ?: 0f,
                                _sharedCurrentLocation.value?.address ?: ""
                        )
                    }
                }
    }

    private fun updateNotificationImmediately(dist: Int, speed: Float, addr: String) {
        if (!_sharedIsTracking.value) return
        serviceScope.launch {
            try {
                val notification = buildNotification(dist, speed, addr)
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                Log.e("FootprintLoc", "Update notification failed: ${e.message}")
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 当应用被从最近任务栏划掉时，如果是正在记录，可以发送一个广播或通知，或者尝试自启动
        // 对于 START_STICKY 服务，系统会自动重启，但我们可以通过 startForeground 增加优先级
        Log.d("FootprintLoc", "Service onTaskRemoved")
    }

    private suspend fun saveTrackingSessionAsFootprint() {
        val endTime = System.currentTimeMillis()
        val points = repository.getTrackPointsOnce(_sessionStartTime, endTime)

        if (points.size >= 2) {
            val totalDistance = calculateTotalDistance(points)
            val lastLocation = _sharedCurrentLocation.value
            val today = java.time.LocalDate.now()

            val entry =
                    com.footprint.data.model.FootprintEntry(
                            title = "自动追踪",
                            location = lastLocation?.toAddressString() ?: "未知地点",
                            detail =
                                    "通过自动追踪记录：共 ${points.size} 个点，耗时 ${ totalDurationMs / 60000 } 分钟",
                            mood = Mood.RELAXED,
                            tags = listOf("自动追踪"),
                            distanceKm = totalDistance / 1000.0,
                            photos = emptyList(),
                            energyLevel = 5,
                            happenedOn = today,
                            latitude = lastLocation?.latitude,
                            longitude = lastLocation?.longitude,
                            altitude = lastLocation?.altitude,
                            weather = null,
                            temperature = null,
                            transportType = com.footprint.data.model.TransportType.WALK,
                            carbonSavedKg = 0.0,
                            icon = "RunCircle"
                    )
            repository.saveEntry(entry)
            Log.d(
                    "FootprintLoc",
                    "Tracking session saved: ${entry.distanceKm} km, points: ${points.size}"
            )
            _totalDistanceTraveled.value = 0.0f // Reset after saving
        } else {
            Log.d("FootprintLoc", "Track too short, not saving (points: ${points.size})")
        }
    }

    private fun calculateTotalDistance(
            points: List<com.footprint.data.local.TrackPointEntity>
    ): Double {
        var distance = 0.0
        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                    start.latitude,
                    start.longitude,
                    end.latitude,
                    end.longitude,
                    results
            )
            distance += results[0]
        }
        return distance
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        serviceScope.cancel()
        super.onDestroy()
    }
}

// Extension function to convert AMapLocation to address string (if available)
fun AMapLocation.toAddressString(): String {
    return if (!this.address.isNullOrEmpty()) {
        this.address
    } else if (!this.city.isNullOrEmpty() && !this.district.isNullOrEmpty()) {
        "${this.city} ${this.district}"
    } else if (!this.poiName.isNullOrEmpty()) {
        this.poiName
    } else {
        String.format("%.3f, %.3f", this.latitude, this.longitude)
    }
}
