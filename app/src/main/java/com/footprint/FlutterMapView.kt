package com.footprint 

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.footprint.ui.effects.ETERNAL_CLOUD_SHADER
import com.footprint.service.LocationTrackingService
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.platform.PlatformView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.Calendar
import java.util.LinkedHashMap

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
    private val historyPolylines = mutableListOf<Polyline>()
    private var heatmapOverlay: TileOverlay? = null
    private val markerList = mutableListOf<Marker>()
    private val capsuleMarkers = mutableListOf<Marker>()
    private val eternalMarkers = mutableListOf<Marker>()
    private var rawCapsules: List<Map<*, *>> = emptyList()
    private var rawEntries: List<Map<*, *>> = emptyList()
    private var currentMode: String = "STANDARD"

    // 专属渲染层
    private var yunnanMask: Polygon? = null

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
                if (marker.title == "ETERNAL_POI") {
                    channel.invokeMethod("onMarkerClick", marker.snippet)
                    return@setOnMarkerClickListener true
                }
                val markerId = marker.snippet?.toLongOrNull()
                if (markerId != null) {
                    if (marker.title == "CAPSULE") {
                        channel.invokeMethod("onCapsuleClick", markerId)
                    } else {
                        channel.invokeMethod("onMarkerClick", markerId)
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
                                    .addAll(com.footprint.utils.PathInterpolator.interpolate(currentPathPoints, 8))
                                    .width(if (currentMode == "CAPSULE") 10f else 12f)
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

        if (modeToUse == "ETERNAL_REALM") {
            applyEternalRealmStyle()
        } else {
            yunnanMask?.remove()
            yunnanMask = null
            aMap?.setMapStatusLimits(null) // 取消边界限制
            fogOverlay.setEternalMode(false)
            aMap?.setCustomMapStyle(CustomMapStyleOptions().apply { isEnable = false }) // 禁用自定义样式
        }
    }

    private fun applyEternalRealmStyle() {
        val map = aMap ?: return
        map.mapType = AMap.MAP_TYPE_NORMAL
        map.showBuildings(false)
        map.isTrafficEnabled = false
        map.showIndoorMap(false)
        fogOverlay.setEternalMode(true)

        // 应用极简水彩风自定义样式
        try {
            val styleJson = context.assets.open("map_style_eternal.json").bufferedReader().readText()
            val styleOptions = CustomMapStyleOptions().apply {
                isEnable = true
                styleData = styleJson.toByteArray()
            }
            map.setCustomMapStyle(styleOptions)
        } catch (e: Exception) {
            android.util.Log.w("FlutterMapView", "Failed to load eternal map style: ${e.message}")
        }

        // 云南省大致边界 (简化版)
        val yunnanBoundary = listOf(
            LatLng(29.23, 98.13), LatLng(28.43, 99.45), LatLng(28.23, 101.45),
            LatLng(28.85, 103.02), LatLng(27.95, 104.32), LatLng(27.42, 105.15),
            LatLng(25.62, 105.15), LatLng(24.95, 106.18), LatLng(22.85, 105.52),
            LatLng(22.51, 104.45), LatLng(21.15, 101.55), LatLng(21.45, 99.98),
            LatLng(23.62, 98.85), LatLng(24.52, 97.52), LatLng(25.95, 98.05),
            LatLng(28.25, 97.95)
        )

        // 外围遮罩 (全世界遮罩，中间留洞给云南)
        val holeOptions = PolygonHoleOptions()
        holeOptions.addAll(yunnanBoundary)
        
        val world = listOf(
            LatLng(85.0, -180.0), LatLng(85.0, 180.0), 
            LatLng(-85.0, 180.0), LatLng(-85.0, -180.0)
        )
        
        yunnanMask?.remove()
        yunnanMask = map.addPolygon(
            PolygonOptions().addAll(world).addHoles(holeOptions)
                .fillColor(Color.parseColor("#F9F1E7")) // Solid paper color completely obscuring outside
                .strokeWidth(0f)
                .zIndex(10f)
        )

        // 限制地图拖动范围仅为云南
        val s = LatLng(21.14, 97.52)
        val n = LatLng(29.23, 106.18)
        map.setMapStatusLimits(LatLngBounds(s, n))

        // 缩放至云南
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(25.04, 101.5), 6.5f))
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
        allPoints.addAll(com.footprint.utils.PathInterpolator.interpolate(currentPathPoints, 8))

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
                
                if (mode == "ETERNAL_REALM") {
                    fogOverlay.setEternalMode(true)
                    updateEternalMarkers()
                } else {
                    fogOverlay.setEternalMode(false)
                    clearEternalMarkers()
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
                val points: List<Any?> = (call.arguments as? List<*>) ?: emptyList()
                
                // 1. 解析点位并按 SessionId 分组
                // 使用 LinkedHashMap 保持 Session 间的顺序，每个 Session 内部是一个或多个分段的列表
                val sessionSegments = mutableListOf<MutableList<LatLng>>()
                val allPointsForFog = mutableListOf<LatLng>()
                
                var currentSid: Long? = null
                var lastSegment: MutableList<LatLng>? = null
                var lastLatLng: LatLng? = null
                var lastTimestamp: Long = 0

                points.forEach {
                    val itMap = it as? Map<*, *> ?: return@forEach
                    val lat = (itMap["lat"] as? Number)?.toDouble() ?: (itMap["latitude"] as? Number)?.toDouble()
                    val lng = (itMap["lng"] as? Number)?.toDouble() ?: (itMap["longitude"] as? Number)?.toDouble()
                    val sid = (itMap["sessionId"] as? Number)?.toLong() ?: 0L
                    val ts = (itMap["timestamp"] as? Number)?.toLong() ?: 0L
                    
                    if (lat != null && lng != null) {
                        val latLng = LatLng(lat, lng)
                        allPointsForFog.add(latLng)

                        // 逻辑：如果 SessionId 变了，或者时间跨度太大（>1小时），或者距离跨度太大（>5公里且非连续点）
                        // 则开启新的线段 (Segment)
                        var needsNewSegment = (sid != currentSid)
                        if (!needsNewSegment && lastLatLng != null) {
                            val timeGap = Math.abs(ts - lastTimestamp)
                            // 距离检查：如果两个坐标点距离超过 5km，认为是不连续的记录（可能是 GPS 跳跃或中间没记上）
                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(lastLatLng!!.latitude, lastLatLng!!.longitude, lat, lng, results)
                            val distGap = results[0]
                            
                            // 只要时间超过1小时或距离超过5km，就断开，避免产生跨越城市的大长线
                            if (timeGap > 3600_000 || distGap > 5000) {
                                needsNewSegment = true
                            }
                        }

                        if (needsNewSegment) {
                            val nextSegment = mutableListOf<LatLng>()
                            nextSegment.add(latLng)
                            sessionSegments.add(nextSegment)
                            lastSegment = nextSegment
                            currentSid = sid
                        } else {
                            lastSegment?.add(latLng)
                        }

                        lastLatLng = latLng
                        lastTimestamp = ts
                    }
                }

                historyPoints = allPointsForFog
                fogOverlay.updateHistoryMercatorCache()
                fogOverlay.invalidate()

                if (heatmapOverlay != null) updateHeatmap()

                historyPolylines.forEach { it.remove() }
                historyPolylines.clear()
                
                if (currentMode != "HEATMAP") {
                    val pathColor = getPathColor(false)
                    for (segment in sessionSegments) {
                        if (segment.size >= 2) {
                            val polyline = aMap?.addPolyline(
                                PolylineOptions()
                                    .addAll(com.footprint.utils.PathInterpolator.interpolate(segment, 8))
                                    .width(if (currentMode == "CAPSULE") 10f else 12f)
                                    .color(pathColor)
                                    .lineCapType(PolylineOptions.LineCapType.LineCapRound)
                                    .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound)
                                    .zIndex(90f)
                            )
                            polyline?.let { historyPolylines.add(it) }
                        }
                    }
                }
                result.success(true)
            }
            "setTrackingPath" -> {
                // 用于显示单个历史足迹轨迹 (Detail Page 使用)
                val points: List<Any?> = (call.arguments as? List<*>) ?: emptyList()
                val latLngs = mutableListOf<LatLng>()
                points.forEach {
                    val itMap = it as? Map<*, *> ?: return@forEach
                    val lat = (itMap["lat"] as? Number)?.toDouble() ?: (itMap["latitude"] as? Number)?.toDouble()
                    val lng = (itMap["lng"] as? Number)?.toDouble() ?: (itMap["longitude"] as? Number)?.toDouble()
                    if (lat != null && lng != null) {
                        latLngs.add(LatLng(lat, lng))
                    }
                }
                
                historyPolylines.forEach { it.remove() }
                historyPolylines.clear()
                
                if (latLngs.size >= 2) {
                    val pathColor = getPathColor(false)
                    val polyline = aMap?.addPolyline(
                        PolylineOptions()
                                .addAll(com.footprint.utils.PathInterpolator.interpolate(latLngs, 8))
                                .width(if (currentMode == "CAPSULE") 10f else 12f)
                                .color(pathColor)
                                .lineCapType(PolylineOptions.LineCapType.LineCapRound)
                                .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound)
                                .zIndex(95f)
                    )
                    polyline?.let { historyPolylines.add(it) }
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

        private var isEternalMode = false
        private var eternalShader: RuntimeShader? = null
        
        fun setEternalMode(enabled: Boolean) {
            isEternalMode = enabled
            if (enabled && eternalShader == null) {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    eternalShader = RuntimeShader(ETERNAL_CLOUD_SHADER)
                }
            }
            invalidate()
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
            if (isEternalMode) {
                drawEternalClouds(canvas)
                return
            }
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
                postInvalidateOnAnimation()
            }
        }

        private fun drawEternalClouds(canvas: Canvas) {
            if (android.os.Build.VERSION.SDK_INT >= 33 && eternalShader != null) {
                val shader = eternalShader!!
                shader.setFloatUniform("uResolution", width.toFloat(), height.toFloat())
                shader.setFloatUniform("uTime", (System.currentTimeMillis() % 1000000L).toFloat() / 1000f)
                shader.setFloatUniform("uTimeOfDay", Calendar.getInstance().get(Calendar.HOUR_OF_DAY) / 24f)
                
                val center = aMap?.cameraPosition?.target ?: LatLng(25.0, 102.0)
                shader.setFloatUniform("uWindOffset", center.longitude.toFloat(), center.latitude.toFloat())
                shader.setFloatUniform("uMapScale", (20f - (aMap?.cameraPosition?.zoom ?: 10f)).coerceAtLeast(1f) * 2f)
                
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), Paint().apply { this.shader = shader })
            } else {
                // Fallback: draw a very light tint
                canvas.drawColor(Color.parseColor("#1AFFFFFF"))
            }

            if (isAnimating) postInvalidateOnAnimation()
        }
    }

    private fun updateEternalMarkers() {
        clearEternalMarkers()
        val pois = listOf(
            Triple(LatLng(25.04, 102.71), "KUNMING", "昆明"),
            Triple(LatLng(25.69, 100.16), "DALI", "大理"),
            Triple(LatLng(26.87, 100.22), "LIJIANG", "丽江")
        )
        pois.forEach { (pos, tag, _) ->
            val opt = MarkerOptions().position(pos).title("ETERNAL_POI").snippet(tag)
            opt.anchor(0.5f, 0.5f)
            opt.icon(BitmapDescriptorFactory.fromBitmap(createEternalMarkerBitmap(tag)))
            val m = aMap?.addMarker(opt)
            m?.let { eternalMarkers.add(it) }
        }
    }

    private fun clearEternalMarkers() {
        eternalMarkers.forEach { it.remove() }
        eternalMarkers.clear()
    }

    private fun createEternalMarkerBitmap(tag: String): Bitmap {
        val size = 72
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A4A4A")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E0D8")
            style = Paint.Style.FILL
            alpha = 180
        }
        // Minimalist wireframe background circle
        c.drawCircle(36f, 36f, 30f, fillP)
        c.drawCircle(36f, 36f, 30f, p)
        
        when (tag) {
            "KUNMING" -> { // Flower 🌺
                c.save()
                c.translate(36f, 36f)
                for (i in 0..4) {
                    val oval = RectF(-6f, -20f, 6f, 0f)
                    c.drawOval(oval, p)
                    c.rotate(72f)
                }
                c.restore()
                c.drawCircle(36f, 36f, 4f, p)
            }
            "DALI" -> { // Book 📖
                val path = Path()
                path.moveTo(22f, 26f)
                path.quadTo(28f, 22f, 36f, 26f)
                path.quadTo(44f, 22f, 50f, 26f)
                path.lineTo(50f, 44f)
                path.quadTo(44f, 40f, 36f, 44f)
                path.quadTo(28f, 40f, 22f, 44f)
                path.close()
                c.drawPath(path, p)
                c.drawLine(36f, 26f, 36f, 44f, p)
            }
            "LIJIANG" -> { // Cat 🐈
                val path = Path()
                path.moveTo(26f, 28f)
                path.lineTo(26f, 18f)
                path.lineTo(32f, 24f)
                path.lineTo(40f, 24f)
                path.lineTo(46f, 18f)
                path.lineTo(46f, 28f)
                path.addArc(RectF(24f, 26f, 48f, 46f), 180f, -180f)
                c.drawPath(path, p)
                c.drawPoint(30f, 34f, p)
                c.drawPoint(42f, 34f, p)
                c.drawLine(34f, 38f, 38f, 38f, p)
            }
        }
        return b
    }
}
