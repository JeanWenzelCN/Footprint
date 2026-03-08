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
        messenger: BinaryMessenger
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
    private var heatmapOverlay: TileOverlay? = null
    private val markerList = mutableListOf<Marker>()
    private val capsuleMarkers = mutableListOf<Marker>()
    private var rawCapsules: List<Map<*, *>> = emptyList()
    private var rawEntries: List<Map<*, *>> = emptyList()
    private var currentMode: String = "STANDARD"

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
                val id = marker.snippet?.toLongOrNull()
                if (id != null) {
                    if (marker.title == "CAPSULE") {
                        channel.invokeMethod("onCapsuleClick", id)
                    } else {
                        channel.invokeMethod("onMarkerClick", id)
                    }
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
                fogOverlay.updateLivePathMercatorCache()
                updateLivePolyline()
                if (heatmapOverlay != null) updateHeatmap()
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
            val pathColor = getPathColor(true)
            livePolyline =
                    map.addPolyline(
                            PolylineOptions()
                                    .addAll(currentPathPoints)
                                    .width(if (currentMode == "CAPSULE") 15f else 18f)
                                    .color(pathColor)
                                    .lineCapType(PolylineOptions.LineCapType.LineCapRound)
                                    .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound)
                                    .zIndex(100f)
                    )
        }
    }

    private fun getPathColor(isLive: Boolean): Int {
        if (currentMode == "CAPSULE") {
            return Color.parseColor(if (isLive) "#00E5FF" else "#01579B")
        }
        val prefs = com.footprint.utils.PreferenceManager(context)
        val hex = when (prefs.artColorStyle) {
            "Deep Blue" -> "#007AFF"
            "Cyber Pink" -> "#FF2D55"
            "Neon Green" -> "#00FF9F"
            "Gold" -> "#FFCC00"
            else -> if (isLive) "#00FF9F" else "#42A5F5"
        }
        return Color.parseColor(hex)
    }

    override fun getView(): View = container

    private fun updateMapStyle(mode: String? = null) {
        val modeToUse = mode ?: currentMode
        // 如果是迷雾、热力或胶囊模式，强制使用夜间模式以配合视觉效果
        aMap?.mapType =
                if (modeToUse == "FOG" || modeToUse == "HEATMAP" || modeToUse == "CAPSULE" || isDark) {
                    AMap.MAP_TYPE_NIGHT
                } else {
                    AMap.MAP_TYPE_NORMAL
                }
        
        // 胶囊模式下可以显示路况图层来增加现代感
        aMap?.isTrafficEnabled = (modeToUse == "CAPSULE")
    }

    private fun updateMarkers() {
        updateEntryMarkers()
        updateCapsuleMarkers()
    }

    private fun updateEntryMarkers() {
        markerList.forEach { it.remove() }
        markerList.clear()

        // 热力图模式下不显示点位标记，让视觉焦点在于密集度
        if (currentMode == "HEATMAP") return

        rawEntries.forEach { it ->
            val entry = it as? Map<*, *> ?: return@forEach
            val lat = (entry["latitude"] as? Number)?.toDouble()
            val lng = (entry["longitude"] as? Number)?.toDouble()
            val id = (entry["id"] as? Number)?.toLong()
            val title = entry["title"] as? String ?: "足迹"

            if (lat != null && lng != null && id != null) {
                val options = MarkerOptions().position(LatLng(lat, lng)).title(title).snippet(id.toString())
                
                if (currentMode == "CAPSULE") {
                    // 胶囊模式下，普通足迹点显示为发光的小圆点
                    options.anchor(0.5f, 0.5f)
                    options.icon(BitmapDescriptorFactory.fromBitmap(createNeonDotBitmap(Color.parseColor("#00B0FF"))))
                } else {
                    // 标准/迷雾模式下使用默认图钉
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                }
                
                val marker = aMap?.addMarker(options)
                marker?.let { markerList.add(it) }
            }
        }
    }

    private fun updateCapsuleMarkers() {
        capsuleMarkers.forEach { it.remove() }
        capsuleMarkers.clear()

        // 胶囊只在“胶囊模式”或“标准模式”下显示，但在胶囊模式下更显著
        if (currentMode != "CAPSULE" && currentMode != "STANDARD") return

        rawCapsules.forEach { data ->
            val id = (data["id"] as? Number)?.toLong() ?: return@forEach
            val lat = (data["latitude"] as? Number)?.toDouble() ?: return@forEach
            val lng = (data["longitude"] as? Number)?.toDouble() ?: return@forEach
            val message = data["message"] as? String ?: "时光胶囊"
            val isUnlocked = data["isUnlocked"] as? Boolean ?: false

            val options = MarkerOptions().position(LatLng(lat, lng))
                    .title("CAPSULE")
                    .snippet(id.toString())
            
            if (currentMode == "CAPSULE") {
                // 胶囊模式下显示大型设计款胶囊
                options.anchor(0.5f, 0.5f)
                options.icon(BitmapDescriptorFactory.fromBitmap(createCapsuleBitmap(message, isUnlocked)))
            } else {
                // 标准模式下显示缩小版的胶囊图标
                options.anchor(0.5f, 0.5f)
                options.icon(BitmapDescriptorFactory.fromBitmap(createSmallCapsuleBitmap(isUnlocked)))
            }
            
            val marker = aMap?.addMarker(options)
            marker?.let { capsuleMarkers.add(it) }
        }
    }

    private fun createCapsuleBitmap(message: String, isUnlocked: Boolean): Bitmap {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 30f
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
        }
        val displayMessage = if (message.length > 8) message.take(7) + "..." else message
        val textWidth = textPaint.measureText(displayMessage)
        val h = 70f
        val padding = 35f
        val w = textWidth + h + padding 
        
        val bitmap = Bitmap.createBitmap(w.toInt() + 20, h.toInt() + 20, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Glow effect
        paint.setShadowLayer(12f, 0f, 0f, if (isUnlocked) Color.parseColor("#00E5FF") else Color.GRAY)
        
        // Background
        paint.color = if (isUnlocked) Color.parseColor("#0091EA") else Color.parseColor("#424242")
        val rect = RectF(10f, 10f, w + 10f, h + 10f)
        canvas.drawRoundRect(rect, h / 2, h / 2, paint)

        // Icon area
        paint.setShadowLayer(0f, 0f, 0f, 0)
        paint.color = Color.WHITE
        paint.alpha = 60
        canvas.drawCircle(h / 2 + 10f, h / 2 + 10f, h / 2 - 8f, paint)
        
        // Text
        canvas.drawText(displayMessage, h + 15f, h / 2 + 10f - (textPaint.descent() + textPaint.ascent()) / 2, textPaint)
        
        return bitmap
    }

    private fun createSmallCapsuleBitmap(isUnlocked: Boolean): Bitmap {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        paint.color = if (isUnlocked) Color.parseColor("#0091EA") else Color.parseColor("#757575")
        val rect = RectF(4f, 12f, size - 4f, size - 12f)
        canvas.drawRoundRect(rect, size / 2f, size / 2f, paint)
        
        paint.color = Color.WHITE
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(rect, size / 2f, size / 2f, paint)
        
        return bitmap
    }

    private fun createNeonDotBitmap(color: Int): Bitmap {
        val size = 40
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        paint.setShadowLayer(10f, 0f, 0f, color)
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, 8f, paint)
        
        paint.setShadowLayer(0f, 0f, 0f, 0)
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, 4f, paint)
        
        return bitmap
    }

    private fun updateHeatmap() {
        val map = aMap ?: return
        heatmapOverlay?.remove()
        heatmapOverlay = null

        val allPoints = mutableListOf<LatLng>()
        allPoints.addAll(historyPoints)
        allPoints.addAll(currentPathPoints)

        if (allPoints.isEmpty()) return

        try {
            val builder = HeatmapTileProvider.Builder()
            builder.data(allPoints)
            // 自定义渐变色，使其更具设计感（深蓝->青->绿->黄->橙红）
            val gradient = Gradient(
                intArrayOf(
                    Color.argb(0, 0, 122, 255),    // 透明
                    Color.rgb(102, 204, 255),      // 淡蓝
                    Color.rgb(102, 255, 204),      // 青
                    Color.rgb(255, 255, 102),      // 黄
                    Color.rgb(255, 87, 34)         // 橙红
                ),
                floatArrayOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f)
            )
            builder.gradient(gradient)
            builder.radius(45) // 增加半径使热力效果更丝滑
            builder.transparency(0.15) // 设置透明度（0为不透明，1为全透明）

            val provider = builder.build()
            heatmapOverlay = map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
        } catch (e: Exception) {
            e.printStackTrace()
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
                currentMode = mode
                updateMapStyle(mode)
                updateMarkers()
                fogOverlay.visibility = if (mode == "FOG") View.VISIBLE else View.GONE
                
                if (mode == "HEATMAP") {
                    updateHeatmap()
                } else {
                    heatmapOverlay?.remove()
                    heatmapOverlay = null
                }
                result.success(true)
            }
            "setEntries" -> {
                @Suppress("UNCHECKED_CAST")
                rawEntries = call.arguments as? List<Map<*, *>> ?: emptyList()
                updateEntryMarkers()
                result.success(true)
            }
            "setCapsules" -> {
                @Suppress("UNCHECKED_CAST")
                rawCapsules = call.arguments as? List<Map<*, *>> ?: emptyList()
                updateCapsuleMarkers()
                result.success(true)
            }
            "setHistoryPoints" -> {
                // 用于加载历史数据的接口 (同时用于迷雾挖洞和历史轨迹线绘制)
                val points = call.arguments as? List<*>
                val latLngPoints = points?.mapNotNull {
                    val itMap = it as? Map<*, *> ?: return@mapNotNull null
                    val lat = (itMap["lat"] as? Number)?.toDouble() ?: (itMap["latitude"] as? Number)?.toDouble()
                    val lng = (itMap["lng"] as? Number)?.toDouble() ?: (itMap["longitude"] as? Number)?.toDouble()
                    if (lat != null && lng != null) LatLng(lat, lng) else null
                } ?: emptyList()

                // 更新迷雾挖洞点位
                historyPoints = latLngPoints
                fogOverlay.updateHistoryMercatorCache()
                fogOverlay.invalidate()

                // 更新热力图（如果开启）
                if (heatmapOverlay != null) updateHeatmap()

                // 更新历史轨迹线
                historyPolyline?.remove()
                historyPolyline = null
                if (latLngPoints.isNotEmpty()) {
                    val pathColor = getPathColor(false)
                    historyPolyline = aMap?.addPolyline(
                        PolylineOptions()
                            .addAll(latLngPoints)
                            .width(if (currentMode == "CAPSULE") 15f else 18f)
                            .color(pathColor)
                            .lineCapType(PolylineOptions.LineCapType.LineCapRound)
                            .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound)
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

        // AGSL Shader Support (API 33+)
        private var runtimeShader: android.graphics.RuntimeShader? = null
        private val agslPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var maskBitmap: Bitmap? = null
        private var maskCanvas: Canvas? = null

        private val pathEraserPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    alpha = 200
                }

        private val spotEraserPaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                    style = Paint.Style.FILL
                }

        private var agslGradient: RadialGradient? = null
        private val agslGradientMatrix = Matrix()

        private var fallbackGradient: RadialGradient? = null
        private val fallbackGradientMatrix = Matrix()

        private val tempSpot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
        }

        private val tempEraser = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = Color.BLACK
        }
        
        private var lastBlurRadius: Float = -1f
        private var cachedBlurFilter: BlurMaskFilter? = null
        
        // --- 高性能 C++ 矩阵映射缓存机制 ---
        private val mercatorLivePath = Path()
        private val scratchingScreenPath = Path()
        private var historyMercatorCoords = FloatArray(0)
        private var screenHistoryScratch = FloatArray(0)
        private val mercatorToScreenMatrix = Matrix()
        
        private fun latLngToMercatorX(lng: Double): Float {
            return (lng / 360.0 + 0.5).toFloat()
        }

        private fun latLngToMercatorY(lat: Double): Float {
            val sinY = Math.sin(lat * Math.PI / 180.0).coerceIn(-0.9999, 0.9999)
            return (0.5 - Math.log((1 + sinY) / (1 - sinY)) / (4 * Math.PI)).toFloat()
        }

        fun updateLivePathMercatorCache() {
            mercatorLivePath.reset()
            var first = true
            currentPathPoints.forEach { pt ->
                val mx = latLngToMercatorX(pt.longitude)
                val my = latLngToMercatorY(pt.latitude)
                if (first) {
                    mercatorLivePath.moveTo(mx, my)
                    first = false
                } else {
                    mercatorLivePath.lineTo(mx, my)
                }
            }
        }

        fun updateHistoryMercatorCache() {
            if (historyMercatorCoords.size != historyPoints.size * 2) {
                historyMercatorCoords = FloatArray(historyPoints.size * 2)
            }
            var i = 0
            historyPoints.forEach { pt ->
                historyMercatorCoords[i++] = latLngToMercatorX(pt.longitude)
                historyMercatorCoords[i++] = latLngToMercatorY(pt.latitude)
            }
        }

        init {
            agslGradient = RadialGradient(
                0f, 0f, 1f,
                intArrayOf(Color.BLACK, Color.rgb(150, 150, 150), Color.WHITE),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            fallbackGradient = RadialGradient(
                0f, 0f, 1f,
                intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                floatArrayOf(0f, 0.35f, 1f),
                Shader.TileMode.CLAMP
            )

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Initialize AGSL Shader for API 33+
                try {
                    runtimeShader =
                            android.graphics.RuntimeShader(
                                    com.footprint.ui.effects.VOLUMETRIC_FOG_SHADER
                            )
                    agslPaint.shader = runtimeShader
                } catch (e: Exception) {
                    e.printStackTrace()
                    setupFallbackShaders()
                }
            } else {
                setupFallbackShaders()
            }
        }

        private fun setupFallbackShaders() {
            // 初始化生成两层无缝云雾柏林噪声纹理 (Fallback)
            val tex1 = createSeamlessCloudTexture(512, 42L, "#55E2E8F0")
            val tex2 = createSeamlessCloudTexture(512, 108L, "#4094A3B8")

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
            maskBitmap?.recycle()
            maskBitmap = null
            maskCanvas = null
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w == 0 || h == 0) return

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                            runtimeShader != null
            ) {
                maskBitmap?.recycle()
                maskBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                maskCanvas = Canvas(maskBitmap!!)
                maskCanvas?.drawColor(Color.WHITE) // Initialize fog mask as fully enveloped (white)
            }

            // 基础深空背景光照
            fogGradientShader =
                    LinearGradient(
                            0f,
                            0f,
                            0f,
                            h.toFloat(),
                            intArrayOf(
                                    Color.parseColor("#E60F172A"),
                                    Color.parseColor("#F21E293B")
                            ),
                            null,
                            Shader.TileMode.CLAMP
                    )
            baseFogPaint.shader = fogGradientShader
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val map = aMap ?: return
            val projection = map.projection ?: return

            if (width == 0 || height == 0) return

            val time = System.currentTimeMillis()

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                            runtimeShader != null &&
                            maskCanvas != null &&
                            maskBitmap != null
            ) {
                // ---- AGSL SHADER PATH (Native Volumetric Cloud) ----

                // 构建高精度仿射变换矩阵，1次计算全局映射
                val center = map.cameraPosition.target
                val offsetScale = Math.pow(2.0, (17.0 - map.cameraPosition.zoom).toDouble()).toFloat()
                val offDeg = 0.001f * offsetScale
                val p0 = projection.toScreenLocation(center)
                val p1 = projection.toScreenLocation(LatLng(center.latitude + offDeg, center.longitude))
                val p2 = projection.toScreenLocation(LatLng(center.latitude, center.longitude + offDeg))
                
                val src = floatArrayOf(
                    latLngToMercatorX(center.longitude), latLngToMercatorY(center.latitude),
                    latLngToMercatorX(center.longitude), latLngToMercatorY(center.latitude + offDeg),
                    latLngToMercatorX(center.longitude + offDeg), latLngToMercatorY(center.latitude)
                )
                val dst = floatArrayOf(
                    p0.x.toFloat(), p0.y.toFloat(),
                    p1.x.toFloat(), p1.y.toFloat(),
                    p2.x.toFloat(), p2.y.toFloat()
                )
                mercatorToScreenMatrix.setPolyToPoly(src, 0, dst, 0, 3)

                // ---- AGSL SHADER PATH (Native Volumetric Cloud) ----
                // 1. Prepare Exploration Mask
                maskCanvas?.drawColor(Color.WHITE) // fully enshrouded

                val zoom = map.cameraPosition.zoom
                val baseRadius = (zoom * 3.5).coerceIn(50.0, 250.0).toFloat()
                val dynamicRadius = baseRadius * 4.5f

                val requiredBlur = baseRadius * 1.8f
                if (Math.abs(lastBlurRadius - requiredBlur) > 2f) {
                    cachedBlurFilter = BlurMaskFilter(requiredBlur, BlurMaskFilter.Blur.NORMAL)
                    lastBlurRadius = requiredBlur
                }

                // Draw Path into Mask
                if (!mercatorLivePath.isEmpty) {
                    tempEraser.maskFilter = cachedBlurFilter
                    tempEraser.strokeWidth = baseRadius * 1.8f
                    scratchingScreenPath.reset()
                    scratchingScreenPath.addPath(mercatorLivePath)
                    scratchingScreenPath.transform(mercatorToScreenMatrix)
                    maskCanvas?.drawPath(scratchingScreenPath, tempEraser)
                }

                // Draw History Spots into Mask
                if (historyMercatorCoords.isNotEmpty()) {
                    if (screenHistoryScratch.size < historyMercatorCoords.size) {
                        screenHistoryScratch = FloatArray(historyMercatorCoords.size)
                    }
                    mercatorToScreenMatrix.mapPoints(screenHistoryScratch, historyMercatorCoords)
                    val count = historyPoints.size * 2
                    var i = 0
                    while (i < count) {
                        val sx = screenHistoryScratch[i]
                        val sy = screenHistoryScratch[i + 1]
                        if (sx >= -dynamicRadius && sx <= width + dynamicRadius && sy >= -dynamicRadius && sy <= height + dynamicRadius) {
                            agslGradient?.let { grad ->
                                agslGradientMatrix.setScale(dynamicRadius, dynamicRadius)
                                agslGradientMatrix.postTranslate(sx, sy)
                                grad.setLocalMatrix(agslGradientMatrix)
                                tempSpot.shader = grad
                                maskCanvas?.drawCircle(sx, sy, dynamicRadius, tempSpot)
                            }
                        }
                        i += 2
                    }
                }

                // 2. Configure Shader Uniforms
                val maskShader =
                        BitmapShader(maskBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                runtimeShader?.setInputShader("maskTexture", maskShader)
                runtimeShader?.setFloatUniform("uResolution", width.toFloat(), height.toFloat())
                runtimeShader?.setFloatUniform("uTime", (time % 10000000L).toFloat() / 1000f)

                val zoomFactor =
                        Math.pow(2.0, (17.0 - map.cameraPosition.zoom).toDouble()).toFloat()
                val target = map.cameraPosition.target
                val mercatorX = target.longitude / 360.0 + 0.5
                val sinY = Math.sin(target.latitude * Math.PI / 180.0).coerceIn(-0.9999, 0.9999)
                val mercatorY = 0.5 - 0.25 * Math.log((1.0 + sinY) / (1.0 - sinY)) / Math.PI
                val worldPxX = mercatorX * 256.0 * Math.pow(2.0, map.cameraPosition.zoom.toDouble())
                val worldPxY = mercatorY * 256.0 * Math.pow(2.0, map.cameraPosition.zoom.toDouble())

                val aspectRatio = width.toFloat() / height.toFloat()
                val mapScaleX = 3.5f * zoomFactor
                val mapScaleY = (3.5f / aspectRatio) * zoomFactor

                val noiseOffsetX = (worldPxX * mapScaleX / width).toFloat()
                val noiseOffsetY = (worldPxY * mapScaleY / height).toFloat()

                val timeWindX = (time % 10000000L).toFloat() / 80000f
                val timeWindY = (time % 10000000L).toFloat() / 120000f

                runtimeShader?.setFloatUniform("uMapScale", mapScaleX, mapScaleY)
                runtimeShader?.setFloatUniform(
                        "uWindOffset",
                        noiseOffsetX + timeWindX,
                        noiseOffsetY + timeWindY
                )
                runtimeShader?.setFloatUniform("uFogDensity", 0.95f)

                runtimeShader?.setFloatUniform("uFogColorBright", 0.89f, 0.91f, 0.94f)
                runtimeShader?.setFloatUniform("uFogColorMid", 0.68f, 0.73f, 0.80f)
                runtimeShader?.setFloatUniform("uFogColorDark", 0.15f, 0.18f, 0.25f)
                runtimeShader?.setFloatUniform("uLightDir", -0.5f, -0.6f, 0.8f)

                // 3. Draw onto Screen
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), agslPaint)
            } else {
                // ---- FALLBACK BITMAP MASK PATH (Older Devices) ----
                val zoom = map.cameraPosition.zoom
                val zoomFactor = Math.pow(2.0, (17.0 - zoom).toDouble()).toFloat()
                val target = map.cameraPosition.target
                val mercatorX = target.longitude / 360.0 + 0.5
                val sinY = Math.sin(target.latitude * Math.PI / 180.0).coerceIn(-0.9999, 0.9999)
                val mercatorY = 0.5 - 0.25 * Math.log((1.0 + sinY) / (1.0 - sinY)) / Math.PI
                val worldPxX = mercatorX * 256.0 * Math.pow(2.0, zoom.toDouble())
                val worldPxY = mercatorY * 256.0 * Math.pow(2.0, zoom.toDouble())

                // 矩阵偏移计算，使云层移动和缩放
                val currentScale1 = 2.5f / zoomFactor
                val texWorldW1 = 512f * currentScale1
                val shiftX1 = (worldPxX.toFloat() % texWorldW1)
                val shiftY1 = (worldPxY.toFloat() % texWorldW1)

                val offset1X = (time % 120000L) / 120000f * 512f
                val offset1Y = (time % 150000L) / 150000f * 512f
                cloudMatrix1.reset()
                cloudMatrix1.setScale(currentScale1, currentScale1)
                cloudMatrix1.postTranslate(
                        width / 2f - shiftX1 - offset1X,
                        height / 2f - shiftY1 - offset1Y
                )
                cloudPaint1.shader.setLocalMatrix(cloudMatrix1)

                val currentScale2 = 3.5f / zoomFactor
                val texWorldW2 = 512f * currentScale2
                val shiftX2 = (worldPxX.toFloat() % texWorldW2)
                val shiftY2 = (worldPxY.toFloat() % texWorldW2)

                val offset2X = (time % 180000L) / 180000f * 512f
                val offset2Y = (time % 100000L) / 100000f * 512f
                cloudMatrix2.reset()
                cloudMatrix2.setScale(currentScale2, currentScale2) // 第二层云不同比例，交叉混合产生涌动(Churning)
                cloudMatrix2.postTranslate(
                        width / 2f - shiftX2 + offset2X,
                        height / 2f - shiftY2 - offset2Y
                )
                cloudPaint2.shader.setLocalMatrix(cloudMatrix2)

                // 使用离屏缓冲实现各种叠加及挖洞
                val sc = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

                // 1. 绘制底层基础迷雾环境
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), baseFogPaint)

                // 2. 叠加体积云纹理层 (利用透明度混合出真正的体积云质感)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), cloudPaint1)
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), cloudPaint2)

                // 构建高精度仿射变换矩阵，1次计算全局映射
                val center = map.cameraPosition.target
                val offsetScale = Math.pow(2.0, (17.0 - zoom).toDouble()).toFloat()
                val offDeg = 0.001f * offsetScale
                val p0 = projection.toScreenLocation(center)
                val p1 = projection.toScreenLocation(LatLng(center.latitude + offDeg, center.longitude))
                val p2 = projection.toScreenLocation(LatLng(center.latitude, center.longitude + offDeg))
                
                val src = floatArrayOf(
                    latLngToMercatorX(center.longitude), latLngToMercatorY(center.latitude),
                    latLngToMercatorX(center.longitude), latLngToMercatorY(center.latitude + offDeg),
                    latLngToMercatorX(center.longitude + offDeg), latLngToMercatorY(center.latitude)
                )
                val dst = floatArrayOf(
                    p0.x.toFloat(), p0.y.toFloat(),
                    p1.x.toFloat(), p1.y.toFloat(),
                    p2.x.toFloat(), p2.y.toFloat()
                )
                mercatorToScreenMatrix.setPolyToPoly(src, 0, dst, 0, 3)

                val baseRadius = (map.cameraPosition.zoom * 3.5).coerceIn(50.0, 250.0).toFloat()
                val requiredBlur = baseRadius * 1.8f
                if (Math.abs(lastBlurRadius - requiredBlur) > 2f) {
                    cachedBlurFilter = BlurMaskFilter(requiredBlur, BlurMaskFilter.Blur.NORMAL)
                    lastBlurRadius = requiredBlur
                }

                // 3. 实时轨迹的高斯边缘消除 (物理侵蚀感)
                if (!mercatorLivePath.isEmpty) {
                    pathEraserPaint.maskFilter = cachedBlurFilter
                    pathEraserPaint.strokeWidth = baseRadius * 1.8f
                    scratchingScreenPath.reset()
                    scratchingScreenPath.addPath(mercatorLivePath)
                    scratchingScreenPath.transform(mercatorToScreenMatrix)
                    canvas.drawPath(scratchingScreenPath, pathEraserPaint)
                }

                // 4. 计算探索掩码的动态驱散 (Dynamic Erosion)
                if (historyMercatorCoords.isNotEmpty()) {
                    if (screenHistoryScratch.size < historyMercatorCoords.size) {
                        screenHistoryScratch = FloatArray(historyMercatorCoords.size)
                    }
                    mercatorToScreenMatrix.mapPoints(screenHistoryScratch, historyMercatorCoords)
                    val count = historyPoints.size * 2
                    var i = 0
                    val baseCheckR = baseRadius * 4.5f
                    while (i < count) {
                        val sx = screenHistoryScratch[i]
                        val sy = screenHistoryScratch[i + 1]
                        
                        if (sx >= -baseCheckR && sx <= width + baseCheckR && sy >= -baseCheckR && sy <= height + baseCheckR) {
                            // 由于使用了连续数组映射，我们无法直接获取经纬度来计算呼吸波浪。
                            // 但我们可以通过简单的像素坐标计算出几乎相同的相位。
                            val phase = (historyMercatorCoords[i] + historyMercatorCoords[i+1]) * 10000.0
                            val breatheErosion = Math.sin(time / 1500.0 + phase).toFloat() * 0.15f
                            val dynamicRadius = baseRadius * 4.0f * (1.0f + breatheErosion)

                            fallbackGradient?.let { grad ->
                                fallbackGradientMatrix.setScale(dynamicRadius, dynamicRadius)
                                fallbackGradientMatrix.postTranslate(sx, sy)
                                grad.setLocalMatrix(fallbackGradientMatrix)
                                spotEraserPaint.shader = grad
                                canvas.drawCircle(sx, sy, dynamicRadius, spotEraserPaint)
                            }
                        }
                        i += 2
                    }
                }

                canvas.restoreToCount(sc)
            }

            if (isAnimating && visibility == VISIBLE) {
                postInvalidateOnAnimation() // 以显示器刷新率循环重绘实现60fps动画
            }
        }
    }
}
