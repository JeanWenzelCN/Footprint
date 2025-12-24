package com.footprint.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import android.content.pm.ServiceInfo

/**
 * 后台位置追踪服务
 * 使用前台服务持续获取用户位置信息
 */
class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isTracking = MutableStateFlow(false)
    private val _currentLocation = MutableStateFlow<Location?>(null)
    private val _trackingPath = MutableStateFlow<List<Location>>(emptyList())

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "location_tracking_channel"
        const val CHANNEL_NAME = "位置追踪"

        const val ACTION_START_TRACKING = "com.footprint.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.footprint.STOP_TRACKING"

        private val _sharedIsTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _sharedIsTracking.asStateFlow()

        private val _sharedCurrentLocation = MutableStateFlow<Location?>(null)
        val currentLocation: StateFlow<Location?> = _sharedCurrentLocation.asStateFlow()

        private val _sharedTrackingPath = MutableStateFlow<List<Location>>(emptyList())
        val trackingPath: StateFlow<List<Location>> = _sharedTrackingPath.asStateFlow()

        fun startTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startLocationTracking()
            ACTION_STOP_TRACKING -> stopLocationTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于后台持续追踪位置"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(locationCount: Int = 0): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            putExtra("destination", "map")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("足迹正在记录")
            .setContentText("已记录 $locationCount 个位置点")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    updateLocation(location)
                }
            }
        }
    }

    private fun startLocationTracking() {
        if (_isTracking.value) return

        // 检查权限
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        // 启动前台服务
        val notification = buildNotification(0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 配置位置请求
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // 每5秒更新一次
        ).apply {
            setMinUpdateIntervalMillis(2000L) // 最快2秒更新一次
            setMaxUpdateDelayMillis(10000L)
        }.build()

        // 开始请求位置更新
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        _isTracking.value = true
        _sharedIsTracking.value = true

        // 清空之前的轨迹
        _trackingPath.value = emptyList()
        _sharedTrackingPath.value = emptyList()
    }

    private fun stopLocationTracking() {
        if (!_isTracking.value) {
            stopSelf()
            return
        }

        fusedLocationClient.removeLocationUpdates(locationCallback)
        _isTracking.value = false
        _sharedIsTracking.value = false

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateLocation(location: Location) {
        _currentLocation.value = location
        _sharedCurrentLocation.value = location

        // 添加到轨迹路径
        val currentPath = _trackingPath.value.toMutableList()
        currentPath.add(location)
        _trackingPath.value = currentPath
        _sharedTrackingPath.value = currentPath

        // 更新通知
        val notification = buildNotification(currentPath.size)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (_isTracking.value) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        _isTracking.value = false
        _sharedIsTracking.value = false
    }
}
