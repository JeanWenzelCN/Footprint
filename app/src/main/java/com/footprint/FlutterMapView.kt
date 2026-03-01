package com.footprint

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.footprint.service.LocationTrackingService
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class FlutterMapView(
        private val context: Context,
        id: Int,
        messenger: BinaryMessenger,
        creationParams: Map<String?, Any?>?
) : PlatformView, MethodChannel.MethodCallHandler {

    private val container = FrameLayout(context)
    private val mapView = MapView(context)
    private var aMap: AMap? = null
    private val channel = MethodChannel(messenger, "com.footprint/amap_$id")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 迷雾覆盖层
    private val fogOverlay = FogOverlayView(context)

    private var isDark: Boolean = false

    // 数据引用
    private var currentPathPoints: List<LatLng> = emptyList()
    private var historyPoints: List<LatLng> = emptyList()

    private var livePolyline: Polyline? = null
    private val markerList = mutableListOf<Marker>()

    init {
        channel.setMethodCallHandler(this)
        mapView.onCreate(Bundle())
        mapView.onResume() // 修复：必须调用 onResume 才能正常渲染地图
        aMap = mapView.map

        // 基础地图配置
        aMap?.apply {
            uiSettings.isMyLocationButtonEnabled = false
            isMyLocationEnabled = true
            // 默认设置为夜间模式以配合液态玻璃风格
            mapType = AMap.MAP_TYPE_NIGHT

            setOnMarkerClickListener { marker ->
                val entryId = marker.snippet?.toLongOrNull()
                if (entryId != null) {
                    channel.invokeMethod("onMarkerClick", entryId)
                }
                true
            }
        }

        container.addView(mapView)
        container.addView(fogOverlay)

        fogOverlay.visibility = View.GONE

        // 1. 监听实时轨迹流 (来自 Kotlin Service)
        scope.launch {
            LocationTrackingService.trackingPath.collectLatest { locations ->
                currentPathPoints = locations.map { LatLng(it.latitude, it.longitude) }
                updateLivePolyline()
                fogOverlay.invalidate()
            }
        }

        // 2. 监听地图相机变化
        aMap?.setOnCameraChangeListener(
                object : AMap.OnCameraChangeListener {
                    override fun onCameraChange(pos: com.amap.api.maps.model.CameraPosition?) {
                        fogOverlay.invalidate()
                    }
                    override fun onCameraChangeFinish(
                            pos: com.amap.api.maps.model.CameraPosition?
                    ) {
                        fogOverlay.invalidate()
                    }
                }
        )
    }

    private fun updateLivePolyline() {
        val map = aMap ?: return
        livePolyline?.remove()
        livePolyline = null
        if (currentPathPoints.isNotEmpty()) {
            livePolyline =
                    map.addPolyline(
                            PolylineOptions()
                                    .addAll(currentPathPoints)
                                    .width(18f)
                                    .color(Color.parseColor("#00FF9F"))
                                    .lineCapType(PolylineOptions.LineCapType.LineCapRound)
                                    .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound)
                                    .zIndex(100f)
                    )
        }
    }

    override fun getView(): View = container

    private fun updateMapStyle(mode: String? = null) {
        val currentMode = mode ?: "STANDARD"
        // 如果是迷雾或热力模式，强制使用夜间模式以配合视觉效果
        // 否则根据应用主题设置地图样式
        aMap?.mapType =
                if (currentMode == "FOG" || currentMode == "HEATMAP" || isDark) {
                    AMap.MAP_TYPE_NIGHT
                } else {
                    AMap.MAP_TYPE_NORMAL
                }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "setFogEnabled" -> {
                val enabled = call.arguments as? Boolean ?: false
                fogOverlay.visibility = if (enabled) View.VISIBLE else View.GONE
                result.success(true)
            }
            "setTheme" -> {
                isDark = call.arguments as? Boolean ?: false
                updateMapStyle()
                result.success(true)
            }
            "setMapMode" -> {
                val mode = call.arguments as? String ?: "STANDARD"
                updateMapStyle(mode)
                fogOverlay.visibility = if (mode == "FOG") View.VISIBLE else View.GONE
                result.success(true)
            }
            "setEntries" -> {
                val entries = call.arguments as? List<Map<String, Any>>
                for (marker in markerList) {
                    marker.remove()
                }
                markerList.clear()

                entries?.forEach { entry ->
                    val lat = entry["latitude"] as? Double
                    val lng = entry["longitude"] as? Double
                    val id = (entry["id"] as? Number)?.toLong()
                    val title = entry["title"] as? String ?: "足迹"

                    if (lat != null && lng != null && id != null) {
                        val marker =
                                aMap?.addMarker(
                                        MarkerOptions()
                                                .position(LatLng(lat, lng))
                                                .title(title)
                                                .snippet(id.toString())
                                                .icon(
                                                        BitmapDescriptorFactory.defaultMarker(
                                                                BitmapDescriptorFactory.HUE_AZURE
                                                        )
                                                )
                                )
                        marker?.let { markerList.add(it) }
                    }
                }
                result.success(true)
            }
            "setHistoryPoints" -> {
                // 用于加载历史数据的接口 (迷雾挖洞)
                val points = call.arguments as? List<Map<String, Double>>
                historyPoints = points?.map { LatLng(it["lat"]!!, it["lng"]!!) } ?: emptyList()
                fogOverlay.invalidate()
                result.success(true)
            }
            "setTrackingPath" -> {
                val points = call.arguments as? List<Map<String, Any>>
                currentPathPoints = points?.mapNotNull { pt ->
                    val lat = (pt["lat"] as? Number)?.toDouble()
                    val lng = (pt["lng"] as? Number)?.toDouble()
                    if (lat != null && lng != null) LatLng(lat, lng) else null
                } ?: emptyList()
                updateLivePolyline()
                fogOverlay.invalidate()
                result.success(true)
            }
            "centerLocation" -> {
                // 1. 检查 Key 是否存在 (从 PreferenceManager 或 ApiKeyManager 获取)
                val apiKey = com.footprint.utils.ApiKeyManager.getApiKey(context)
                if (apiKey.isNullOrBlank()) {
                    result.error("KEY_MISSING", "高德地图 API Key 缺失", "请在设置中配置有效的 API Key 以使用地图功能")
                    return
                }

                // 2. 检查位置权限
                val hasFineLocation =
                        androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasFineLocation) {
                    result.error("PERMISSION_DENIED", "定位权限未授予", "请在系统设置中允许应用访问位置信息")
                    return
                }

                val loc = aMap?.myLocation
                if (loc != null && loc.latitude > 1.0 && loc.longitude > 1.0) {
                    aMap?.animateCamera(
                            com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                    LatLng(loc.latitude, loc.longitude),
                                    17f
                            )
                    )
                    result.success(true)
                } else {
                    val trackingLoc = LocationTrackingService.currentLocation.value
                    if (trackingLoc != null &&
                                    trackingLoc.latitude > 1.0 &&
                                    trackingLoc.longitude > 1.0
                    ) {
                        aMap?.animateCamera(
                                com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                        LatLng(trackingLoc.latitude, trackingLoc.longitude),
                                        17f
                                )
                        )
                        result.success(true)
                    } else {
                        // 如果都没有位置信息，返回错误提示
                        result.error(
                                "LOCATION_UNAVAILABLE",
                                "获取位置失败",
                                "目前无法获取定位，请确保 GPS 已开启并位于室外开阔地带"
                        )
                    }
                }
            }
            else -> result.notImplemented()
        }
    }

    override fun dispose() {
        scope.cancel()
        channel.setMethodCallHandler(null)
        mapView.onPause()
        mapView.onDestroy()
    }

    // --- 高性能迷雾渲染类 ---
    inner class FogOverlayView(context: Context) : View(context) {
        private val fogPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E60F172A") // 90% 透明度的深蓝色迷雾
                }

        private val holePaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }

        private val circlePaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                    style = Paint.Style.FILL
                }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val map = aMap ?: return
            val projection = map.projection ?: return

            // 使用离屏缓冲实现 DstOut 混合效果
            val sc = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

            // 1. 画背景迷雾
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fogPaint)

            // 2. 计算动态挖洞半径 (根据缩放等级调整)
            val zoom = map.cameraPosition.zoom
            val radius = (zoom * 2.0).coerceIn(20.0, 150.0).toFloat()

            holePaint.strokeWidth = radius * 1.5f

            // 3. 绘制实时轨迹路径挖洞
            if (currentPathPoints.isNotEmpty()) {
                val path = Path()
                var first = true
                currentPathPoints.forEach { latLng ->
                    val screenPos = projection.toScreenLocation(latLng)
                    if (first) {
                        path.moveTo(screenPos.x.toFloat(), screenPos.y.toFloat())
                        first = false
                    } else {
                        path.lineTo(screenPos.x.toFloat(), screenPos.y.toFloat())
                    }
                }
                canvas.drawPath(path, holePaint)
            }

            // 4. 绘制历史点（使用圆点打洞）
            historyPoints.forEach { latLng ->
                val screenPos = projection.toScreenLocation(latLng)
                canvas.drawCircle(
                        screenPos.x.toFloat(),
                        screenPos.y.toFloat(),
                        radius * 2.0f,
                        circlePaint
                )
            }

            canvas.restoreToCount(sc)
        }
    }
}
