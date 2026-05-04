package com.footprint.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.footprint.data.model.Mood
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
    private lateinit var notificationManager: NotificationManager
    private var _notificationUpdateJob: Job? = null
    private val trackingPathBuffer = mutableListOf<AMapLocation>()
    private val pendingTrackPoints = mutableListOf<AMapLocation>()

    // Rate limiting for IO / UI updates
    private var _lastSaveTime: Long = 0
    private var _lastNotifyTime: Long = 0
    private var _lastDistancePersist: Float = 0.0f
    private var _lastTrackingMode: TrackingMode? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking_channel"
        const val ACTION_START_TRACKING = "com.footprint.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.footprint.STOP_TRACKING"
        const val ACTION_PAUSE_TRACKING = "com.footprint.PAUSE_TRACKING"
        const val ACTION_RESUME_TRACKING = "com.footprint.RESUME_TRACKING"
        const val ACTION_RESTORE_TRACKING = "com.footprint.RESTORE_TRACKING"

        // Thresholds
        private const val MAX_SPEED_THRESHOLD_MS =
                50.0f // 50 m/s = 180 km/h (Limit for driving/train, rejects teleport)
        private const val MIN_DISTANCE_THRESHOLD_M = 0.5f // Capture even very short movements
        private const val MIN_VALID_LATLNG = 0.1 // Reject 0.0 or near 0.0
        private const val BASE_INTERVAL_MS = 10000L
        private const val STATIONARY_INTERVAL_MS = 60000L
        private const val WALKING_INTERVAL_MS = 10000L
        private const val MOVING_INTERVAL_MS = 5000L
        private const val FAST_MOVING_INTERVAL_MS = 3000L
        private const val TRACK_BATCH_WINDOW_MS = 20000L
        private const val TRACK_BATCH_SIZE = 5
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 60000L
        private const val DISTANCE_PERSIST_STEP_M = 50.0f

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
            get() {
                if (!_sharedIsTracking.value) return 0L
                val currentSegment = if (_sharedIsPaused.value) 0L else (System.currentTimeMillis() - _lastResumeTime)
                return _accumulatedDurationMs + currentSegment
            }

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
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopTracking(context: Context) {
            val intent =
                    Intent(context, LocationTrackingService::class.java).apply {
                        action = ACTION_STOP_TRACKING
                    }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseTracking(context: Context) {
            val intent =
                    Intent(context, LocationTrackingService::class.java).apply {
                        action = ACTION_PAUSE_TRACKING
                    }
            ContextCompat.startForegroundService(context, intent)
        }

        fun resumeTracking(context: Context) {
            val intent =
                    Intent(context, LocationTrackingService::class.java).apply {
                        action = ACTION_RESUME_TRACKING
                    }
            ContextCompat.startForegroundService(context, intent)
        }

        fun restoreTracking(context: Context) {
            val intent =
                    Intent(context, LocationTrackingService::class.java).apply {
                        action = ACTION_RESTORE_TRACKING
                    }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private enum class TrackingMode(
            val intervalMs: Long,
            val locationMode: AMapLocationClientOption.AMapLocationMode
    ) {
        STATIONARY(
                STATIONARY_INTERVAL_MS,
                AMapLocationClientOption.AMapLocationMode.Battery_Saving
        ),
        WALKING(
                WALKING_INTERVAL_MS,
                AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        ),
        MOVING(
                MOVING_INTERVAL_MS,
                AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        ),
        FAST(
                FAST_MOVING_INTERVAL_MS,
                AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("FootprintLoc", "Service onCreate")
        repository = (application as com.footprint.FootprintApplication).repository
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()

        // 禁止在 onCreate 时自动恢复 is_tracking = true，
        // 只有通过显式的 ACTION_START_TRACKING 或 ACTION_RESUME_TRACKING 才会开启。
        // 这解决了“一打开地图就自动记录”的问题。
        _sharedIsTracking.value = false
        _sharedIsPaused.value = false
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
                        interval = BASE_INTERVAL_MS
                        isNeedAddress = false
                        isMockEnable = false
                        isLocationCacheEnable = true
                        isOnceLocation = false
                        isSensorEnable = false
                        isGpsFirst = false
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
                _lastSaveTime = 0L
                _lastNotifyTime = 0L
                _lastDistancePersist = 0.0f
                _lastTrackingMode = null
                trackingPathBuffer.clear()
                pendingTrackPoints.clear()
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
            ACTION_RESTORE_TRACKING -> {
                restoreTrackingStateFromPrefs()
                if (_sharedIsTracking.value) {
                    resumeTrackingFlow()
                    Log.d("FootprintLoc", "定位服务已按持久化状态恢复")
                } else {
                    stopSelf()
                }
            }
            ACTION_PAUSE_TRACKING -> {
                if (_sharedIsTracking.value && !_sharedIsPaused.value) {
                    _sharedIsPaused.value = true
                    _accumulatedDurationMs += (System.currentTimeMillis() - _lastResumeTime)
                    locationClient?.stopLocation()
                    flushPendingTrackPointsAsync()
                    persistTrackingSnapshot(forceDistance = true)
                    
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
                    _lastNotifyTime = 0L
                    locationClient?.startLocation()
                    
                    getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_paused", false)
                        .apply()
                        
                    Log.d("FootprintLoc", "定位服务已恢复")
                }
            }
            // ...
            ACTION_STOP_TRACKING -> {
                val finalDurationMs =
                        if (_sharedIsPaused.value) {
                            _accumulatedDurationMs
                        } else {
                            _accumulatedDurationMs +
                                    (System.currentTimeMillis() - _lastResumeTime)
                        }
                locationClient?.stopLocation()
                _notificationUpdateJob?.cancel()
                persistTrackingSnapshot(forceDistance = true)
                _sharedIsTracking.value = false

                // Clear persistence
                getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_tracking", false)
                        .apply()

                serviceScope.launch {
                    withContext(NonCancellable) {
                        flushPendingTrackPointsSync()
                        saveTrackingSessionAsFootprint(finalDurationMs)
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
                                }
                            }
                        }
                                ?: run {
                                    // First point
                                    isValidPoint = true
                                }

                        if (isValidPoint) {
                            queueTrackPoint(clonedLocation, now)

                            // Update Real-time Path
                            trackingPathBuffer.add(clonedLocation)
                            _sharedTrackingPath.value = trackingPathBuffer.toList()
                            _lastLocation = clonedLocation

                            // Adaptive Interval and accuracy profile
                            updateAdaptiveTrackingProfile(location.speed)

                            persistTrackingSnapshot()

                            // Rate limit notification updates to reduce background IPC/work.
                            if (now - _lastNotifyTime > NOTIFICATION_UPDATE_INTERVAL_MS) {
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

    private fun restoreTrackingStateFromPrefs() {
        val prefs = getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
        val isTracking = prefs.getBoolean("is_tracking", false)
        val isPaused = prefs.getBoolean("is_paused", false)

        _sharedIsTracking.value = isTracking
        _sharedIsPaused.value = isPaused

        if (!isTracking) {
            _totalDistanceTraveled.value = 0.0f
            _sessionStartTime = 0L
            _accumulatedDurationMs = 0L
            _lastResumeTime = 0L
            return
        }

        _sessionStartTime = prefs.getLong("session_start", System.currentTimeMillis())
        _totalDistanceTraveled.value = prefs.getFloat("total_distance", 0.0f)
        _lastDistancePersist = _totalDistanceTraveled.value
        val now = System.currentTimeMillis()
        _accumulatedDurationMs = if (isPaused) 0L else (now - _sessionStartTime).coerceAtLeast(0L)
        _lastResumeTime = if (isPaused) 0L else now
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
        _sharedIsTracking.value = true

        // Recover points from database to restore the trace line
        serviceScope.launch {
            try {
                val points =
                        repository.getTrackPointsOnce(_sessionStartTime, System.currentTimeMillis())
                trackingPathBuffer.clear()
                trackingPathBuffer.addAll(
                        points.map {
                            AMapLocation("").apply {
                                latitude = it.latitude
                                longitude = it.longitude
                                time = it.timestamp
                            }
                        }
                )
                _sharedTrackingPath.value = trackingPathBuffer.toList()
                Log.d("FootprintLoc", "Recovered ${_sharedTrackingPath.value.size} points from DB")
            } catch (e: Exception) {
                Log.e("FootprintLoc", "Failed to recover points: ${e.message}")
            }
        }

        if (!_sharedIsPaused.value) {
            applyTrackingMode(TrackingMode.WALKING)
            locationClient?.startLocation()
        }
        startNotificationUpdates()
    }

    private fun updateAdaptiveTrackingProfile(speedMs: Float) {
        val speedKmh = speedMs * 3.6f
        val newMode =
                when {
                    speedKmh > 30 -> TrackingMode.FAST
                    speedKmh > 8 -> TrackingMode.MOVING
                    speedKmh > 1.5 -> TrackingMode.WALKING
                    else -> TrackingMode.STATIONARY
                }
        applyTrackingMode(newMode)
    }

    private fun applyTrackingMode(mode: TrackingMode) {
        if (_lastTrackingMode == mode) return
        locationOption?.let { currentOption ->
            currentOption.interval = mode.intervalMs
            currentOption.locationMode = mode.locationMode
            locationClient?.setLocationOption(currentOption)
            _lastTrackingMode = mode
            Log.d(
                    "FootprintLoc",
                    "Tracking profile updated: ${mode.name}, interval=${mode.intervalMs}ms"
            )
        }
    }

    private fun queueTrackPoint(location: AMapLocation, now: Long) {
        pendingTrackPoints.add(location)
        if (pendingTrackPoints.size >= TRACK_BATCH_SIZE ||
                        now - _lastSaveTime >= TRACK_BATCH_WINDOW_MS
        ) {
            flushPendingTrackPointsAsync()
            _lastSaveTime = now
        }
    }

    private fun flushPendingTrackPointsAsync() {
        if (pendingTrackPoints.isEmpty()) return
        val snapshot = pendingTrackPoints.toList()
        pendingTrackPoints.clear()
        serviceScope.launch {
            try {
                repository.saveTrackPoints(snapshot, _sessionStartTime)
            } catch (e: Exception) {
                Log.e("FootprintLoc", "Failed to flush points: ${e.message}")
                pendingTrackPoints.addAll(0, snapshot)
            }
        }
    }

    private suspend fun flushPendingTrackPointsSync() {
        if (pendingTrackPoints.isEmpty()) return
        val snapshot = pendingTrackPoints.toList()
        pendingTrackPoints.clear()
        try {
            repository.saveTrackPoints(snapshot, _sessionStartTime)
        } catch (e: Exception) {
            pendingTrackPoints.addAll(0, snapshot)
            throw e
        }
    }

    private fun persistTrackingSnapshot(forceDistance: Boolean = false) {
        if (!_sharedIsTracking.value) return
        val distance = _totalDistanceTraveled.value
        if (!forceDistance && distance - _lastDistancePersist < DISTANCE_PERSIST_STEP_M) return
        _lastDistancePersist = distance
        getSharedPreferences("tracking_prefs", Context.MODE_PRIVATE)
                .edit()
                .putFloat("total_distance", distance)
                .putLong("session_start", _sessionStartTime)
                .apply()
    }

    private fun startNotificationUpdates() {
        _notificationUpdateJob?.cancel()
        _notificationUpdateJob =
                serviceScope.launch {
                    while (isActive && _sharedIsTracking.value) {
                        delay(NOTIFICATION_UPDATE_INTERVAL_MS)
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

    private suspend fun saveTrackingSessionAsFootprint(durationMs: Long) {
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
                                    "通过自动追踪记录：共 ${points.size} 个点，耗时 ${durationMs / 60000} 分钟",
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
        runBlocking(NonCancellable) {
            flushPendingTrackPointsSync()
        }
        trackingPathBuffer.clear()
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
