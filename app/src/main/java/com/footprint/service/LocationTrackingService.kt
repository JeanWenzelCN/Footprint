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

    private var _totalDistanceTraveled = MutableStateFlow(0.0f)
    private var _lastLocation: AMapLocation? = null
    private var _sessionStartTime: Long = 0
    private var _notificationUpdateJob: Job? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking_channel"
        const val ACTION_START_TRACKING = "com.footprint.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.footprint.STOP_TRACKING"

        // Thresholds
        private const val MAX_SPEED_THRESHOLD_MS =
                50.0f // 50 m/s = 180 km/h (Limit for driving/train, rejects teleport)
        private const val MIN_DISTANCE_THRESHOLD_M = 5.0f // Ignore drift < 5m
        private const val MIN_VALID_LATLNG = 0.1 // Reject 0.0 or near 0.0

        private val _sharedIsTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _sharedIsTracking.asStateFlow()

        private val _sharedCurrentLocation = MutableStateFlow<AMapLocation?>(null)
        val currentLocation: StateFlow<AMapLocation?> = _sharedCurrentLocation.asStateFlow()

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
            // Always start as a regular service first. The service will promote itself to
            // foreground.
            context.startService(intent)
        }

        fun stopTracking(context: Context) {
            val intent =
                    Intent(context, LocationTrackingService::class.java).apply {
                        action = ACTION_STOP_TRACKING
                    }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = (application as com.footprint.FootprintApplication).repository
        initLocationClient()

        serviceScope.launch {
            val app = applicationContext as com.footprint.FootprintApplication
            val startOfDay =
                    java.time.LocalDate.now()
                            .atStartOfDay(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()

            app.repository.getTrackPoints(startOfDay, Long.MAX_VALUE).collect { points ->
                val locations =
                        points.map { entity ->
                            AMapLocation("gps").apply {
                                latitude = entity.latitude
                                longitude = entity.longitude
                                speed = entity.speed
                                accuracy = entity.accuracy
                                altitude = entity.altitude
                                time = entity.timestamp
                            }
                        }
                _sharedTrackingPath.value = locations
            }
        }
    }

    private fun initLocationClient() {
        try {
            AMapLocationClient.updatePrivacyShow(applicationContext, true, true)
            AMapLocationClient.updatePrivacyAgree(applicationContext, true)

            locationClient = AMapLocationClient(applicationContext)
            locationClient?.setLocationListener(this)

            locationOption =
                    AMapLocationClientOption().apply {
                        locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                        interval = 5000L // 初始间隔 5秒
                        isNeedAddress = true
                        isMockEnable = false
                        isLocationCacheEnable = false
                        isOnceLocation = false
                        isSensorEnable = true
                        isGpsFirst = true
                        // 开启高德 SDK 自带的后台定位能力
                        locationClient?.enableBackgroundLocation(
                                NOTIFICATION_ID,
                                buildNotification(0, 0f, "")
                        )
                    }
            locationClient?.setLocationOption(locationOption)
        } catch (e: Exception) {
            Log.e("FootprintLoc", "SDK初始化失败: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                _totalDistanceTraveled.value = 0.0f
                _lastLocation = null
                _sessionStartTime = System.currentTimeMillis()

                // Acquire WakeLock
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock =
                        powerManager.newWakeLock(
                                        PowerManager.PARTIAL_WAKE_LOCK,
                                        "Footprint:TrackingWakeLock"
                                )
                                .apply { acquire() }

                // Android 14+ requires runtime permission check before calling startForeground with
                // location type
                if (ActivityCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(
                                        this,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e(
                            "FootprintLoc",
                            "Cannot start foreground service: Location permission missing"
                    )
                    stopSelf()
                    return START_NOT_STICKY
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                            NOTIFICATION_ID,
                            buildNotification(0, 0f, ""),
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification(0, 0f, ""))
                }
                locationClient?.startLocation()
                _sharedIsTracking.value = true
                startNotificationUpdates()
                Log.d("FootprintLoc", "定位服务已启动, Session start: $_sessionStartTime")
            }
            ACTION_STOP_TRACKING -> {
                locationClient?.stopLocation()
                _sharedIsTracking.value = false
                _notificationUpdateJob?.cancel()

                // Release WakeLock
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
                wakeLock = null

                serviceScope.launch { saveTrackingSessionAsFootprint() }
                locationClient?.disableBackgroundLocation(true)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onLocationChanged(location: AMapLocation?) {
        if (location != null) {
            if (location.errorCode == 0) {
                // 彻底解决非洲 0,0 坐标问题：只有在经纬度有效且精度合理时才更新
                // update: using abs to allow valid negative coordinates, but reject near-zero
                if (abs(location.latitude) > MIN_VALID_LATLNG &&
                                abs(location.longitude) > MIN_VALID_LATLNG &&
                                location.accuracy < 200
                ) {
                    _sharedCurrentLocation.value = location
                    _locationError.value = null // Clear previous errors
                    if (_sharedIsTracking.value) {

                        var isValidPoint = false

                        _lastLocation?.let { lastLoc ->
                            val distance = location.distanceTo(lastLoc)
                            val timeDeltaMs = location.time - lastLoc.time
                            val timeDeltaSec = timeDeltaMs / 1000.0

                            // Speed Sanity Check (Anti-Teleport)
                            // If timeDelta is too small (e.g. same second), assume duplication or
                            // glitch unless distance is 0
                            var speed = 0.0
                            if (timeDeltaSec > 0) {
                                speed = distance / timeDeltaSec
                            }

                            if (distance > 0 && timeDeltaSec <= 0) {
                                Log.w(
                                        "FootprintLoc",
                                        "Duplicate timestamp or negative time. Ignoring."
                                )
                                return // Ignore this point
                            }

                            // Filter:
                            // 1. Teleport glitch: Speed > MAX (unless it's the very first points
                            // initializing)
                            // 2. Drift: Distance < MIN
                            if (speed > MAX_SPEED_THRESHOLD_MS) {
                                Log.w(
                                        "FootprintLoc",
                                        "Ignored glitch: $distance m in $timeDeltaSec s ($speed m/s)"
                                )
                            } else if (distance < MIN_DISTANCE_THRESHOLD_M) {
                                // Too close, probably drift
                                // Log.v("FootprintLoc", "Ignored drift: $distance m")
                            } else {
                                // Valid movement
                                isValidPoint = true
                                _totalDistanceTraveled.value += distance
                                Log.d(
                                        "FootprintLoc",
                                        "Distance added: $distance, Total: ${_totalDistanceTraveled.value}"
                                )
                            }
                        }
                                ?: run {
                                    // First point
                                    isValidPoint = true
                                }

                        if (isValidPoint) {
                            // 持久化存储 (DB)
                            serviceScope.launch {
                                try {
                                    val app =
                                            applicationContext as com.footprint.FootprintApplication
                                    app.repository.saveTrackPoint(location)
                                } catch (e: Exception) {
                                    Log.e("FootprintLoc", "Failed to save point: ${e.message}")
                                }
                            }
                            _lastLocation = location // Update last location ONLY if valid

                            // Adaptive Interval: Adjust frequency based on speed (m/s)
                            updateAdaptiveInterval(location.speed)

                            // Update notification with all stats
                            val notification =
                                    buildNotification(
                                            _totalDistanceTraveled.value.toInt(),
                                            location.speed,
                                            location.address ?: ""
                                    )
                            val manager = getSystemService(NotificationManager::class.java)
                            manager.notify(NOTIFICATION_ID, notification)
                        }
                    }
                    Log.d(
                            "FootprintLoc",
                            "坐标获取成功: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}"
                    )
                }
            } else {
                val errText = "定位错误: ${location.errorCode} - ${location.errorInfo}"
                Log.e("FootprintLoc", errText)

                // 仅针对需要用户干预的关键错误弹 Toast (7=Key鉴权失败, 12=缺权限)
                // 忽略错误 10 (网络/GPS不稳定)，避免在弱网环境下频繁弹窗打扰用户
                if (location.errorCode == 7 || location.errorCode == 12) {
                    val userMsg =
                            when (location.errorCode) {
                                7 -> "Key鉴权失败：请检查高德后台包名是否为 com.footprint"
                                12 -> "缺少定位权限：请在设置中授予权限"
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
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(CHANNEL_ID, "足迹记录", NotificationManager.IMPORTANCE_DEFAULT)
                            .apply {
                                description = "实时显示步数、距离和位置"
                                setShowBadge(false)
                            }
            manager.createNotificationChannel(channel)
        }

        val remoteViews =
                android.widget.RemoteViews(
                        packageName,
                        com.footprint.R.layout.notification_tracking
                )

        // Update stats
        val distanceKm = distanceMeters / 1000.0
        val speedKmh = speedMs * 3.6f
        val elapsedMs = System.currentTimeMillis() - _sessionStartTime
        val duration = formatDuration(elapsedMs)

        remoteViews.setTextViewText(
                com.footprint.R.id.notification_distance,
                "%.2f km".format(distanceKm)
        )
        remoteViews.setTextViewText(
                com.footprint.R.id.notification_speed,
                "%.1f km/h".format(speedKmh)
        )
        remoteViews.setTextViewText(com.footprint.R.id.notification_time, duration)
        remoteViews.setTextViewText(
                com.footprint.R.id.notification_address,
                address.ifEmpty { "正在记录轨迹..." }
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setCustomContentView(remoteViews)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(mainPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止记录", stopPendingIntent)
                .build()
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60))
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    private fun updateAdaptiveInterval(speedMs: Float) {
        val speedKmh = speedMs * 3.6f
        val newInterval =
                when {
                    speedKmh > 30 -> 2000L // 快速移动 (开车/公交): 2s
                    speedKmh > 5 -> 5000L // 正常跑步/骑行: 5s
                    speedKmh > 0.5 -> 10000L // 走路: 10s
                    else -> 30000L // 静止: 30s
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
                        delay(1000) // 每秒更新一次时间
                        val notification =
                                buildNotification(
                                        _totalDistanceTraveled.value.toInt(),
                                        _sharedCurrentLocation.value?.speed ?: 0f,
                                        _sharedCurrentLocation.value?.address ?: ""
                                )
                        val manager = getSystemService(NotificationManager::class.java)
                        manager.notify(NOTIFICATION_ID, notification)
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
                                    "通过自动追踪记录：共 ${points.size} 个点，耗时 ${ (endTime - _sessionStartTime) / 60000 } 分钟",
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
        "(${this.latitude}, ${this.longitude})"
    }
}
