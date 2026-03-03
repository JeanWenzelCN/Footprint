package com.footprint

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.amap.api.maps.model.MyLocationStyle
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

    // 缓存原生地图最新的定位坐标（通过 OnMyLocationChangeListener 实时更新）
    private var cachedLat: Double = 0.0
    private var cachedLng: Double = 0.0

    // 数据引用
    private var currentPathPoints: List<LatLng> = emptyList()
    private var historyPoints: List<LatLng> = emptyList()

    private var livePolyline: Polyline? = null
    private var historyPolyline: Polyline? = null
    private val markerList = mutableListOf<Marker>()

    init {
        channel.setMethodCallHandler(this)
        mapView.onCreate(Bundle())
        mapView.onResume() // 修复：必须调用 onResume 才能正常渲染地图
        aMap = mapView.map

        // 基础地图配置
        aMap?.apply {
            uiSettings.apply {
                isMyLocationButtonEnabled = false
                isZoomControlsEnabled = false
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = false
            }
            isMyLocationEnabled = false
            // 默认设置为夜间模式以配合液态玻璃风格
            mapType = AMap.MAP_TYPE_NIGHT

            // 配置定位蓝点样式：保留默认箭头图标，精度圈稍小一些
            val locationStyle =
                    MyLocationStyle()
                            .myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                            .radiusFillColor(Color.parseColor("#1A4FC3F7")) // 浅蓝填充（10%不透明度）
                            .strokeColor(Color.parseColor("#404FC3F7")) // 蓝色边框（25%不透明度）
                            .strokeWidth(1f)
            myLocationStyle = locationStyle

            // === 关键：监听原生地图的位置变化，缓存最新坐标 ===
            // aMap?.myLocation 在异步获取位置前可能返回 (0,0)
            // 通过 listener 可以在蓝点显示的同时拿到真实坐标
            setOnMyLocationChangeListener { location ->
                if (location.latitude > 1.0 && location.longitude > 1.0) {
                    cachedLat = location.latitude
                    cachedLng = location.longitude
                }
            }

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
            val prefs = com.footprint.utils.PreferenceManager(context)
            val liveColorHex =
                    when (prefs.artColorStyle) {
                        "Deep Blue" -> "#007AFF"
                        "Cyber Pink" -> "#FF2D55"
                        "Neon Green" -> "#00FF9F"
                        "Gold" -> "#FFCC00"
                        else -> "#00FF9F"
                    }
            livePolyline =
                    map.addPolyline(
                            PolylineOptions()
                                    .addAll(currentPathPoints)
                                    .width(18f)
                                    .color(Color.parseColor(liveColorHex))
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
                    val lat = (entry["latitude"] as? Number)?.toDouble()
                    val lng = (entry["longitude"] as? Number)?.toDouble()
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
                
                // Automatically register all map entries as historical locations to dispel fog
                historyPoints = entries?.mapNotNull { 
                    val lat = (it["latitude"] as? Number)?.toDouble()
                    val lng = (it["longitude"] as? Number)?.toDouble()
                    if (lat != null && lng != null) LatLng(lat, lng) else null
                } ?: emptyList()
                
                fogOverlay.invalidate()
                result.success(true)
            }
            "setHistoryPoints" -> {
                // 用于加载历史数据的接口 (迷雾挖洞)
                val points = call.arguments as? List<Map<String, Any>>
                historyPoints =
                        points?.mapNotNull {
                            val lat =
                                    (it["lat"] as? Number)?.toDouble()
                                            ?: (it["latitude"] as? Number)?.toDouble()
                            val lng =
                                    (it["lng"] as? Number)?.toDouble()
                                            ?: (it["longitude"] as? Number)?.toDouble()
                            if (lat != null && lng != null) LatLng(lat, lng) else null
                        }
                                ?: emptyList()
                fogOverlay.invalidate()
                result.success(true)
            }
            "setTrackingPath" -> {
                val points = call.arguments as? List<Map<String, Any>>
                val latLngPoints =
                        points?.mapNotNull {
                            val lat =
                                    (it["lat"] as? Number)?.toDouble()
                                            ?: (it["latitude"] as? Number)?.toDouble()
                            val lng =
                                    (it["lng"] as? Number)?.toDouble()
                                            ?: (it["longitude"] as? Number)?.toDouble()
                            if (lat != null && lng != null) LatLng(lat, lng) else null
                        }
                                ?: emptyList()

                historyPolyline?.remove()
                historyPolyline = null

                if (latLngPoints.isNotEmpty()) {
                    val prefs = com.footprint.utils.PreferenceManager(context)
                    val historyColorHex =
                            when (prefs.artColorStyle) {
                                "Deep Blue" -> "#007AFF"
                                "Cyber Pink" -> "#FF2D55"
                                "Neon Green" -> "#00FF9F"
                                "Gold" -> "#FFCC00"
                                else -> "#42A5F5"
                            }
                    historyPolyline =
                            aMap?.addPolyline(
                                    PolylineOptions()
                                            .addAll(latLngPoints)
                                            .width(18f)
                                            .color(Color.parseColor(historyColorHex))
                                            .lineCapType(PolylineOptions.LineCapType.LineCapRound)
                                            .lineJoinType(
                                                    PolylineOptions.LineJoinType.LineJoinRound
                                            )
                                            .zIndex(90f)
                            )
                }
                result.success(true)
            }
            "centerLocation" -> {
                // 优先使用 Flutter 传入的坐标
                val args = call.arguments
                var lat: Double? = null
                var lng: Double? = null
                var zoomLevel = 17f
                if (args is Map<*, *>) {
                    lat = (args["latitude"] as? Number)?.toDouble()
                    lng = (args["longitude"] as? Number)?.toDouble()
                    zoomLevel = (args["zoom"] as? Number)?.toFloat() ?: 17f
                }

                // 1. Flutter 传入的坐标
                if (lat != null && lng != null && lat > 1.0 && lng > 1.0) {
                    aMap?.animateCamera(
                            com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                    LatLng(lat, lng),
                                    zoomLevel
                            )
                    )
                    result.success(true)
                    return
                }

                // 2. 使用缓存的位置坐标（来自 OnMyLocationChangeListener）
                if (cachedLat > 1.0 && cachedLng > 1.0) {
                    aMap?.animateCamera(
                            com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                    LatLng(cachedLat, cachedLng),
                                    zoomLevel
                            )
                    )
                    result.success(true)
                    return
                }

                // 3. AMap 自身的 myLocation 属性
                val loc = aMap?.myLocation
                if (loc != null && loc.latitude > 1.0 && loc.longitude > 1.0) {
                    aMap?.animateCamera(
                            com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                    LatLng(loc.latitude, loc.longitude),
                                    zoomLevel
                            )
                    )
                    result.success(true)
                    return
                }

                // 3. Kotlin 追踪服务的位置（兼容旧逻辑）
                val trackingLoc = LocationTrackingService.currentLocation.value
                if (trackingLoc != null && trackingLoc.latitude > 1.0 && trackingLoc.longitude > 1.0
                ) {
                    aMap?.animateCamera(
                            com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                    LatLng(trackingLoc.latitude, trackingLoc.longitude),
                                    zoomLevel
                            )
                    )
                    result.success(true)
                    return
                }

                // 都没有位置信息
                result.error("LOCATION_UNAVAILABLE", "获取位置失败", "目前无法获取定位，请确保 GPS 已开启并位于室外开阔地带")
            }
            "setLocationEnabled" -> {
                val enabled = call.arguments as? Boolean ?: false
                aMap?.isMyLocationEnabled = enabled
                result.success(true)
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

    // --- 基于程序化纹理的体积云迷雾系统 (Procedural Volumetric Fog) ---
    inner class FogOverlayView(context: Context) : View(context) {
        private var isAnimating = false
        private val cloudMatrix1 = Matrix()
        private val cloudMatrix2 = Matrix()
        private var fogGradientShader: LinearGradient? = null

        private val baseFogPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val cloudPaint1 = Paint(Paint.ANTI_ALIAS_FLAG)
        private val cloudPaint2 = Paint(Paint.ANTI_ALIAS_FLAG)

        private val pathEraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
            alpha = 200
        }

        private val spotEraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            style = Paint.Style.FILL
        }

        init {
            // 初始化生成两层无缝云雾柏林噪声纹理 (利用不同种子和缩放创建差值，产生翻滚感)
            val tex1 = createSeamlessCloudTexture(512, 42L, "#55E2E8F0") // 浅蓝亮部
            val tex2 = createSeamlessCloudTexture(512, 108L, "#4094A3B8") // 灰蓝暗部

            cloudPaint1.shader = BitmapShader(tex1, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            cloudPaint2.shader = BitmapShader(tex2, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }

        private fun createSeamlessCloudTexture(size: Int, seed: Long, colorHex: String): Bitmap {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.parseColor(colorHex)
            paint.maskFilter = BlurMaskFilter(size / 6f, BlurMaskFilter.Blur.NORMAL)

            val rnd = java.util.Random(seed)
            for (i in 0 until 50) {
                val cx = rnd.nextFloat() * size
                val cy = rnd.nextFloat() * size
                val r = rnd.nextFloat() * (size / 4f) + (size / 10f)

                // 9宫格绘制保证边缘纹理无缝拼接 (Seamless Tiling)
                for (dx in listOf(-size, 0, size)) {
                    for (dy in listOf(-size, 0, size)) {
                        canvas.drawCircle(cx + dx.toFloat(), cy + dy.toFloat(), r, paint)
                    }
                }
            }
            return bitmap
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            isAnimating = true
            postInvalidateOnAnimation()
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            isAnimating = false
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w == 0 || h == 0) return

            // 基础深空背景光照
            fogGradientShader = LinearGradient(0f, 0f, 0f, h.toFloat(),
                intArrayOf(Color.parseColor("#E60F172A"), Color.parseColor("#F21E293B")),
                null, Shader.TileMode.CLAMP)
            baseFogPaint.shader = fogGradientShader
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val map = aMap ?: return
            val projection = map.projection ?: return

            if (width == 0 || height == 0) return

            val time = System.currentTimeMillis()
            
            // 矩阵偏移计算，使云层移动
            val offset1X = (time % 120000L) / 120000f * 512f
            val offset1Y = (time % 150000L) / 150000f * 512f
            cloudMatrix1.reset()
            cloudMatrix1.setScale(2.5f, 2.5f) // 放大云朵细节
            cloudMatrix1.postTranslate(-offset1X, -offset1Y)
            cloudPaint1.shader.setLocalMatrix(cloudMatrix1)

            val offset2X = (time % 180000L) / 180000f * 512f
            val offset2Y = (time % 100000L) / 100000f * 512f
            cloudMatrix2.reset()
            cloudMatrix2.setScale(3.5f, 3.5f) // 第二层云不同比例，交叉混合产生涌动(Churning)
            cloudMatrix2.postTranslate(offset2X, -offset2Y)
            cloudPaint2.shader.setLocalMatrix(cloudMatrix2)

            // 使用离屏缓冲实现各种叠加及挖洞
            val sc = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

            // 1. 绘制底层基础迷雾环境
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), baseFogPaint)

            // 2. 叠加体积云纹理层 (利用透明度混合出真正的体积云质感)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), cloudPaint1)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), cloudPaint2)

            val zoom = map.cameraPosition.zoom
            val baseRadius = (zoom * 3.5).coerceIn(50.0, 250.0).toFloat()

            // 3. 实时轨迹的高斯边缘消除 (物理侵蚀感)
            if (currentPathPoints.isNotEmpty()) {
                pathEraserPaint.strokeWidth = baseRadius * 1.8f
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
                canvas.drawPath(path, pathEraserPaint)
            }

            // 4. 计算探索掩码的动态驱散 (Dynamic Erosion)
            historyPoints.forEach { latLng ->
                val screenPos = projection.toScreenLocation(latLng)
                // 只渲染视野内部的点
                if (screenPos.x >= -baseRadius*4 && screenPos.x <= width + baseRadius*4 &&
                    screenPos.y >= -baseRadius*4 && screenPos.y <= height + baseRadius*4) {
                    
                    // 利用坐标Hash和时间计算独立呼吸波长，让边缘呈现动态侵蚀涌动效果
                    val phase = latLng.latitude * 1000.0 + latLng.longitude * 1000.0
                    val breatheErosion = Math.sin(time / 1500.0 + phase).toFloat() * 0.15f
                    val dynamicRadius = baseRadius * 4.0f * (1.0f + breatheErosion)

                    spotEraserPaint.shader = RadialGradient(
                        screenPos.x.toFloat(), screenPos.y.toFloat(), dynamicRadius,
                        intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                        floatArrayOf(0f, 0.35f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    
                    canvas.drawCircle(
                        screenPos.x.toFloat(),
                        screenPos.y.toFloat(),
                        dynamicRadius,
                        spotEraserPaint
                    )
                }
            }

            canvas.restoreToCount(sc)

            if (isAnimating && visibility == VISIBLE) {
                postInvalidateOnAnimation() // 以显示器刷新率循环重绘实现60fps动画
            }
        }
    }


}
