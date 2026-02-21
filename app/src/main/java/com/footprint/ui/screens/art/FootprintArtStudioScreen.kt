package com.footprint.ui.screens.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.footprint.FootprintViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootprintArtStudioScreen(viewModel: FootprintViewModel, onBack: () -> Unit) {
        val context = LocalContext.current
        val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
        val mapView = remember { TextureMapView(context).apply { onCreate(null) } }

        // Map Lifecycle
        DisposableEffect(lifecycle, mapView) {
                val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                                else -> {}
                        }
                }
                lifecycle.addObserver(observer)

                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        mapView.onResume()
                }

                onDispose {
                        lifecycle.removeObserver(observer)
                        mapView.onDestroy()
                }
        }

        // State for controls
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val defaultColor =
                remember(uiState.artColorStyle) {
                        when (uiState.artColorStyle) {
                                "Deep Blue" -> Color(0xFF007AFF)
                                "Cyber Pink" -> Color(0xFFFF2D55)
                                "Neon Green" -> Color(0xFF00FF9F)
                                "Gold" -> Color(0xFFFFCC00)
                                else -> Color(0xFF00FF9F)
                        }
                }

        var selectedColor by remember(defaultColor) { mutableStateOf(defaultColor) }
        var lineWeight by remember { mutableFloatStateOf(5f) }
        var glowRadius by remember { mutableFloatStateOf(15f) }
        var mapStyle by remember { mutableStateOf(ArtMapStyle.DARK) }
        var selectedLayout by remember { mutableStateOf(ArtLayout.FULLCREEN_A24) }
        var showControls by remember { mutableStateOf(true) }

        // Time Scope (Default to this year)
        var startDate by remember { mutableStateOf(LocalDate.now().withDayOfYear(1)) }
        var endDate by remember { mutableStateOf(LocalDate.now()) }
        val startTimestamp =
                remember(startDate) {
                        LocalDateTime.of(startDate, LocalTime.MIN)
                                .toInstant(ZoneOffset.UTC)
                                .toEpochMilli()
                }
        val endTimestamp =
                remember(endDate) {
                        LocalDateTime.of(endDate, LocalTime.MAX)
                                .toInstant(ZoneOffset.UTC)
                                .toEpochMilli()
                }

        val tracePoints by
                viewModel
                        .getTrackPoints(startTimestamp, endTimestamp)
                        .collectAsStateWithLifecycle(initialValue = emptyList())

        // Calc distance
        val totalDistanceKm =
                remember(tracePoints) {
                        if (tracePoints.size < 2) 0.0
                        else {
                                var dist = 0.0
                                for (i in 0 until tracePoints.size - 1) {
                                        val results = FloatArray(1)
                                        android.location.Location.distanceBetween(
                                                tracePoints[i].latitude,
                                                tracePoints[i].longitude,
                                                tracePoints[i + 1].latitude,
                                                tracePoints[i + 1].longitude,
                                                results
                                        )
                                        dist += results[0]
                                }
                                dist / 1000.0
                        }
                }

        // Map Logic: Camera Update & Drawing
        LaunchedEffect(mapView, tracePoints, mapStyle, lineWeight, selectedColor, glowRadius) {
                val map = mapView.map ?: return@LaunchedEffect

                // 1. Update Map Style
                map.mapType =
                        when (mapStyle) {
                                ArtMapStyle.DARK, ArtMapStyle.VOID -> AMap.MAP_TYPE_NIGHT
                                ArtMapStyle.LIGHT -> AMap.MAP_TYPE_NORMAL
                                ArtMapStyle.SATELLITE -> AMap.MAP_TYPE_SATELLITE
                        }

                // Customizing for "Void"
                map.showMapText(mapStyle != ArtMapStyle.VOID)
                map.showBuildings(mapStyle != ArtMapStyle.VOID)

                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isScaleControlsEnabled = false

                // 2. Draw Track
                map.clear()

                if (tracePoints.isNotEmpty()) {
                        val latLngs = tracePoints.map { LatLng(it.latitude, it.longitude) }

                        // Draw "Glow"
                        map.addPolyline(
                                com.amap.api.maps.model.PolylineOptions()
                                        .addAll(latLngs)
                                        .width(lineWeight + glowRadius * 2f)
                                        .color(selectedColor.copy(alpha = 0.4f).toArgb())
                        )

                        // Draw Core Line
                        map.addPolyline(
                                com.amap.api.maps.model.PolylineOptions()
                                        .addAll(latLngs)
                                        .width(lineWeight)
                                        .color(selectedColor.toArgb())
                        )

                        // 3. Move Camera
                        try {
                                val builder = LatLngBounds.builder()
                                latLngs.forEach { builder.include(it) }
                                map.moveCamera(
                                        CameraUpdateFactory.newLatLngBounds(builder.build(), 150)
                                )
                        } catch (e: Exception) {
                                if (latLngs.isNotEmpty()) {
                                        map.moveCamera(
                                                CameraUpdateFactory.newLatLngZoom(latLngs[0], 14f)
                                        )
                                }
                        }
                }
        }

        val scaffoldState =
                androidx.compose.material3.rememberBottomSheetScaffoldState(
                        bottomSheetState =
                                androidx.compose.material3.rememberStandardBottomSheetState(
                                        initialValue =
                                                androidx.compose.material3.SheetValue
                                                        .PartiallyExpanded
                                )
                )

        androidx.compose.material3.BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = 240.dp,
                sheetContainerColor = Color.Black.copy(alpha = 0.6f),
                sheetDragHandle = {
                        androidx.compose.material3.BottomSheetDefaults.DragHandle(
                                color = Color.White
                        )
                },
                sheetContent = {
                        if (showControls) {
                                ArtStyleControls(
                                        artName = uiState.artAuthorName,
                                        onArtNameChange = {
                                                viewModel.updateArtSettings(
                                                        it,
                                                        uiState.artFontName,
                                                        uiState.artColorStyle,
                                                        uiState.artTextColor,
                                                        uiState.artTextItalic,
                                                        uiState.artTextBorder
                                                )
                                        },
                                        fontName = uiState.artFontName,
                                        onFontNameChange = {
                                                viewModel.updateArtSettings(
                                                        uiState.artAuthorName,
                                                        it,
                                                        uiState.artColorStyle,
                                                        uiState.artTextColor,
                                                        uiState.artTextItalic,
                                                        uiState.artTextBorder
                                                )
                                        },
                                        coreColorName = uiState.artColorStyle,
                                        onCoreColorNameChange = {
                                                viewModel.updateArtSettings(
                                                        uiState.artAuthorName,
                                                        uiState.artFontName,
                                                        it,
                                                        uiState.artTextColor,
                                                        uiState.artTextItalic,
                                                        uiState.artTextBorder
                                                )
                                        },
                                        lineWeight = lineWeight,
                                        onLineWeightChange = { lineWeight = it },
                                        glowRadius = glowRadius,
                                        onGlowRadiusChange = { glowRadius = it },
                                        mapStyle = mapStyle,
                                        onMapStyleChange = { mapStyle = it },
                                        startDate = startDate,
                                        endDate = endDate,
                                        onStartDateClick = {
                                                android.app.DatePickerDialog(
                                                                context,
                                                                { _, y, m, d ->
                                                                        startDate =
                                                                                LocalDate.of(
                                                                                        y,
                                                                                        m + 1,
                                                                                        d
                                                                                )
                                                                },
                                                                startDate.year,
                                                                startDate.monthValue - 1,
                                                                startDate.dayOfMonth
                                                        )
                                                        .show()
                                        },
                                        onEndDateClick = {
                                                android.app.DatePickerDialog(
                                                                context,
                                                                { _, y, m, d ->
                                                                        endDate =
                                                                                LocalDate.of(
                                                                                        y,
                                                                                        m + 1,
                                                                                        d
                                                                                )
                                                                },
                                                                endDate.year,
                                                                endDate.monthValue - 1,
                                                                endDate.dayOfMonth
                                                        )
                                                        .show()
                                        },
                                        layout = selectedLayout,
                                        onLayoutChange = { selectedLayout = it },
                                        textColor = uiState.artTextColor,
                                        onTextColorChange = {
                                                viewModel.updateArtSettings(
                                                        uiState.artAuthorName,
                                                        uiState.artFontName,
                                                        uiState.artColorStyle,
                                                        it,
                                                        uiState.artTextItalic,
                                                        uiState.artTextBorder
                                                )
                                        },
                                        isItalic = uiState.artTextItalic,
                                        onItalicChange = {
                                                viewModel.updateArtSettings(
                                                        uiState.artAuthorName,
                                                        uiState.artFontName,
                                                        uiState.artColorStyle,
                                                        uiState.artTextColor,
                                                        it,
                                                        uiState.artTextBorder
                                                )
                                        },
                                        hasBorder = uiState.artTextBorder,
                                        onBorderChange = {
                                                viewModel.updateArtSettings(
                                                        uiState.artAuthorName,
                                                        uiState.artFontName,
                                                        uiState.artColorStyle,
                                                        uiState.artTextColor,
                                                        uiState.artTextItalic,
                                                        it
                                                )
                                        },
                                        accentColor = selectedColor
                                )
                        }
                }
        ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        // 1. Base Map Layer
                        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

                        // 3. Layout Overlay
                        ArtLayoutOverlay(
                                layout = selectedLayout,
                                distanceKm = totalDistanceKm,
                                dateRange =
                                        "${startDate.year}.${startDate.monthValue} - ${endDate.year}.${endDate.monthValue}",
                                artName = uiState.artAuthorName,
                                artFont = uiState.artFontName,
                                artColor = selectedColor,
                                textColor = uiState.artTextColor,
                                isItalic = uiState.artTextItalic,
                                hasBorder = uiState.artTextBorder,
                                modifier = Modifier.fillMaxSize()
                        )

                        // Top Bar & Export Button
                        if (showControls) {
                                Box(
                                        modifier =
                                                Modifier.fillMaxSize()
                                                        .padding(
                                                                top = 48.dp,
                                                                start = 16.dp,
                                                                end = 16.dp
                                                        )
                                ) {
                                        SmallFloatingActionButton(
                                                onClick = onBack,
                                                modifier = Modifier.align(Alignment.TopStart),
                                                containerColor = Color.Black.copy(alpha = 0.5f)
                                        ) {
                                                Icon(
                                                        Icons.AutoMirrored.Filled.ArrowBack,
                                                        "Back",
                                                        tint = Color.White
                                                )
                                        }

                                        ExtendedFloatingActionButton(
                                                onClick = {
                                                        android.widget.Toast.makeText(
                                                                        context,
                                                                        "正在渲染极清海报...",
                                                                        android.widget.Toast
                                                                                .LENGTH_LONG
                                                                )
                                                                .show()
                                                        showControls = false
                                                        android.os.Handler(
                                                                        android.os.Looper
                                                                                .getMainLooper()
                                                                )
                                                                .postDelayed(
                                                                        {
                                                                                val amap =
                                                                                        mapView.map
                                                                                val centerLat =
                                                                                        amap?.cameraPosition
                                                                                                ?.target
                                                                                                ?.latitude
                                                                                                ?: 39.9
                                                                                val centerLng =
                                                                                        amap?.cameraPosition
                                                                                                ?.target
                                                                                                ?.longitude
                                                                                                ?: 116.3
                                                                                val viewWidthStr =
                                                                                        mapView.width
                                                                                                .coerceAtLeast(
                                                                                                        1080
                                                                                                )
                                                                                                .toDouble()
                                                                                val viewHeightStr =
                                                                                        mapView.height
                                                                                                .coerceAtLeast(
                                                                                                        1920
                                                                                                )
                                                                                                .toDouble()

                                                                                val maxDim =
                                                                                        viewWidthStr
                                                                                                .coerceAtLeast(
                                                                                                        viewHeightStr
                                                                                                )
                                                                                val targetDim =
                                                                                        6000.0 // Target maximum resolution for the longest side (e.g., 6K resolution)
                                                                                val scale =
                                                                                        (targetDim /
                                                                                                        maxDim)
                                                                                                .coerceAtLeast(
                                                                                                        1.0
                                                                                                )
                                                                                                .coerceAtMost(
                                                                                                        8.0
                                                                                                )

                                                                                val exportWidth =
                                                                                        (viewWidthStr *
                                                                                                        scale)
                                                                                                .toInt()
                                                                                val exportHeight =
                                                                                        (viewHeightStr *
                                                                                                        scale)
                                                                                                .toInt()
                                                                                val currentZoom =
                                                                                        amap?.cameraPosition
                                                                                                ?.zoom
                                                                                                ?.toDouble()
                                                                                                ?: 14.0
                                                                                val exportZoom =
                                                                                        currentZoom +
                                                                                                kotlin.math
                                                                                                        .log2(
                                                                                                                scale
                                                                                                        )

                                                                                val tJson =
                                                                                        org.json
                                                                                                .JSONArray()
                                                                                tracePoints
                                                                                        .forEach {
                                                                                                pt
                                                                                                ->
                                                                                                val obj =
                                                                                                        org.json
                                                                                                                .JSONObject()
                                                                                                obj.put(
                                                                                                        "lat",
                                                                                                        pt.latitude
                                                                                                )
                                                                                                obj.put(
                                                                                                        "lng",
                                                                                                        pt.longitude
                                                                                                )
                                                                                                tJson.put(
                                                                                                        obj
                                                                                                )
                                                                                        }
                                                                                val traceStr =
                                                                                        tJson.toString()

                                                                                val fileName =
                                                                                        "footprint_art_${System.currentTimeMillis()}.png"
                                                                                val outputFile =
                                                                                        java.io
                                                                                                .File(
                                                                                                        context.cacheDir,
                                                                                                        fileName
                                                                                                )

                                                                                val themeStr = when (mapStyle) {
                                                                                    ArtMapStyle.LIGHT -> "light"
                                                                                    ArtMapStyle.DARK -> "dark"
                                                                                    ArtMapStyle.SATELLITE -> "satellite"
                                                                                    ArtMapStyle.VOID -> "void"
                                                                                }

                                                                                val cValue =
                                                                                        when (uiState.artColorStyle
                                                                                        ) {
                                                                                                "White" ->
                                                                                                        android.graphics
                                                                                                                .Color
                                                                                                                .WHITE
                                                                                                "Black" ->
                                                                                                        android.graphics
                                                                                                                .Color
                                                                                                                .BLACK
                                                                                                "Gold" ->
                                                                                                        android.graphics
                                                                                                                .Color
                                                                                                                .parseColor(
                                                                                                                        "#FFD700"
                                                                                                                )
                                                                                                "Deep Blue" ->
                                                                                                        android.graphics
                                                                                                                .Color
                                                                                                                .parseColor(
                                                                                                                        "#00008B"
                                                                                                                )
                                                                                                else ->
                                                                                                        android.graphics
                                                                                                                .Color
                                                                                                                .parseColor(
                                                                                                                        "#FF453A"
                                                                                                                )
                                                                                        }
                                                                                val traceColorHex =
                                                                                        String.format(
                                                                                                "#%06X",
                                                                                                0xFFFFFF and
                                                                                                        cValue
                                                                                        )

                                                                                Thread {
                                                                                                val statusCode =
                                                                                                        com.footprint
                                                                                                                .utils
                                                                                                                .NativeRenderer
                                                                                                                .generateGigapixelMap(
                                                                                                                        outputFile
                                                                                                                                .absolutePath,
                                                                                                                        traceStr,
                                                                                                                        themeStr,
                                                                                                                        traceColorHex,
                                                                                                                        glowRadius,
                                                                                                                        exportWidth,
                                                                                                                        exportHeight,
                                                                                                                        centerLat,
                                                                                                                        centerLng,
                                                                                                                        exportZoom
                                                                                                                )

                                                                                                val mainHandler =
                                                                                                        android.os
                                                                                                                .Handler(
                                                                                                                        android.os
                                                                                                                                .Looper
                                                                                                                                .getMainLooper()
                                                                                                                )
                                                                                                if (statusCode ==
                                                                                                                0 &&
                                                                                                                outputFile
                                                                                                                        .exists()
                                                                                                ) {
                                                                                                        val opt =
                                                                                                                android.graphics
                                                                                                                        .BitmapFactory
                                                                                                                        .Options()
                                                                                                        opt.inMutable =
                                                                                                                true
                                                                                                        val bitmap =
                                                                                                                android.graphics
                                                                                                                        .BitmapFactory
                                                                                                                        .decodeFile(
                                                                                                                                outputFile
                                                                                                                                        .absolutePath,
                                                                                                                                opt
                                                                                                                        )
                                                                                                        if (bitmap !=
                                                                                                                        null
                                                                                                        ) {
                                                                                                                val canvas =
                                                                                                                        android.graphics
                                                                                                                                .Canvas(
                                                                                                                                        bitmap
                                                                                                                                )
                                                                                                                val typefaceId =
                                                                                                                        when (uiState.artFontName
                                                                                                                        ) {
                                                                                                                                "MaShanZheng" ->
                                                                                                                                        com.footprint
                                                                                                                                                .R
                                                                                                                                                .font
                                                                                                                                                .ma_shan_zheng
                                                                                                                                "ZhiMangXing" ->
                                                                                                                                        com.footprint
                                                                                                                                                .R
                                                                                                                                                .font
                                                                                                                                                .zhi_mang_xing
                                                                                                                                "LongCang" ->
                                                                                                                                        com.footprint
                                                                                                                                                .R
                                                                                                                                                .font
                                                                                                                                                .long_cang
                                                                                                                                "LiuJianMaoCao" ->
                                                                                                                                        com.footprint
                                                                                                                                                .R
                                                                                                                                                .font
                                                                                                                                                .liu_jian_mao_cao
                                                                                                                                "ZCOOLXiaoWei" ->
                                                                                                                                        com.footprint
                                                                                                                                                .R
                                                                                                                                                .font
                                                                                                                                                .zcool_xiao_wei
                                                                                                                                else ->
                                                                                                                                        null
                                                                                                                        }
                                                                                                                val customTypeface =
                                                                                                                        typefaceId
                                                                                                                                ?.let {
                                                                                                                                        androidx.core
                                                                                                                                                .content
                                                                                                                                                .res
                                                                                                                                                .ResourcesCompat
                                                                                                                                                .getFont(
                                                                                                                                                        context,
                                                                                                                                                        it
                                                                                                                                                )
                                                                                                                                }
                                                                                                                                ?: android.graphics
                                                                                                                                        .Typeface
                                                                                                                                        .DEFAULT

                                                                                                                val textCValue =
                                                                                                                        when (uiState.artTextColor
                                                                                                                        ) {
                                                                                                                                "Black" ->
                                                                                                                                        android.graphics
                                                                                                                                                .Color
                                                                                                                                                .BLACK
                                                                                                                                "Gold" ->
                                                                                                                                        android.graphics
                                                                                                                                                .Color
                                                                                                                                                .parseColor(
                                                                                                                                                        "#FFCC00"
                                                                                                                                                )
                                                                                                                                "Deep Blue" ->
                                                                                                                                        android.graphics
                                                                                                                                                .Color
                                                                                                                                                .parseColor(
                                                                                                                                                        "#007AFF"
                                                                                                                                                )
                                                                                                                                else ->
                                                                                                                                        android.graphics
                                                                                                                                                .Color
                                                                                                                                                .WHITE
                                                                                                                        }

                                                                                                                val textPaint =
                                                                                                                        android.graphics
                                                                                                                                .Paint()
                                                                                                                                .apply {
                                                                                                                                        color =
                                                                                                                                                textCValue
                                                                                                                                        textSize =
                                                                                                                                                240f *
                                                                                                                                                        scale.toFloat()
                                                                                                                                        isAntiAlias =
                                                                                                                                                true
                                                                                                                                        typeface =
                                                                                                                                                customTypeface
                                                                                                                                        setShadowLayer(
                                                                                                                                                8f *
                                                                                                                                                        scale.toFloat(),
                                                                                                                                                0f,
                                                                                                                                                6f *
                                                                                                                                                        scale.toFloat(),
                                                                                                                                                android.graphics
                                                                                                                                                        .Color
                                                                                                                                                        .BLACK
                                                                                                                                        )
                                                                                                                                }

                                                                                                                val textWidth =
                                                                                                                        textPaint
                                                                                                                                .measureText(
                                                                                                                                        uiState.artAuthorName
                                                                                                                                )
                                                                                                                val x =
                                                                                                                        (bitmap.width -
                                                                                                                                textWidth) /
                                                                                                                                2f
                                                                                                                val y =
                                                                                                                        bitmap.height -
                                                                                                                                (180f *
                                                                                                                                        scale.toFloat())
                                                                                                                canvas.drawText(
                                                                                                                        uiState.artAuthorName,
                                                                                                                        x,
                                                                                                                        y,
                                                                                                                        textPaint
                                                                                                                )

                                                                                                                com.footprint
                                                                                                                        .utils
                                                                                                                        .ExportUtils
                                                                                                                        .saveBitmapToGallery(
                                                                                                                                context,
                                                                                                                                bitmap
                                                                                                                        )
                                                                                                        }
                                                                                                        mainHandler
                                                                                                                .post {
                                                                                                                        android.widget
                                                                                                                                .Toast
                                                                                                                                .makeText(
                                                                                                                                        context,
                                                                                                                                        "超清海报已保存",
                                                                                                                                        android.widget
                                                                                                                                                .Toast
                                                                                                                                                .LENGTH_SHORT
                                                                                                                                )
                                                                                                                                .show()
                                                                                                                        showControls =
                                                                                                                                true
                                                                                                                }
                                                                                                } else {
                                                                                                        val errorMsg =
                                                                                                                when (statusCode
                                                                                                                ) {
                                                                                                                        -1 ->
                                                                                                                                "参数传递失败 (JNI Error)"
                                                                                                                        1 ->
                                                                                                                                "轨迹格式错误 (JSON Parse Error)"
                                                                                                                        2 ->
                                                                                                                                "无法创建输出文件 (File Create Error)"
                                                                                                                        3 ->
                                                                                                                                "图片写入失败 (PNG Header Error)"
                                                                                                                        4 ->
                                                                                                                                "内存分配失败 (Skia Allocation Error)"
                                                                                                                        5 ->
                                                                                                                                "图像块渲染失败 (Chunk Write Error)"
                                                                                                                        else ->
                                                                                                                                "海报渲染失败, 代码: $statusCode"
                                                                                                                }
                                                                                                        mainHandler
                                                                                                                .post {
                                                                                                                        android.widget
                                                                                                                                .Toast
                                                                                                                                .makeText(
                                                                                                                                        context,
                                                                                                                                        errorMsg,
                                                                                                                                        android.widget
                                                                                                                                                .Toast
                                                                                                                                                .LENGTH_SHORT
                                                                                                                                )
                                                                                                                                .show()
                                                                                                                        showControls =
                                                                                                                                true
                                                                                                                }
                                                                                                }
                                                                                        }
                                                                                        .start()
                                                                        },
                                                                        200
                                                                )
                                                },
                                                modifier = Modifier.align(Alignment.TopEnd),
                                                containerColor = selectedColor,
                                                contentColor = Color.Black
                                        ) {
                                                Icon(Icons.Default.Download, "导出")
                                                Spacer(Modifier.width(8.dp))
                                                Text("导出高清海报")
                                        }
                                }
                        }
                }
        }
}

