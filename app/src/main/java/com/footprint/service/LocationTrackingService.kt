package com.footprint.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.footprint.data.model.Mood
import com.footprint.data.model.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationTrackingService : Service(), AMapLocationListener {

    private var locationClient: AMapLocationClient? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: com.footprint.data.repository.FootprintRepository

    private var _totalDistanceTraveled = MutableStateFlow(0.0f)
    private var _lastLocation: AMapLocation? = null
    
    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking_channel"
        const val ACTION_START_TRACKING = "com.footprint.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.footprint.STOP_TRACKING"

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
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            // Always start as a regular service first. The service will promote itself to foreground.
            context.startService(intent)
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
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
            val startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            app.repository.getTrackPoints(startOfDay, Long.MAX_VALUE).collect { points ->
                val locations = points.map { entity ->
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
            
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                interval = 2000L // 2秒刷新一次，更灵敏
                isNeedAddress = true
                isMockEnable = true
                isLocationCacheEnable = false // 禁用缓存，强制获取最新位置
                isOnceLocation = false
            }
            locationClient?.setLocationOption(option)
        } catch (e: Exception) {
            Log.e("FootprintLoc", "SDK初始化失败: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> {
                _totalDistanceTraveled.value = 0.0f
                _lastLocation = null
                startForeground(NOTIFICATION_ID, buildNotification(0))
                locationClient?.startLocation()
                _sharedIsTracking.value = true
                Log.d("FootprintLoc", "定位服务已启动")
            }
            ACTION_STOP_TRACKING -> {
                locationClient?.stopLocation()
                _sharedIsTracking.value = false
                serviceScope.launch {
                    saveTrackingSessionAsFootprint()
                }
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onLocationChanged(location: AMapLocation?) {
        if (location != null) {
            if (location.errorCode == 0) {
                // 彻底解决非洲 0,0 坐标问题：只有在经纬度有效且精度合理时才更新
                if (location.latitude > 1.0 && location.longitude > 1.0) {
                    _sharedCurrentLocation.value = location
                    _locationError.value = null // Clear previous errors
                    if (_sharedIsTracking.value) {
                        // Calculate distance
                        _lastLocation?.let { lastLoc ->
                            val distance = location.distanceTo(lastLoc)
                            _totalDistanceTraveled.value += distance
                            Log.d("FootprintLoc", "Distance added: $distance, Total: ${_totalDistanceTraveled.value}")
                        }
                        _lastLocation = location

                        // Update notification with distance
                        startForeground(NOTIFICATION_ID, buildNotification(_totalDistanceTraveled.value.toInt()))

                        // 2. 持久化存储 (DB) - UI会在Flow收集器中自动更新
                        serviceScope.launch {
                            try {
                                val app = applicationContext as com.footprint.FootprintApplication
                                app.repository.saveTrackPoint(location)
                            } catch (e: Exception) {
                                Log.e("FootprintLoc", "Failed to save point: ${e.message}")
                            }
                        }
                    }
                    Log.d("FootprintLoc", "坐标获取成功: ${location.latitude}, ${location.longitude}")
                }
            } else {
                val errText = "定位错误: ${location.errorCode} - ${location.errorInfo}"
                Log.e("FootprintLoc", errText)
                
                // 仅针对需要用户干预的关键错误弹 Toast (7=Key鉴权失败, 12=缺权限)
                // 忽略错误 10 (网络/GPS不稳定)，避免在弱网环境下频繁弹窗打扰用户
                if (location.errorCode == 7 || location.errorCode == 12) {
                    val userMsg = when (location.errorCode) {
                        7 -> "Key鉴权失败：请检查高德后台包名是否为 com.footprint"
                        12 -> "缺少定位权限：请在设置中授予权限"
                        else -> "未知定位错误: ${location.errorCode}"
                    }
                    _locationError.value = userMsg
                }
            }
        }
    }

    private fun buildNotification(distanceKm: Int): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "足迹记录", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val text = if (distanceKm > 0) "已记录 ${"%.2f".format(distanceKm / 1000.0)} 公里" else "正在后台记录你的轨迹..."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在探索世界")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private suspend fun saveTrackingSessionAsFootprint() {
        if (_totalDistanceTraveled.value > 0) {
            val lastLocation = _sharedCurrentLocation.value
            val today = java.time.LocalDate.now()

            val entry = com.footprint.data.model.FootprintEntry(
                title = "自动追踪",
                location = lastLocation?.toAddressString() ?: "未知地点",
                detail = "通过自动追踪功能记录的行程",
                mood = Mood.RELAXED, // Default mood
                tags = listOf("自动追踪"),
                distanceKm = (_totalDistanceTraveled.value / 1000.0), // Convert meters to kilometers
                photos = emptyList(),
                energyLevel = 5, // Default energy level
                happenedOn = today,
                latitude = lastLocation?.latitude,
                longitude = lastLocation?.longitude,
                altitude = lastLocation?.altitude,
                weather = null,
                temperature = null,
                transportType = com.footprint.data.model.TransportType.WALK, // Default transport type
                carbonSavedKg = 0.0,
                icon = "RunCircle" // Default icon
            )
            repository.saveEntry(entry)
            Log.d("FootprintLoc", "Tracking session saved as FootprintEntry: ${entry.distanceKm} km")
            _totalDistanceTraveled.value = 0.0f // Reset after saving
        }
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