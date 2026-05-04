package com.footprint 

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.SystemClock
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
    private var eternalBondPolyline: Polyline? = null
    private var rawCapsules: List<Map<*, *>> = emptyList()
    private var rawEntries: List<Map<*, *>> = emptyList()
    private var currentMode: String = "STANDARD"
    private var lastLivePolylineUpdateMs: Long = 0L
    private var lastRenderedLivePointCount: Int = 0

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
                fogOverlay.requestRender()
            }
        }

        // 2. 监听地图相机变化
        aMap?.setOnCameraChangeListener(
                object : AMap.OnCameraChangeListener {
                    override fun onCameraChange(pos: com.amap.api.maps.model.CameraPosition?) {
                        fogOverlay.requestRender()
                    }
                    override fun onCameraChangeFinish(
                            pos: com.amap.api.maps.model.CameraPosition?
                    ) {
                        fogOverlay.requestRender()
                    }
                }
        )
    }

    private fun updateLivePolyline(force: Boolean = false) {
        val map = aMap ?: return
        val pointCount = currentPathPoints.size
        if (pointCount == 0) {
            livePolyline?.remove()
            livePolyline = null
            lastRenderedLivePointCount = 0
            lastLivePolylineUpdateMs = 0L
            return
        }

        val now = SystemClock.elapsedRealtime()
        val pointDelta = pointCount - lastRenderedLivePointCount
        if (!force && pointDelta in 0..2 && now - lastLivePolylineUpdateMs < 120L) {
            return
        }

        livePolyline?.remove()
        livePolyline = null
        val smoothedPoints = com.footprint.utils.PathInterpolator.interpolate(currentPathPoints, 8)
        val gradientColors = getGradientColorsForSmoothedPoints(smoothedPoints)
        livePolyline =
                map.addPolyline(
                        PolylineOptions()
                                .addAll(smoothedPoints)
                                .width(if (currentMode == "CAPSULE") 10f else 12f)
                                .useGradient(true)
                                .colorValues(gradientColors)
                                .lineCapType(PolylineOptions.LineCapType.LineCapRound)
                                .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound)
                                .zIndex(100f)
                )
        lastLivePolylineUpdateMs = now
        lastRenderedLivePointCount = pointCount
    }

    private fun getGradientColorsForSmoothedPoints(points: List<LatLng>): List<Int> {
        if (points.isEmpty()) return emptyList()
        val colors = mutableListOf<Int>()
        for (i in 0 until points.size) {
            val current = points[i]
            val prev = if (i > 0) points[i - 1] else current
            val next = if (i < points.size - 1) points[i + 1] else current
            
            val results = FloatArray(1)
            android.location.Location.distanceBetween(prev.latitude, prev.longitude, next.latitude, next.longitude, results)
            
            val dist = results[0]
            val approxSpeedMs = dist / 1.25f
            val speedKmh = approxSpeedMs * 3.6f
            
            val color = getSmoothColorForSpeed(speedKmh)
            colors.add(color)
        }
        return colors
    }

    private fun getSmoothColorForSpeed(speedKmh: Float): Int {
        val speedPoints = floatArrayOf(0f, 4f, 10f, 20f, 40f)
        val colors = intArrayOf(
            Color.parseColor("#2196F3"), // Blue (Walking slow/Standing)
            Color.parseColor("#4CAF50"), // Green (Walking/Hiking)
            Color.parseColor("#FFEB3B"), // Yellow (Cycling)
            Color.parseColor("#FF9800"), // Orange (City Driving)
            Color.parseColor("#F44336")  // Red (Highway)
        )

        if (speedKmh <= speedPoints[0]) return colors[0]
        if (speedKmh >= speedPoints[speedPoints.size - 1]) return colors[colors.size - 1]

        for (i in 0 until speedPoints.size - 1) {
            if (speedKmh < speedPoints[i + 1]) {
                val ratio = (speedKmh - speedPoints[i]) / (speedPoints[i + 1] - speedPoints[i])
                return interpolateColor(colors[i], colors[i + 1], ratio)
            }
        }
        return colors[colors.size - 1]
    }

    private fun interpolateColor(color1: Int, color2: Int, ratio: Float): Int {
        val a = (Color.alpha(color1) * (1 - ratio) + Color.alpha(color2) * ratio).toInt()
        val r = (Color.red(color1) * (1 - ratio) + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * (1 - ratio) + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * (1 - ratio) + Color.blue(color2) * ratio).toInt()
        return Color.argb(a, r, g, b)
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
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isTiltGesturesEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true
        map.uiSettings.isCompassEnabled = false
        map.uiSettings.isScaleControlsEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false
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
                LatLng(27.00541, 103.614373),
                LatLng(27.112186, 103.624516),
                LatLng(27.287536, 103.84551),
                LatLng(27.425395, 103.956484),
                LatLng(27.322335, 104.134216),
                LatLng(27.336394, 104.247877),
                LatLng(27.457929, 104.410176),
                LatLng(27.331708, 104.570221),
                LatLng(27.345369, 104.754369),
                LatLng(27.306049, 104.904097),
                LatLng(27.404997, 105.150927),
                LatLng(27.490532, 105.235891),
                LatLng(27.654989, 105.309845),
                LatLng(27.817142, 105.274385),
                LatLng(27.98316, 105.26875),
                LatLng(28.07195, 105.166359),
                LatLng(27.994845, 104.946926),
                LatLng(27.900538, 104.79529),
                LatLng(27.867978, 104.558344),
                LatLng(27.993739, 104.378792),
                LatLng(28.091514, 104.40064),
                LatLng(28.24435, 104.460808),
                LatLng(28.306635, 104.313941),
                LatLng(28.482585, 104.261142),
                LatLng(28.555726, 104.35677),
                LatLng(28.615631, 104.314895),
                LatLng(28.637292, 104.125199),
                LatLng(28.626462, 103.874641),
                LatLng(28.495394, 103.810397),
                LatLng(28.284985, 103.828951),
                LatLng(28.25703, 103.649485),
                LatLng(28.087728, 103.453634),
                LatLng(27.966419, 103.541719),
                LatLng(27.777595, 103.459789),
                LatLng(27.601201, 103.298531),
                LatLng(27.421903, 103.142907),
                LatLng(27.410474, 102.944975),
                LatLng(27.076287, 102.898938),
                LatLng(26.840548, 102.962748),
                LatLng(26.594324, 103.019362),
                LatLng(26.371925, 102.998208),
                LatLng(26.277681, 102.764816),
                LatLng(26.282331, 102.611273),
                LatLng(26.244726, 102.349011),
                LatLng(26.108317, 102.137033),
                LatLng(26.107995, 101.922888),
                LatLng(26.156319, 101.80758),
                LatLng(26.252264, 101.615716),
                LatLng(26.346849, 101.660626),
                LatLng(26.499703, 101.50691),
                LatLng(26.60096, 101.452117),
                LatLng(26.74645, 101.492171),
                LatLng(26.740542, 101.436077),
                LatLng(26.865357, 101.374695),
                LatLng(26.958958, 101.227048),
                LatLng(27.19413, 101.074892),
                LatLng(27.355058, 100.99669),
                LatLng(27.521699, 100.912073),
                LatLng(27.681675, 100.836472),
                LatLng(27.867029, 100.70209),
                LatLng(27.808601, 100.546379),
                LatLng(27.816351, 100.41191),
                LatLng(27.748638, 100.307092),
                LatLng(27.90765, 100.169935),
                LatLng(28.147424, 100.021161),
                LatLng(28.233479, 100.16352),
                LatLng(28.405854, 100.060956),
                LatLng(28.561537, 99.961599),
                LatLng(28.7195, 99.733149),
                LatLng(28.771552, 99.605789),
                LatLng(28.616259, 99.496723),
                LatLng(28.493115, 99.395633),
                LatLng(28.35779, 99.40699),
                LatLng(28.150735, 99.402395),
                LatLng(28.298054, 99.280584),
                LatLng(28.472761, 99.183395),
                LatLng(28.641058, 99.147329),
                LatLng(28.890141, 99.123573),
                LatLng(29.154559, 99.115077),
                LatLng(29.20452, 98.976446),
                LatLng(28.987168, 98.952691),
                LatLng(28.806814, 98.922173),
                LatLng(28.929589, 98.810939),
                LatLng(28.972854, 98.63078),
                LatLng(28.771631, 98.678984),
                LatLng(28.611785, 98.613701),
                LatLng(28.39327, 98.705167),
                LatLng(28.200232, 98.650201),
                LatLng(28.104528, 98.428773),
                LatLng(28.296007, 98.338867),
                LatLng(28.347247, 98.229974),
                LatLng(28.219693, 98.20119),
                LatLng(28.04323, 98.139374),
                LatLng(27.872562, 98.181596),
                LatLng(27.725055, 98.223905),
                LatLng(27.553887, 98.306442),
                LatLng(27.653721, 98.429987),
                LatLng(27.584956, 98.586737),
                LatLng(27.46372, 98.704127),
                LatLng(27.257496, 98.715484),
                LatLng(27.050968, 98.765596),
                LatLng(26.798657, 98.762128),
                LatLng(26.570417, 98.768197),
                LatLng(26.32321, 98.707595),
                LatLng(26.110404, 98.701439),
                LatLng(26.057323, 98.601216),
                LatLng(25.833571, 98.680805),
                LatLng(25.759752, 98.481139),
                LatLng(25.595366, 98.382303),
                LatLng(25.568914, 98.189572),
                LatLng(25.35418, 98.099666),
                LatLng(25.219818, 97.948464),
                LatLng(25.080044, 97.74533),
                LatLng(24.877018, 97.784865),
                LatLng(24.76714, 97.57124),
                LatLng(24.431468, 97.531879),
                LatLng(24.355494, 97.715506),
                LatLng(24.227058, 97.729898),
                LatLng(24.047878, 97.635744),
                LatLng(23.880712, 97.633229),
                LatLng(23.907199, 97.763884),
                LatLng(24.014234, 97.902168),
                LatLng(24.113427, 98.220177),
                LatLng(24.108694, 98.555439),
                LatLng(24.133501, 98.818482),
                LatLng(24.022237, 98.773919),
                LatLng(23.850623, 98.690862),
                LatLng(23.727492, 98.824117),
                LatLng(23.521223, 98.803657),
                LatLng(23.353463, 98.89469),
                LatLng(23.230934, 98.910816),
                LatLng(23.086579, 99.106494),
                LatLng(23.111006, 99.32532),
                LatLng(23.046024, 99.529408),
                LatLng(22.934742, 99.446785),
                LatLng(22.751569, 99.326794),
                LatLng(22.520751, 99.35974),
                LatLng(22.36233, 99.276683),
                LatLng(22.165645, 99.158426),
                LatLng(22.095659, 99.354711),
                LatLng(22.094499, 99.575618),
                LatLng(22.060611, 99.721445),
                LatLng(22.014199, 99.965761),
                LatLng(21.821344, 99.944173),
                LatLng(21.703512, 100.118957),
                LatLng(21.567125, 100.122771),
                LatLng(21.482619, 100.280909),
                LatLng(21.459238, 100.48283),
                LatLng(21.555317, 100.753588),
                LatLng(21.697115, 100.940943),
                LatLng(21.692546, 101.117201),
                LatLng(21.557313, 101.208928),
                LatLng(21.296882, 101.249676),
                LatLng(21.24164, 101.476479),
                LatLng(21.168786, 101.696606),
                LatLng(21.26464, 101.803765),
                LatLng(21.513233, 101.772467),
                LatLng(21.644186, 101.807666),
                LatLng(21.845413, 101.740215),
                LatLng(21.967855, 101.60644),
                LatLng(22.269607, 101.564304),
                LatLng(22.432762, 101.667128),
                LatLng(22.389201, 101.86272),
                LatLng(22.430861, 102.131398),
                LatLng(22.554032, 102.322568),
                LatLng(22.682043, 102.407792),
                LatLng(22.730707, 102.607372),
                LatLng(22.623128, 102.823424),
                LatLng(22.460941, 102.993353),
                LatLng(22.537847, 103.139439),
                LatLng(22.678496, 103.282751),
                LatLng(22.752971, 103.441063),
                LatLng(22.641944, 103.573364),
                LatLng(22.75025, 103.685639),
                LatLng(22.566583, 103.889726),
                LatLng(22.684765, 104.031478),
                LatLng(22.841992, 104.261489),
                LatLng(22.690127, 104.376017),
                LatLng(22.846359, 104.579498),
                LatLng(22.862508, 104.760958),
                LatLng(23.080657, 104.807861),
                LatLng(23.188613, 104.958803),
                LatLng(23.247694, 105.122143),
                LatLng(23.368812, 105.349379),
                LatLng(23.220745, 105.4972),
                LatLng(23.312578, 105.593261),
                LatLng(23.408613, 105.671897),
                LatLng(23.499332, 105.912918),
                LatLng(23.495642, 106.072183),
                LatLng(23.665432, 106.149518),
                LatLng(23.878914, 106.192434),
                LatLng(24.084208, 106.052502),
                LatLng(24.069514, 105.90789),
                LatLng(24.073759, 105.765358),
                LatLng(24.137826, 105.595256),
                LatLng(24.038978, 105.413016),
                LatLng(24.074984, 105.234937),
                LatLng(24.248174, 105.201298),
                LatLng(24.38229, 105.102896),
                LatLng(24.443841, 104.784973),
                LatLng(24.330648, 104.683709),
                LatLng(24.518216, 104.549934),
                LatLng(24.727189, 104.520283),
                LatLng(24.974966, 104.687004),
                LatLng(25.121823, 104.695674),
                LatLng(25.171275, 104.823034),
                LatLng(25.275942, 104.754195),
                LatLng(25.364603, 104.615998),
                LatLng(25.495011, 104.449711),
                LatLng(25.58335, 104.368995),
                LatLng(25.760477, 104.328853),
                LatLng(25.892786, 104.418413),
                LatLng(26.072101, 104.456733),
                LatLng(26.253146, 104.542044),
                LatLng(26.378733, 104.680935),
                LatLng(26.524586, 104.570655),
                LatLng(26.644922, 104.467831),
                LatLng(26.619506, 104.348794),
                LatLng(26.635252, 104.174357),
                LatLng(26.514345, 104.042575),
                LatLng(26.530266, 103.819327),
                LatLng(26.73607, 103.767395),
                LatLng(26.916871, 103.765228),
                LatLng(27.00541, 103.614373)
            
            
            
            
            
            
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
                .fillColor(Color.parseColor("#FF000000")) // Solid paper color completely obscuring outside
                .strokeWidth(0f)
                .zIndex(10f)
        )
        // 美化云南省边缘的描边
        val strokeColor = Color.parseColor("#B0C4DE")
        // --- 孤岛沙盘特效 (Isolated Sandbox) ---
        // 增加虚空背景的多层光影效果，产生悬浮厚度感
        val layers = 5
        for (i in 1..layers) {
            val offsetColor = Color.argb(100 - i * 15, 176, 196, 222) // 淡蓝光影逐渐淡出
            aMap?.addPolyline(PolylineOptions()
                .addAll(yunnanBoundary)
                .width(12f + i * 4f)
                .color(offsetColor)
                .zIndex(9f - i) // 放在遮罩下方实现发光层
            )
        }

        // 核心发光边缘 (琥珀边缘)
        aMap?.addPolyline(PolylineOptions()
            .addAll(yunnanBoundary)
            .width(10f)
            .color(Color.parseColor("#80FFFFFF")) // 极细高光
            .zIndex(11.1f)
            .lineCapType(PolylineOptions.LineCapType.LineCapRound)
        )

        map.addPolyline(PolylineOptions()
            .addAll(yunnanBoundary)
            .width(8f)
            .color(strokeColor)
            .zIndex(11f)
            .lineCapType(PolylineOptions.LineCapType.LineCapRound)
        )


        // 限制地图拖动及缩放范围，将视野绝对锁定在云南省 (Isolated Bounds)
        val s = LatLng(21.14, 97.40)
        val n = LatLng(29.23, 106.20)
        map.setMapStatusLimits(LatLngBounds(s, n))
        map.minZoomLevel = 5.5f
        map.maxZoomLevel = 11.0f

        // 缩放至云南全境，呈现出一种“俯瞰琥珀”的上帝视角
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(25.04, 101.8), 6.5f))
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
            val adcode = entry["adcode"] as? String

            // 数据隔离：如果处于永恒之境模式，只显示云南省(53开头的adcode)的数据
            if (currentMode == "ETERNAL_REALM") {
                val isYunnan = adcode?.startsWith("53") ?: (lat != null && lng != null && lat in 21.0..29.5 && lng in 97.0..106.5)
                if (!isYunnan) return@forEach
            }
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
            val adcode = data["adcode"] as? String

            // 同样对时光胶囊进行地域隔离
            if (currentMode == "ETERNAL_REALM") {
                val isYunnan = adcode?.startsWith("53") ?: (lat in 21.0..29.5 && lng in 97.0..106.5)
                if (!isYunnan) return@forEach
            }
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
                fogOverlay.requestRender()
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
                fogOverlay.visibility =
                    if (mode == "FOG" || mode == "ETERNAL_REALM") View.VISIBLE else View.GONE
                
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
                fogOverlay.requestRender()
                result.success(true)
            }
            "setEntries" -> {
                @Suppress("UNCHECKED_CAST")
                rawEntries = call.arguments as? List<Map<*, *>> ?: emptyList()
                updateEntryMarkers()
                result.success(true)
            }
            "setEternalBondPath" -> {
                val rawPoints: List<*> = call.arguments as? List<*> ?: emptyList<Any>()
                updateEternalBondPath(rawPoints)
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
                
                // 彻底清空逻辑：无论什么模式，先移除所有 Polyline 和迷雾缓存
                historyPolylines.forEach { it.remove() }
                historyPolylines.clear()
                historyPoints = emptyList()
                fogOverlay.updateHistoryMercatorCache()
                fogOverlay.requestRender()

                if (points.isEmpty()) {
                    if (heatmapOverlay != null) updateHeatmap()
                    result.success(true)
                    return
                }

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
                fogOverlay.requestRender()

                if (heatmapOverlay != null) updateHeatmap()
                
                if (currentMode != "HEATMAP") {
                    for (segment in sessionSegments) {
                        if (segment.size >= 2) {
                            val smoothedSegment = com.footprint.utils.PathInterpolator.interpolate(segment, 8)
                            val gradientColors = getGradientColorsForSmoothedPoints(smoothedSegment)
                            val polyline = aMap?.addPolyline(
                                PolylineOptions()
                                    .addAll(smoothedSegment)
                                    .width(if (currentMode == "CAPSULE") 10f else 12f)
                                    .useGradient(true)
                                    .colorValues(gradientColors)
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
                    val smoothedPoints = com.footprint.utils.PathInterpolator.interpolate(latLngs, 8)
                    val gradientColors = getGradientColorsForSmoothedPoints(smoothedPoints)
                    val polyline = aMap?.addPolyline(
                        PolylineOptions()
                                .addAll(smoothedPoints)
                                .width(if (currentMode == "CAPSULE") 10f else 12f)
                                .useGradient(true)
                                .colorValues(gradientColors)
                                .lineCapType(PolylineOptions.LineCapType.LineCapRound)
                                .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound)
                                .zIndex(95f)
                    )
                    polyline?.let { historyPolylines.add(it) }
                }
                result.success(true)
            }
            "centerLocation" -> {
                val args = call.arguments
                var latArg: Double? = null
                var lngArg: Double? = null
                var zoomLevel = 17f
                if (args is Map<*, *>) {
                    latArg = (args["latitude"] as? Number)?.toDouble()
                    lngArg = (args["longitude"] as? Number)?.toDouble()
                    zoomLevel = (args["zoom"] as? Number)?.toFloat() ?: 17f
                }

                // 1. 如果 Flutter 已经明确传入了坐标，直接使用
                if (latArg != null && lngArg != null && latArg > 1.0 && lngArg > 1.0) {
                    aMap?.animateCamera(
                            com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                    LatLng(latArg, lngArg),
                                    zoomLevel
                            )
                    )
                    result.success(true)
                    return
                }

                // 2. 否则，异步等待原生定位 Fix (最多等待 3s)
                scope.launch {
                    var finalLat = 0.0
                    var finalLng = 0.0

                    // 确保开启定位图层
                    aMap?.isMyLocationEnabled = true

                    // 轮询检查是否有可用坐标
                    for (i in 0 until 15) { // 15 * 200ms = 3s
                        if (cachedLat > 1.0 && cachedLng > 1.0) {
                            finalLat = cachedLat
                            finalLng = cachedLng
                        }
                        if (finalLat < 1.0) {
                            aMap?.myLocation?.let {
                                if (it.latitude > 1.0) {
                                    finalLat = it.latitude
                                    finalLng = it.longitude
                                }
                            }
                        }
                        if (finalLat < 1.0) {
                            LocationTrackingService.currentLocation.value?.let {
                                if (it.latitude > 1.0) {
                                    finalLat = it.latitude
                                    finalLng = it.longitude
                                }
                            }
                        }

                        if (finalLat > 1.0) break
                        delay(200)
                    }

                    if (finalLat > 1.0) {
                        aMap?.animateCamera(
                                com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                        LatLng(finalLat, finalLng),
                                        zoomLevel
                                )
                        )
                        result.success(true)
                    } else {
                        // 策略 3: 使用一次性的强力定位 Client
                        try {
                            val singleClient = com.amap.api.location.AMapLocationClient(context)
                            val option = com.amap.api.location.AMapLocationClientOption()
                            option.isOnceLocation = true
                            option.locationMode = com.amap.api.location.AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                            singleClient.setLocationOption(option)
                            
                            var foundByClient = false
                            singleClient.setLocationListener { loc ->
                                if (loc != null && loc.errorCode == 0 && loc.latitude > 1.0) {
                                    aMap?.animateCamera(
                                            com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(loc.latitude, loc.longitude),
                                                    zoomLevel
                                            )
                                    )
                                    foundByClient = true
                                }
                            }
                            singleClient.startLocation()
                            
                            // 等待强力定位结果 (3秒)
                            for(j in 0 until 10) {
                                if (foundByClient) break
                                delay(300)
                            }
                            
                            if (foundByClient) {
                                result.success(true)
                            } else {
                                result.error("LOCATION_UNAVAILABLE", "获取位置失败", "无法获取定位，请确保 GPS 已开启")
                            }
                            singleClient.onDestroy()
                        } catch (e: Exception) {
                            result.error("LOCATION_ERROR", e.message, null)
                        }
                    }
                }
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
            updateAnimationState()
            requestRender()
        }

        fun requestRender() {
            if (shouldAnimate()) {
                postInvalidateOnAnimation()
            } else {
                invalidate()
            }
        }

        private fun shouldAnimate(): Boolean = visibility == VISIBLE || isEternalMode

        private fun updateAnimationState() {
            val shouldAnimate = shouldAnimate()
            if (shouldAnimate == isAnimating) return
            isAnimating = shouldAnimate
            if (isAnimating && isAttachedToWindow) {
                postInvalidateOnAnimation()
            }
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
            updateAnimationState()
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

        override fun onVisibilityChanged(changedView: View, visibility: Int) {
            super.onVisibilityChanged(changedView, visibility)
            updateAnimationState()
            if (visibility == VISIBLE) {
                postInvalidateOnAnimation()
            }
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

            if (isAnimating) {
                postInvalidateOnAnimation()
            }
        }

        private fun drawEternalClouds(canvas: Canvas) {
            if (android.os.Build.VERSION.SDK_INT >= 33 && eternalShader != null) {
                try {
                    val shader = eternalShader!!
                    shader.setFloatUniform("uResolution", width.toFloat(), height.toFloat())
                    shader.setFloatUniform(
                            "uTime",
                            (System.currentTimeMillis() % 1000000L).toFloat() / 1000f
                    )
                    shader.setFloatUniform(
                            "uTimeOfDay",
                            Calendar.getInstance().get(Calendar.HOUR_OF_DAY) / 24f
                    )

                    val center = aMap?.cameraPosition?.target ?: LatLng(25.0, 102.0)
                    shader.setFloatUniform(
                            "uWindOffset",
                            center.longitude.toFloat(),
                            center.latitude.toFloat()
                    )

                    val zoom = (aMap?.cameraPosition?.zoom ?: 10f)
                    val baseScale = (20f - zoom).coerceAtLeast(1f) * 2f
                    val aspect = if (height > 0) width.toFloat() / height.toFloat() else 1f
                    shader.setFloatUniform(
                            "uMapScale",
                            baseScale,
                            (baseScale / aspect).coerceAtLeast(0.5f)
                    )

                    canvas.drawRect(
                            0f,
                            0f,
                            width.toFloat(),
                            height.toFloat(),
                            Paint().apply { this.shader = shader }
                    )
                } catch (e: IllegalArgumentException) {
                    android.util.Log.e(
                            "FlutterMapView",
                            "Eternal shader uniform mismatch: ${e.message}"
                    )
                    canvas.drawColor(Color.parseColor("#1AFFFFFF"))
                }
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
            Triple(LatLng(25.0517, 102.7074), "KUNMING", "昆明"),
            Triple(LatLng(24.9587, 102.6506), "DIANCHI", "滇池"),
            Triple(LatLng(25.6980, 100.1636), "DALI", "大理"),
            Triple(LatLng(25.7956, 100.1777), "ERHAI", "洱海"),
            Triple(LatLng(26.8721, 100.2380), "LIJIANG", "丽江"),
            Triple(LatLng(27.1096, 100.2982), "YULONG", "玉龙雪山"),
            Triple(LatLng(27.8277, 99.7073), "SHANGRILA", "香格里拉"),
            Triple(LatLng(22.0075, 100.7974), "XISHUANGBANNA", "西双版纳"),
            Triple(LatLng(25.0171, 98.4966), "TENGCHONG", "腾冲")
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

    private fun updateEternalBondPath(rawPoints: List<*>) {
        eternalBondPolyline?.remove()
        eternalBondPolyline = null
        if (rawPoints.size < 2) return

        val latLngs =
                rawPoints.mapNotNull { point ->
                    val map = point as? Map<*, *> ?: return@mapNotNull null
                    val lat = (map["lat"] as? Number)?.toDouble()
                    val lng = (map["lng"] as? Number)?.toDouble()
                    if (lat == null || lng == null) null else LatLng(lat, lng)
                }

        if (latLngs.size < 2) return

        eternalBondPolyline =
                aMap?.addPolyline(
                        PolylineOptions()
                                .addAll(latLngs)
                                .width(10f)
                                .colorValues(
                                        listOf(
                                                Color.parseColor("#88F6C5"),
                                                Color.parseColor("#F5D48A")
                                        )
                                )
                                .useGradient(true)
                                .setDottedLine(true)
                                .zIndex(120f)
                )
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
            "KUNMING" -> {
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
            "DIANCHI" -> {
                c.drawArc(RectF(18f, 28f, 54f, 48f), 180f, 180f, false, p)
                c.drawArc(RectF(24f, 22f, 48f, 40f), 180f, 180f, false, p)
            }
            "DALI" -> {
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
            "ERHAI" -> {
                c.drawArc(RectF(18f, 28f, 54f, 48f), 180f, 180f, false, p)
                c.drawLine(20f, 40f, 52f, 40f, p)
                c.drawCircle(32f, 30f, 2f, p)
            }
            "LIJIANG" -> {
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
            "YULONG" -> {
                val path = Path()
                path.moveTo(18f, 46f)
                path.lineTo(30f, 24f)
                path.lineTo(38f, 36f)
                path.lineTo(46f, 20f)
                path.lineTo(54f, 46f)
                c.drawPath(path, p)
            }
            "SHANGRILA" -> {
                c.drawCircle(36f, 34f, 10f, p)
                c.drawLine(36f, 18f, 36f, 52f, p)
                c.drawLine(24f, 26f, 48f, 42f, p)
            }
            "XISHUANGBANNA" -> {
                val leaf = Path()
                leaf.moveTo(36f, 18f)
                leaf.quadTo(52f, 28f, 36f, 52f)
                leaf.quadTo(20f, 28f, 36f, 18f)
                c.drawPath(leaf, p)
                c.drawLine(36f, 24f, 36f, 46f, p)
            }
            "TENGCHONG" -> {
                c.drawRoundRect(RectF(22f, 24f, 50f, 46f), 6f, 6f, p)
                c.drawLine(28f, 30f, 44f, 30f, p)
                c.drawLine(28f, 36f, 44f, 36f, p)
            }
        }
        return b
    }
}