@Composable
fun ArtLayoutOverlay(
        layout: ArtLayout,
        distanceKm: Double,
        dateRange: String,
        artName: String,
        artFont: String,
        artColor: Color,
        textColor: String,
        isItalic: Boolean,
        hasBorder: Boolean,
        modifier: Modifier = Modifier
) {
        val fontFamily =
                remember(artFont) {
                        when (artFont) {
                                "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                                "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
                                "MaShanZheng" ->
                                        androidx.compose.ui.text.font.FontFamily(
                                                androidx.compose.ui.text.font.Font(
                                                        com.footprint.R.font.ma_shan_zheng,
                                                        FontWeight.Normal
                                                )
                                        )
                                "ZhiMangXing" ->
                                        androidx.compose.ui.text.font.FontFamily(
                                                androidx.compose.ui.text.font.Font(
                                                        com.footprint.R.font.zhi_mang_xing,
                                                        FontWeight.Normal
                                                )
                                        )
                                "LongCang" ->
                                        androidx.compose.ui.text.font.FontFamily(
                                                androidx.compose.ui.text.font.Font(
                                                        com.footprint.R.font.long_cang,
                                                        FontWeight.Normal
                                                )
                                        )
                                "LiuJianMaoCao" ->
                                        androidx.compose.ui.text.font.FontFamily(
                                                androidx.compose.ui.text.font.Font(
                                                        com.footprint.R.font.liu_jian_mao_cao,
                                                        FontWeight.Normal
                                                )
                                        )
                                "ZCOOLXiaoWei" ->
                                        androidx.compose.ui.text.font.FontFamily(
                                                androidx.compose.ui.text.font.Font(
                                                        com.footprint.R.font.zcool_xiao_wei,
                                                        FontWeight.Normal
                                                )
                                        )
                                else -> androidx.compose.ui.text.font.FontFamily.Default
                        }
                }

        val actualTextColor =
                when (textColor) {
                        "Black" -> Color.Black
                        "Gold" -> Color(0xFFFFCC00)
                        "Deep Blue" -> Color(0xFF007AFF)
                        "White" -> Color.White
                        else -> Color.White
                }

        val fontStyle =
                if (isItalic) androidx.compose.ui.text.font.FontStyle.Italic
                else androidx.compose.ui.text.font.FontStyle.Normal

        val isCalligraphy =
                artFont in
                        listOf(
                                "MaShanZheng",
                                "ZhiMangXing",
                                "LongCang",
                                "Cursive",
                                "LiuJianMaoCao",
                                "ZCOOLXiaoWei"
                        )
        when (layout) {
                ArtLayout.FULLCREEN_A24 -> {
                        Box(
                                modifier = modifier.fillMaxSize().padding(bottom = 120.dp),
                                contentAlignment = Alignment.BottomCenter
                        ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val baseStyle =
                                                MaterialTheme.typography.headlineLarge.copy(
                                                        fontWeight =
                                                                if (isCalligraphy) FontWeight.Normal
                                                                else FontWeight.Black,
                                                        letterSpacing = 4.sp
                                                )
                                        Box {
                                                if (hasBorder) {
                                                        Text(
                                                                artName.ifBlank { "漂泊的灵魂" },
                                                                style =
                                                                        baseStyle.copy(
                                                                                color =
                                                                                        Color.Black
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.8f
                                                                                                ),
                                                                                drawStyle =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .drawscope
                                                                                                .Stroke(
                                                                                                        width =
                                                                                                                6f,
                                                                                                        join =
                                                                                                                androidx.compose
                                                                                                                        .ui
                                                                                                                        .graphics
                                                                                                                        .StrokeJoin
                                                                                                                        .Round
                                                                                                )
                                                                        ),
                                                                fontFamily = fontFamily,
                                                                fontStyle = fontStyle
                                                        )
                                                }
                                                Text(
                                                        artName.ifBlank { "漂泊的灵魂" },
                                                        style =
                                                                baseStyle.copy(
                                                                        color = actualTextColor
                                                                ),
                                                        fontFamily = fontFamily,
                                                        fontStyle = fontStyle
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                "$dateRange • %.1f KM".format(distanceKm),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White.copy(alpha = 0.7f),
                                                letterSpacing = 2.sp
                                        )
                                }
                        }
                }
                ArtLayout.POLAROID -> {
                        Box(modifier = modifier.fillMaxSize()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                        // Polaroid Proportions: Roughly 3.5 width x 4.2 height
                                        // We want a large map area.

                                        val cardMargin = size.width * 0.05f
                                        val cardWidth = size.width - (cardMargin * 2)

                                        // Frame Color
                                        val paperColor = Color(0xFFFAFAFA) // Off-white photo paper

                                        // Hole (Map Area)
                                        val mapAspect = 1f // Square map
                                        val mapWidth = cardWidth * 0.9f
                                        val mapHeight = mapWidth * mapAspect

                                        val mapLeft = (size.width - mapWidth) / 2
                                        val mapTop = size.height * 0.15f
                                        val mapRight = mapLeft + mapWidth
                                        val mapBottom = mapTop + mapHeight

                                        // Draw the Mask (The Frame) - 4 Rects
                                        // 1. Top Area (Above Map)
                                        drawRect(
                                                paperColor,
                                                topLeft = Offset.Zero,
                                                size =
                                                        androidx.compose.ui.geometry.Size(
                                                                size.width,
                                                                mapTop
                                                        )
                                        )
                                        // 2. Bottom Area (Below Map) - This is the "Chin" where
                                        // text goes
                                        drawRect(
                                                paperColor,
                                                topLeft = Offset(0f, mapBottom),
                                                size =
                                                        androidx.compose.ui.geometry.Size(
                                                                size.width,
                                                                size.height - mapBottom
                                                        )
                                        )
                                        // 3. Left Area (Left of Map)
                                        drawRect(
                                                paperColor,
                                                topLeft = Offset(0f, mapTop),
                                                size =
                                                        androidx.compose.ui.geometry.Size(
                                                                mapLeft,
                                                                mapHeight
                                                        )
                                        )
                                        // 4. Right Area (Right of Map)
                                        drawRect(
                                                paperColor,
                                                topLeft = Offset(mapRight, mapTop),
                                                size =
                                                        androidx.compose.ui.geometry.Size(
                                                                size.width - mapRight,
                                                                mapHeight
                                                        )
                                        )

                                        // Add subtle inner shadow/border for depth
                                        drawRect(
                                                color = Color.Black.copy(alpha = 0.1f),
                                                topLeft = Offset(mapLeft, mapTop),
                                                size =
                                                        androidx.compose.ui.geometry.Size(
                                                                mapWidth,
                                                                mapHeight
                                                        ),
                                                style = Stroke(width = 2f)
                                        )
                                }

                                // Polaroid Text
                                Column(
                                        modifier =
                                                Modifier.align(Alignment.BottomCenter)
                                                        .padding(bottom = 120.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        val baseStyle =
                                                MaterialTheme.typography.headlineLarge.copy(
                                                        fontWeight =
                                                                if (isCalligraphy) FontWeight.Normal
                                                                else FontWeight.Bold
                                                )
                                        Box {
                                                if (hasBorder) {
                                                        Text(
                                                                artName.ifBlank { "My Journey" },
                                                                style =
                                                                        baseStyle.copy(
                                                                                color =
                                                                                        Color.Black
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.8f
                                                                                                ),
                                                                                drawStyle =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .drawscope
                                                                                                .Stroke(
                                                                                                        width =
                                                                                                                6f,
                                                                                                        join =
                                                                                                                androidx.compose
                                                                                                                        .ui
                                                                                                                        .graphics
                                                                                                                        .StrokeJoin
                                                                                                                        .Round
                                                                                                )
                                                                        ),
                                                                fontFamily = fontFamily,
                                                                fontStyle = fontStyle
                                                        )
                                                }
                                                Text(
                                                        artName.ifBlank { "My Journey" },
                                                        style =
                                                                baseStyle.copy(
                                                                        color = actualTextColor
                                                                ),
                                                        fontFamily = fontFamily,
                                                        fontStyle = fontStyle
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                "$dateRange • %.1f km".format(distanceKm),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = Color.Gray
                                        )
                                }
                        }
                }
                ArtLayout.GEEK_STATS -> {
                        Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
                                Column(
                                        modifier =
                                                Modifier.align(Alignment.TopEnd)
                                                        .background(
                                                                Color.Black.copy(alpha = 0.7f),
                                                                RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(16.dp)
                                ) {
                                        val baseStyle =
                                                MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight =
                                                                if (isCalligraphy) FontWeight.Normal
                                                                else FontWeight.Bold,
                                                        letterSpacing = 2.sp
                                                )
                                        Box {
                                                if (hasBorder) {
                                                        Text(
                                                                artName.uppercase().ifBlank {
                                                                        "DATA VISUALIZATION"
                                                                },
                                                                style =
                                                                        baseStyle.copy(
                                                                                color =
                                                                                        Color.Black
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.8f
                                                                                                ),
                                                                                drawStyle =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .drawscope
                                                                                                .Stroke(
                                                                                                        width =
                                                                                                                4f,
                                                                                                        join =
                                                                                                                androidx.compose
                                                                                                                        .ui
                                                                                                                        .graphics
                                                                                                                        .StrokeJoin
                                                                                                                        .Round
                                                                                                )
                                                                        ),
                                                                fontFamily = fontFamily,
                                                                fontStyle = fontStyle
                                                        )
                                                }
                                                Text(
                                                        artName.uppercase().ifBlank {
                                                                "DATA VISUALIZATION"
                                                        },
                                                        style =
                                                                baseStyle.copy(
                                                                        color = actualTextColor
                                                                ),
                                                        fontFamily = fontFamily,
                                                        fontStyle = fontStyle
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                "DIST: %.2f KM".format(distanceKm),
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily =
                                                        androidx.compose.ui.text.font.FontFamily
                                                                .Monospace
                                        )
                                        Text(
                                                "DATE: $dateRange",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily =
                                                        androidx.compose.ui.text.font.FontFamily
                                                                .Monospace
                                        )
                                        Text(
                                                "MODE: TRACKING",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily =
                                                        androidx.compose.ui.text.font.FontFamily
                                                                .Monospace
                                        )
                                }

                                // Decorative elements
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                        val color = artColor.copy(alpha = 0.5f)
                                        // Corner brackets
                                        val length = 40.dp.toPx()
                                        val stroke = 2.dp.toPx()

                                        // Top Left
                                        drawLine(color, Offset(0f, 0f), Offset(length, 0f), stroke)
                                        drawLine(color, Offset(0f, 0f), Offset(0f, length), stroke)

                                        // Bottom Right
                                        drawLine(
                                                color,
                                                Offset(size.width, size.height),
                                                Offset(size.width - length, size.height),
                                                stroke
                                        )
                                        drawLine(
                                                color,
                                                Offset(size.width, size.height),
                                                Offset(size.width, size.height - length),
                                                stroke
                                        )
                                }
                        }
                }
        }
}
