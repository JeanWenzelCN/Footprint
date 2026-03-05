package com.footprint.ui.screens.art

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.footprint.utils.HolographicExportUtils
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import org.intellij.lang.annotations.Language

@Language("AGSL")
const val CYBER_GLITCH_SHADER =
        """
    uniform shader composable;
    uniform vec2 uResolution;
    uniform float uTime;

    float hash(vec2 p) {
        return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
    }

    vec4 main(vec2 fragCoord) {
        vec2 uv = fragCoord / uResolution;
        
        // Horizontal Glitch Blocks
        float glitch = step(0.98, hash(vec2(floor(uv.y * 50.0), floor(uTime * 10.0))));
        float offset = glitch * 0.02 * sin(uTime * 20.0);
        
        // Scanlines
        float scanline = sin(uv.y * 800.0) * 0.04;
        
        // Sampling with offset
        vec4 color = composable.eval(fragCoord + vec2(offset * uResolution.x, 0.0));
        
        // Neon tint
        color.rgb += vec3(0.0, 0.1, 0.2); // Base cyberpunk blue
        
        // Random color bursts
        if (glitch > 0.0) {
            color.r += 0.2;
            color.b += 0.3;
        }
        
        return vec4(color.rgb - scanline, color.a);
    }
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootprintArtStudioScreen(viewModel: FootprintViewModel, onBack: () -> Unit) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
        val mapView = remember { TextureMapView(context).apply { onCreate(null) } }
        val hazeState = remember { HazeState() }
        val haptic = LocalHapticFeedback.current

        // Haptic feedback for Woodcraft Studio
        LaunchedEffect(uiState.polaroidFrameStyle) {
                if (uiState.polaroidFrameStyle == "ACOUSTIC_WOOD" && uiState.hapticFeedbackEnabled
                ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
        }

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

        // State for storage picking
        var pendingExportHtml by remember { mutableStateOf<String?>(null) }
        val createDocumentLauncher =
                rememberLauncherForActivityResult(
                        ActivityResultContracts.CreateDocument("text/html")
                ) { uri ->
                        uri?.let {
                                try {
                                        context.contentResolver.openOutputStream(it)?.use { os ->
                                                OutputStreamWriter(os).use { writer ->
                                                        writer.write(pendingExportHtml ?: "")
                                                }
                                        }
                                        android.widget.Toast.makeText(
                                                        context,
                                                        "文件已成功导出",
                                                        android.widget.Toast.LENGTH_SHORT
                                                )
                                                .show()
                                } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                                        context,
                                                        "导出失败: ${e.message}",
                                                        android.widget.Toast.LENGTH_SHORT
                                                )
                                                .show()
                                }
                        }
                        pendingExportHtml = null
                }

        // State for controls

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

        // Material 3 DatePicker dialog state
        var showStartDatePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }

        // Time Scope (Default to this year)
        var startDate by remember { mutableStateOf(LocalDate.now().withDayOfYear(1)) }
        var endDate by remember { mutableStateOf(LocalDate.now()) }
        val startTimestamp =
                remember(startDate) {
                        startDate
                                .atStartOfDay(java.time.ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                }
        val endTimestamp =
                remember(endDate) {
                        endDate.atTime(java.time.LocalTime.MAX)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
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

                // Ensure map is visible (fix for map disappearing)
                map.isMyLocationEnabled = false

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

                        // 3. Move Camera - Only if points changed significantly
                        if (tracePoints.isNotEmpty()) {
                                try {
                                        val builder = LatLngBounds.builder()
                                        latLngs.forEach { builder.include(it) }
                                        val bounds = builder.build()
                                        // Use a slight delay to ensure map is ready
                                        android.os.Handler(android.os.Looper.getMainLooper())
                                                .postDelayed(
                                                        {
                                                                try {
                                                                        map.moveCamera(
                                                                                CameraUpdateFactory
                                                                                        .newLatLngBounds(
                                                                                                bounds,
                                                                                                150
                                                                                        )
                                                                        )
                                                                } catch (e: Exception) {
                                                                        map.moveCamera(
                                                                                CameraUpdateFactory
                                                                                        .newLatLngZoom(
                                                                                                latLngs[
                                                                                                        0],
                                                                                                12f
                                                                                        )
                                                                        )
                                                                }
                                                        },
                                                        100
                                                )
                                } catch (e: Exception) {
                                        // Ignore bound errors
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
                sheetContainerColor = MaterialTheme.colorScheme.surface,
                sheetDragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle() },
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
                                        onStartDateClick = { showStartDatePicker = true },
                                        onEndDateClick = { showEndDatePicker = true },
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
                                        accentColor = selectedColor,
                                        polaroidFrameStyle = uiState.polaroidFrameStyle,
                                        onPolaroidFrameStyleChange = {
                                                viewModel.updatePolaroidSettings(
                                                        it,
                                                        uiState.polaroidFramePadding,
                                                        uiState.polaroidInnerBorder
                                                )
                                        },
                                        polaroidFramePadding = uiState.polaroidFramePadding,
                                        onPolaroidFramePaddingChange = {
                                                viewModel.updatePolaroidSettings(
                                                        uiState.polaroidFrameStyle,
                                                        it,
                                                        uiState.polaroidInnerBorder
                                                )
                                        },
                                        polaroidInnerBorder = uiState.polaroidInnerBorder,
                                        onPolaroidInnerBorderChange = {
                                                viewModel.updatePolaroidSettings(
                                                        uiState.polaroidFrameStyle,
                                                        uiState.polaroidFramePadding,
                                                        it
                                                )
                                        },
                                        woodType = uiState.woodType,
                                        onWoodTypeChange = {
                                                viewModel.updateWoodSettings(
                                                        it,
                                                        uiState.engravingDepth,
                                                        uiState.canvasGrain
                                                )
                                        },
                                        engravingDepth = uiState.engravingDepth,
                                        onEngravingDepthChange = {
                                                viewModel.updateWoodSettings(
                                                        uiState.woodType,
                                                        it,
                                                        uiState.canvasGrain
                                                )
                                        },
                                        canvasGrain = uiState.canvasGrain,
                                        onCanvasGrainChange = {
                                                viewModel.updateWoodSettings(
                                                        uiState.woodType,
                                                        uiState.engravingDepth,
                                                        it
                                                )
                                        },
                                        armorType = uiState.armorType,
                                        onArmorTypeChange = {
                                                viewModel.updateMechanicalSettings(
                                                        it,
                                                        uiState.mechanicalSeams,
                                                        uiState.hasHazardStriping
                                                )
                                        },
                                        mechanicalSeams = uiState.mechanicalSeams,
                                        onMechanicalSeamsChange = {
                                                viewModel.updateMechanicalSettings(
                                                        uiState.armorType,
                                                        it,
                                                        uiState.hasHazardStriping
                                                )
                                        },
                                        hasHazardStriping = uiState.hasHazardStriping,
                                        onHasHazardStripingChange = {
                                                viewModel.updateMechanicalSettings(
                                                        uiState.armorType,
                                                        uiState.mechanicalSeams,
                                                        it
                                                )
                                        },
                                )
                        }
                }
        ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        // 1. Base Map Layer
                        AndroidView(
                                factory = { mapView },
                                modifier = Modifier.fillMaxSize().haze(hazeState)
                        )

                        // 3. Layout Overlay
                        ArtLayoutOverlay(
                                layout = selectedLayout,
                                distanceKm = totalDistanceKm,
                                dateRange =
                                        "${startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))} - ${endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))}",
                                artName = uiState.artAuthorName,
                                artFont = uiState.artFontName,
                                artColor = selectedColor,
                                textColor = uiState.artTextColor,
                                isItalic = uiState.artTextItalic,
                                hasBorder = uiState.artTextBorder,
                                modifier = Modifier.fillMaxSize(),
                                polaroidFrameStyle = uiState.polaroidFrameStyle,
                                polaroidFramePadding = uiState.polaroidFramePadding,
                                polaroidInnerBorder = uiState.polaroidInnerBorder,
                                woodType = uiState.woodType,
                                engravingDepth = uiState.engravingDepth,
                                canvasGrain = uiState.canvasGrain,
                                armorType = uiState.armorType,
                                mechanicalSeams = uiState.mechanicalSeams,
                                hasHazardStriping = uiState.hasHazardStriping,
                                userNickname = uiState.userNickname,
                                hazeState = hazeState
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
                                        Row(
                                                modifier = Modifier.align(Alignment.TopEnd),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                                // 1. Digital Poster Export (Static PNG)
                                                ExtendedFloatingActionButton(
                                                        onClick = {
                                                                android.widget.Toast.makeText(
                                                                                context,
                                                                                "正在渲染极清海报...",
                                                                                android.widget.Toast
                                                                                        .LENGTH_SHORT
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

                                                                                        // Calculate
                                                                                        // target
                                                                                        // resolution and scale
                                                                                        val viewWidth =
                                                                                                mapView.width
                                                                                                        .coerceAtLeast(
                                                                                                                1080
                                                                                                        )
                                                                                                        .toDouble()
                                                                                        val viewHeight =
                                                                                                mapView.height
                                                                                                        .coerceAtLeast(
                                                                                                                1920
                                                                                                        )
                                                                                                        .toDouble()
                                                                                        val maxDim =
                                                                                                if (viewWidth >
                                                                                                                viewHeight
                                                                                                )
                                                                                                        viewWidth
                                                                                                else
                                                                                                        viewHeight
                                                                                        val targetDim =
                                                                                                6000.0 // 6K Resolution Goal
                                                                                        val scale =
                                                                                                (targetDim /
                                                                                                                maxDim)
                                                                                                        .coerceIn(
                                                                                                                1.0,
                                                                                                                8.0
                                                                                                        )

                                                                                        val exportWidth =
                                                                                                (viewWidth *
                                                                                                                scale)
                                                                                                        .toInt()
                                                                                        val exportHeight =
                                                                                                (viewHeight *
                                                                                                                scale)
                                                                                                        .toInt()
                                                                                        val currentZoom =
                                                                                                amap?.cameraPosition
                                                                                                        ?.zoom
                                                                                                        ?.toDouble()
                                                                                                        ?: 14.0
                                                                                        val exportZoom =
                                                                                                currentZoom +
                                                                                                        Math.log(
                                                                                                                scale
                                                                                                        ) /
                                                                                                                Math.log(
                                                                                                                        2.0
                                                                                                                )

                                                                                        // Format
                                                                                        // trace
                                                                                        // data
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

                                                                                        val themeStr =
                                                                                                when (mapStyle
                                                                                                ) {
                                                                                                        ArtMapStyle
                                                                                                                .LIGHT ->
                                                                                                                "light"
                                                                                                        ArtMapStyle
                                                                                                                .DARK ->
                                                                                                                "dark"
                                                                                                        ArtMapStyle
                                                                                                                .SATELLITE ->
                                                                                                                "satellite"
                                                                                                        ArtMapStyle
                                                                                                                .VOID ->
                                                                                                                "void"
                                                                                                }

                                                                                        val traceColorInt =
                                                                                                when (uiState.artColorStyle
                                                                                                ) {
                                                                                                        "Deep Blue" ->
                                                                                                                android.graphics
                                                                                                                        .Color
                                                                                                                        .parseColor(
                                                                                                                                "#007AFF"
                                                                                                                        )
                                                                                                        "Cyber Pink" ->
                                                                                                                android.graphics
                                                                                                                        .Color
                                                                                                                        .parseColor(
                                                                                                                                "#FF2D55"
                                                                                                                        )
                                                                                                        "Neon Green" ->
                                                                                                                android.graphics
                                                                                                                        .Color
                                                                                                                        .parseColor(
                                                                                                                                "#00FF9F"
                                                                                                                        )
                                                                                                        "Gold" ->
                                                                                                                android.graphics
                                                                                                                        .Color
                                                                                                                        .parseColor(
                                                                                                                                "#FFCC00"
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
                                                                                                                traceColorInt
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

                                                                                        val finalTypeface =
                                                                                                if (uiState.artTextItalic
                                                                                                ) {
                                                                                                        android.graphics
                                                                                                                .Typeface
                                                                                                                .create(
                                                                                                                        customTypeface,
                                                                                                                        android.graphics
                                                                                                                                .Typeface
                                                                                                                                .ITALIC
                                                                                                                )
                                                                                                } else {
                                                                                                        customTypeface
                                                                                                }

                                                                                        // Perform
                                                                                        // heavy
                                                                                        // rendering
                                                                                        // in
                                                                                        // background thread
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
                                                                                                                                if (glowRadius >
                                                                                                                                                10f
                                                                                                                                )
                                                                                                                                        5.0f
                                                                                                                                else
                                                                                                                                        (glowRadius /
                                                                                                                                                2f),
                                                                                                                                exportWidth,
                                                                                                                                exportHeight,
                                                                                                                                centerLat,
                                                                                                                                centerLng,
                                                                                                                                exportZoom
                                                                                                                        )

                                                                                                        android.os
                                                                                                                .Handler(
                                                                                                                        android.os
                                                                                                                                .Looper
                                                                                                                                .getMainLooper()
                                                                                                                )
                                                                                                                .post {
                                                                                                                        if (statusCode ==
                                                                                                                                        0 &&
                                                                                                                                        outputFile
                                                                                                                                                .exists()
                                                                                                                        ) {
                                                                                                                                try {
                                                                                                                                        val opt =
                                                                                                                                                android.graphics
                                                                                                                                                        .BitmapFactory
                                                                                                                                                        .Options()
                                                                                                                                                        .apply {
                                                                                                                                                                inMutable =
                                                                                                                                                                        true
                                                                                                                                                        }
                                                                                                                                        val bitmap =
                                                                                                                                                android.graphics
                                                                                                                                                        .BitmapFactory
                                                                                                                                                        .decodeFile(
                                                                                                                                                                outputFile
                                                                                                                                                                        .absolutePath,
                                                                                                                                                                opt
                                                                                                                                                        )

                                                                                                                                        val canvas =
                                                                                                                                                android.graphics
                                                                                                                                                        .Canvas(
                                                                                                                                                                bitmap
                                                                                                                                                        )
                                                                                                                                        com.footprint
                                                                                                                                                .utils
                                                                                                                                                .ArtLayoutOverlayUtils
                                                                                                                                                .drawOverlay(
                                                                                                                                                        context =
                                                                                                                                                                context,
                                                                                                                                                        canvas =
                                                                                                                                                                canvas,
                                                                                                                                                        bitmap =
                                                                                                                                                                bitmap,
                                                                                                                                                        layout =
                                                                                                                                                                selectedLayout,
                                                                                                                                                        uiState =
                                                                                                                                                                uiState,
                                                                                                                                                        totalDistanceKm =
                                                                                                                                                                totalDistanceKm,
                                                                                                                                                        startDate =
                                                                                                                                                                startDate,
                                                                                                                                                        endDate =
                                                                                                                                                                endDate,
                                                                                                                                                        customTypeface =
                                                                                                                                                                finalTypeface,
                                                                                                                                                        scale =
                                                                                                                                                                scale
                                                                                                                                                )

                                                                                                                                        com.footprint
                                                                                                                                                .utils
                                                                                                                                                .ExportUtils
                                                                                                                                                .saveBitmapToGallery(
                                                                                                                                                        context,
                                                                                                                                                        bitmap
                                                                                                                                                )
                                                                                                                                        android.widget
                                                                                                                                                .Toast
                                                                                                                                                .makeText(
                                                                                                                                                        context,
                                                                                                                                                        "海报已保存至相册",
                                                                                                                                                        android.widget
                                                                                                                                                                .Toast
                                                                                                                                                                .LENGTH_SHORT
                                                                                                                                                )
                                                                                                                                                .show()
                                                                                                                                } catch (
                                                                                                                                        e:
                                                                                                                                                Exception) {
                                                                                                                                        android.widget
                                                                                                                                                .Toast
                                                                                                                                                .makeText(
                                                                                                                                                        context,
                                                                                                                                                        "渲染处理失败: ${e.message}",
                                                                                                                                                        android.widget
                                                                                                                                                                .Toast
                                                                                                                                                                .LENGTH_SHORT
                                                                                                                                                )
                                                                                                                                                .show()
                                                                                                                                }
                                                                                                                        } else {
                                                                                                                                android.widget
                                                                                                                                        .Toast
                                                                                                                                        .makeText(
                                                                                                                                                context,
                                                                                                                                                "海报生成失败 ($statusCode)",
                                                                                                                                                android.widget
                                                                                                                                                        .Toast
                                                                                                                                                        .LENGTH_SHORT
                                                                                                                                        )
                                                                                                                                        .show()
                                                                                                                        }
                                                                                                                        showControls =
                                                                                                                                true
                                                                                                                }
                                                                                                }
                                                                                                .start()
                                                                                },
                                                                                200
                                                                        )
                                                        },
                                                        containerColor = selectedColor,
                                                        contentColor = Color.Black
                                                ) {
                                                        Icon(
                                                                Icons.Default.Download,
                                                                contentDescription = null
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text("高清海报")
                                                }

                                                // 2. Holographic Scroll Export (Interactive HTML)
                                                ExtendedFloatingActionButton(
                                                        onClick = {
                                                                val amap = mapView.map
                                                                val center =
                                                                        amap?.cameraPosition?.target
                                                                                ?: LatLng(
                                                                                        39.9,
                                                                                        116.3
                                                                                )
                                                                val zoom =
                                                                        amap?.cameraPosition?.zoom
                                                                                ?.toDouble()
                                                                                ?: 14.0
                                                                val bounds =
                                                                        amap?.projection
                                                                                ?.visibleRegion
                                                                                ?.latLngBounds

                                                                val themeStr =
                                                                        when (mapStyle) {
                                                                                ArtMapStyle.LIGHT ->
                                                                                        "light"
                                                                                ArtMapStyle.DARK ->
                                                                                        "dark"
                                                                                ArtMapStyle
                                                                                        .SATELLITE ->
                                                                                        "satellite"
                                                                                ArtMapStyle.VOID ->
                                                                                        "void"
                                                                        }

                                                                val traceColorInt =
                                                                        when (uiState.artColorStyle
                                                                        ) {
                                                                                "Deep Blue" ->
                                                                                        android.graphics
                                                                                                .Color
                                                                                                .parseColor(
                                                                                                        "#007AFF"
                                                                                                )
                                                                                "Cyber Pink" ->
                                                                                        android.graphics
                                                                                                .Color
                                                                                                .parseColor(
                                                                                                        "#FF2D55"
                                                                                                )
                                                                                "Neon Green" ->
                                                                                        android.graphics
                                                                                                .Color
                                                                                                .parseColor(
                                                                                                        "#00FF9F"
                                                                                                )
                                                                                "Gold" ->
                                                                                        android.graphics
                                                                                                .Color
                                                                                                .parseColor(
                                                                                                        "#FFCC00"
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
                                                                                        traceColorInt
                                                                        )

                                                                val dateStr =
                                                                        "${startDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))} - ${endDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))}"
                                                                val metadata =
                                                                        "$dateStr • %.2f KM".format(
                                                                                totalDistanceKm
                                                                        )

                                                                val latLngPoints =
                                                                        tracePoints.map {
                                                                                LatLng(
                                                                                        it.latitude,
                                                                                        it.longitude
                                                                                )
                                                                        }

                                                                val htmlContent =
                                                                        com.footprint.utils
                                                                                .HolographicExportUtils
                                                                                .generateHolographicScrollContent(
                                                                                        context =
                                                                                                context,
                                                                                        title =
                                                                                                uiState.artAuthorName
                                                                                                        .ifBlank {
                                                                                                                "漂泊的灵魂"
                                                                                                        },
                                                                                        metadata =
                                                                                                metadata,
                                                                                        tracePoints =
                                                                                                latLngPoints,
                                                                                        mapStyle =
                                                                                                themeStr,
                                                                                        traceColor =
                                                                                                traceColorHex,
                                                                                        hasGlow =
                                                                                                glowRadius >
                                                                                                        5f,
                                                                                        initialCenter =
                                                                                                center,
                                                                                        initialZoom =
                                                                                                zoom,
                                                                                        uiState =
                                                                                                uiState,
                                                                                        artLayout =
                                                                                                selectedLayout,
                                                                                        totalDistanceKm =
                                                                                                totalDistanceKm,
                                                                                        bounds =
                                                                                                bounds
                                                                                )

                                                                if (htmlContent != null) {
                                                                        pendingExportHtml =
                                                                                htmlContent
                                                                        val timeStamp =
                                                                                SimpleDateFormat(
                                                                                                "yyyyMMdd_HHmm",
                                                                                                Locale.getDefault()
                                                                                        )
                                                                                        .format(
                                                                                                Date()
                                                                                        )
                                                                        val fileName =
                                                                                "Footprint_Art_$timeStamp.html"
                                                                        createDocumentLauncher
                                                                                .launch(fileName)
                                                                } else {
                                                                        android.widget.Toast
                                                                                .makeText(
                                                                                        context,
                                                                                        "全息画卷内容生成失败",
                                                                                        android.widget
                                                                                                .Toast
                                                                                                .LENGTH_SHORT
                                                                                )
                                                                                .show()
                                                                }
                                                        },
                                                        containerColor =
                                                                Color.Black.copy(alpha = 0.7f),
                                                        contentColor = selectedColor
                                                ) {
                                                        Icon(
                                                                com.footprint.ui.screens.art
                                                                        .ArtIcons.Holographic,
                                                                contentDescription = null
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text("全息画卷")
                                                }
                                        }
                                }
                        }
                }
        }

        // ── Material 3 Start Date Picker Dialog ──
        if (showStartDatePicker) {
                val startDatePickerState =
                        rememberDatePickerState(
                                initialSelectedDateMillis =
                                        startDate
                                                .atStartOfDay(java.time.ZoneId.of("UTC"))
                                                .toInstant()
                                                .toEpochMilli()
                        )
                DatePickerDialog(
                        onDismissRequest = { showStartDatePicker = false },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                startDatePickerState.selectedDateMillis?.let {
                                                        millis ->
                                                        startDate =
                                                                java.time.Instant.ofEpochMilli(
                                                                                millis
                                                                        )
                                                                        .atZone(
                                                                                java.time.ZoneId.of(
                                                                                        "UTC"
                                                                                )
                                                                        )
                                                                        .toLocalDate()
                                                }
                                                showStartDatePicker = false
                                        }
                                ) { Text("确定") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showStartDatePicker = false }) { Text("取消") }
                        }
                ) { DatePicker(state = startDatePickerState) }
        }

        // ── Material 3 End Date Picker Dialog ──
        if (showEndDatePicker) {
                val endDatePickerState =
                        rememberDatePickerState(
                                initialSelectedDateMillis =
                                        endDate.atStartOfDay(java.time.ZoneId.of("UTC"))
                                                .toInstant()
                                                .toEpochMilli()
                        )
                DatePickerDialog(
                        onDismissRequest = { showEndDatePicker = false },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                endDatePickerState.selectedDateMillis?.let { millis
                                                        ->
                                                        endDate =
                                                                java.time.Instant.ofEpochMilli(
                                                                                millis
                                                                        )
                                                                        .atZone(
                                                                                java.time.ZoneId.of(
                                                                                        "UTC"
                                                                                )
                                                                        )
                                                                        .toLocalDate()
                                                }
                                                showEndDatePicker = false
                                        }
                                ) { Text("确定") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showEndDatePicker = false }) { Text("取消") }
                        }
                ) { DatePicker(state = endDatePickerState) }
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
        modifier: Modifier = Modifier,
        polaroidFrameStyle: String = "CLASSIC_WHITE",
        polaroidFramePadding: Float = 0.5f,
        polaroidInnerBorder: Float = 1f,
        woodType: WoodType = WoodType.ASH,
        engravingDepth: Float = 0.5f,
        canvasGrain: Float = 0.3f,
        armorType: ArmorType = ArmorType.GUNMETAL,
        mechanicalSeams: Float = 0.5f,
        hasHazardStriping: Boolean = false,
        userNickname: String = "旅行者",
        hazeState: HazeState? = null
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

        val themeContentColor =
                when (polaroidFrameStyle) {
                        "ACOUSTIC_WOOD" -> Color(0xFF3E2723)
                        "CYBER_GLITCH" -> Color(0xFF00FFFF)
                        "HEAVY_MECHANICAL", "CLASSIC_BLACK" -> Color.White
                        "CLASSIC_WHITE" -> Color.Black
                        else -> Color.White
                }

        val actualTextColor =
                when (textColor) {
                        "Black" -> Color.Black
                        "Gold" -> Color(0xFFFFCC00)
                        "Deep Blue" -> Color(0xFF007AFF)
                        "White" -> Color.White
                        "Match Core" -> themeContentColor
                        else -> themeContentColor
                }

        val woodBaseColor =
                when (woodType) {
                        WoodType.ASH -> Color(0xFFE5D3B3)
                        WoodType.WALNUT -> Color(0xFF5D4037)
                        WoodType.VINTAGE_OAK -> Color(0xFFD2B48C)
                }

        val armorColor =
                when (armorType) {
                        ArmorType.GUNMETAL -> Color(0xFF455A64)
                        ArmorType.CARBON_FIBER -> Color(0xFF212121)
                        ArmorType.WORN_OLIVE -> Color(0xFF556B2F)
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
                                                "$dateRange • %.2f KM".format(distanceKm),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = actualTextColor.copy(alpha = 0.8f),
                                                letterSpacing = 1.sp,
                                                fontFamily = fontFamily
                                        )
                                }
                        }
                }
                ArtLayout.POLAROID -> {
                        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                                // Pull calculations up for both hazeChild and Canvas
                                val minPadding = constraints.maxWidth * 0.04f
                                val maxPadding = constraints.maxWidth * 0.15f
                                val sidePadding =
                                        minPadding +
                                                (maxPadding - minPadding) * polaroidFramePadding
                                val topPadding = sidePadding
                                val bottomPadding =
                                        constraints.maxHeight *
                                                (0.15f + 0.15f * polaroidFramePadding)

                                val mapWidth = constraints.maxWidth - (sidePadding * 2)
                                val mapHeight = constraints.maxHeight - topPadding - bottomPadding
                                val mapLeft = sidePadding
                                val mapTop = topPadding
                                val mapRight = mapLeft + mapWidth
                                val mapBottom = mapTop + mapHeight

                                // 1. Map Frame & Material logic
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                        // Material Colors

                                        val frameColor =
                                                when (polaroidFrameStyle) {
                                                        "CLASSIC_BLACK" -> Color(0xFF1A1A1A)
                                                        "ACOUSTIC_WOOD" -> woodBaseColor
                                                        "HEAVY_MECHANICAL" -> armorColor
                                                        "CYBER_GLITCH" -> Color(0xFF0F0F0F)
                                                        else -> Color(0xFFFAFAFA)
                                                }

                                        // Draw the Frame (Outer masking)
                                        drawRect(
                                                frameColor,
                                                topLeft = Offset.Zero,
                                                size =
                                                        androidx.compose.ui.geometry.Size(
                                                                size.width,
                                                                mapTop
                                                        )
                                        )
                                        drawRect(
                                                frameColor,
                                                topLeft = Offset(0f, mapBottom),
                                                size =
                                                        androidx.compose.ui.geometry.Size(
                                                                size.width,
                                                                size.height - mapBottom
                                                        )
                                        )
                                        drawRect(
                                                frameColor,
                                                topLeft = Offset(0f, mapTop),
                                                size =
                                                        androidx.compose.ui.geometry.Size(
                                                                mapLeft,
                                                                mapHeight
                                                        )
                                        )
                                        drawRect(
                                                frameColor,
                                                topLeft = Offset(mapRight, mapTop),
                                                size =
                                                        androidx.compose.ui.geometry.Size(
                                                                size.width - mapRight,
                                                                mapHeight
                                                        )
                                        )

                                        // Material Textures / Effects
                                        when (polaroidFrameStyle) {
                                                "ACOUSTIC_WOOD" -> {
                                                        // 1. Miter Joints & Directional Grain
                                                        val pathTop =
                                                                androidx.compose.ui.graphics.Path()
                                                                        .apply {
                                                                                moveTo(0f, 0f)
                                                                                lineTo(
                                                                                        size.width,
                                                                                        0f
                                                                                )
                                                                                lineTo(
                                                                                        mapRight,
                                                                                        mapTop
                                                                                )
                                                                                lineTo(
                                                                                        mapLeft,
                                                                                        mapTop
                                                                                )
                                                                                close()
                                                                        }
                                                        val pathBottom =
                                                                androidx.compose.ui.graphics.Path()
                                                                        .apply {
                                                                                moveTo(
                                                                                        0f,
                                                                                        size.height
                                                                                )
                                                                                lineTo(
                                                                                        size.width,
                                                                                        size.height
                                                                                )
                                                                                lineTo(
                                                                                        mapRight,
                                                                                        mapBottom
                                                                                )
                                                                                lineTo(
                                                                                        mapLeft,
                                                                                        mapBottom
                                                                                )
                                                                                close()
                                                                        }
                                                        val pathLeft =
                                                                androidx.compose.ui.graphics.Path()
                                                                        .apply {
                                                                                moveTo(0f, 0f)
                                                                                lineTo(
                                                                                        0f,
                                                                                        size.height
                                                                                )
                                                                                lineTo(
                                                                                        mapLeft,
                                                                                        mapBottom
                                                                                )
                                                                                lineTo(
                                                                                        mapLeft,
                                                                                        mapTop
                                                                                )
                                                                                close()
                                                                        }
                                                        val pathRight =
                                                                androidx.compose.ui.graphics.Path()
                                                                        .apply {
                                                                                moveTo(
                                                                                        size.width,
                                                                                        0f
                                                                                )
                                                                                lineTo(
                                                                                        size.width,
                                                                                        size.height
                                                                                )
                                                                                lineTo(
                                                                                        mapRight,
                                                                                        mapBottom
                                                                                )
                                                                                lineTo(
                                                                                        mapRight,
                                                                                        mapTop
                                                                                )
                                                                                close()
                                                                        }

                                                        // Draw grain for each section
                                                        fun DrawScope.drawSectionGrain(
                                                                path:
                                                                        androidx.compose.ui.graphics.Path,
                                                                isVertical: Boolean
                                                        ) {
                                                                clipPath(path) {
                                                                        val density =
                                                                                if (woodType ==
                                                                                                WoodType.VINTAGE_OAK
                                                                                )
                                                                                        60
                                                                                else 35
                                                                        for (i in 0 until density) {
                                                                                val offset =
                                                                                        (i.toFloat() /
                                                                                                density.toFloat()) *
                                                                                                (if (isVertical
                                                                                                )
                                                                                                        size.width
                                                                                                else
                                                                                                        size.height)
                                                                                if (isVertical) {
                                                                                        drawLine(
                                                                                                color =
                                                                                                        Color.Black
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.05f
                                                                                                                ),
                                                                                                start =
                                                                                                        Offset(
                                                                                                                offset,
                                                                                                                0f
                                                                                                        ),
                                                                                                end =
                                                                                                        Offset(
                                                                                                                offset +
                                                                                                                        size.width *
                                                                                                                                0.02f,
                                                                                                                size.height
                                                                                                        ),
                                                                                                strokeWidth =
                                                                                                        1.5f
                                                                                        )
                                                                                } else {
                                                                                        drawLine(
                                                                                                color =
                                                                                                        Color.Black
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.05f
                                                                                                                ),
                                                                                                start =
                                                                                                        Offset(
                                                                                                                0f,
                                                                                                                offset
                                                                                                        ),
                                                                                                end =
                                                                                                        Offset(
                                                                                                                size.width,
                                                                                                                offset +
                                                                                                                        size.height *
                                                                                                                                0.02f
                                                                                                        ),
                                                                                                strokeWidth =
                                                                                                        1.5f
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }

                                                        drawSectionGrain(pathTop, false)
                                                        drawSectionGrain(pathBottom, false)
                                                        drawSectionGrain(pathLeft, true)
                                                        drawSectionGrain(pathRight, true)

                                                        // 2. Bevel & Emboss (Inner Border
                                                        // Highlights)
                                                        drawLine(
                                                                Color.White.copy(alpha = 0.2f),
                                                                Offset(0f, 0f),
                                                                Offset(size.width, 0f),
                                                                2f
                                                        )
                                                        drawLine(
                                                                Color.White.copy(alpha = 0.2f),
                                                                Offset(0f, 0f),
                                                                Offset(0f, size.height),
                                                                2f
                                                        )
                                                        drawLine(
                                                                Color.Black.copy(alpha = 0.2f),
                                                                Offset(size.width, 0f),
                                                                Offset(size.width, size.height),
                                                                2f
                                                        )
                                                        drawLine(
                                                                Color.Black.copy(alpha = 0.2f),
                                                                Offset(0f, size.height),
                                                                Offset(size.width, size.height),
                                                                2f
                                                        )
                                                }
                                                "HEAVY_MECHANICAL" -> {
                                                        // 1. Chamfered Corners for the frame
                                                        val chamferSize = 24f
                                                        val framePath =
                                                                androidx.compose.ui.graphics.Path()
                                                                        .apply {
                                                                                // Outer Frame
                                                                                moveTo(
                                                                                        chamferSize,
                                                                                        0f
                                                                                )
                                                                                lineTo(
                                                                                        size.width -
                                                                                                chamferSize,
                                                                                        0f
                                                                                )
                                                                                lineTo(
                                                                                        size.width,
                                                                                        chamferSize
                                                                                )
                                                                                lineTo(
                                                                                        size.width,
                                                                                        size.height -
                                                                                                chamferSize
                                                                                )
                                                                                lineTo(
                                                                                        size.width -
                                                                                                chamferSize,
                                                                                        size.height
                                                                                )
                                                                                lineTo(
                                                                                        chamferSize,
                                                                                        size.height
                                                                                )
                                                                                lineTo(
                                                                                        0f,
                                                                                        size.height -
                                                                                                chamferSize
                                                                                )
                                                                                lineTo(
                                                                                        0f,
                                                                                        chamferSize
                                                                                )
                                                                                close()
                                                                                // Subtract Inner
                                                                                // Map Area
                                                                                addRect(
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .geometry
                                                                                                .Rect(
                                                                                                        mapLeft,
                                                                                                        mapTop,
                                                                                                        mapRight,
                                                                                                        mapBottom
                                                                                                )
                                                                                )
                                                                                fillType =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .PathFillType
                                                                                                .EvenOdd
                                                                        }
                                                        drawPath(framePath, frameColor)

                                                        // 2. Armor Texture (Noise/Brushed)
                                                        val noiseAlpha = 0.1f * canvasGrain
                                                        drawRect(
                                                                color =
                                                                        Color.Black.copy(
                                                                                alpha = noiseAlpha
                                                                        ),
                                                                blendMode =
                                                                        androidx.compose.ui.graphics
                                                                                .BlendMode.Overlay
                                                        )

                                                        // 3. Rivets along the seams
                                                        val rivetColor =
                                                                Color.Black.copy(alpha = 0.4f)
                                                        val rivetHighlight =
                                                                Color.White.copy(alpha = 0.2f)
                                                        val rivetRadius =
                                                                3f + (mechanicalSeams * 2f)
                                                        val rivetSpacing =
                                                                60f +
                                                                        (1.0f - mechanicalSeams) *
                                                                                100f

                                                        fun drawRivet(x: Float, y: Float) {
                                                                drawCircle(
                                                                        rivetColor,
                                                                        radius = rivetRadius,
                                                                        center = Offset(x, y)
                                                                )
                                                                drawCircle(
                                                                        rivetHighlight,
                                                                        radius = rivetRadius * 0.5f,
                                                                        center =
                                                                                Offset(
                                                                                        x - 1f,
                                                                                        y - 1f
                                                                                )
                                                                )
                                                        }

                                                        // Draw rivets at frame corners
                                                        drawRivet(mapLeft / 2, mapTop / 2)
                                                        drawRivet(
                                                                size.width - mapLeft / 2,
                                                                mapTop / 2
                                                        )
                                                        drawRivet(mapLeft / 2, size.height - 30f)
                                                        drawRivet(
                                                                size.width - mapLeft / 2,
                                                                size.height - 30f
                                                        )

                                                        // Draw rivets along the edges
                                                        var currentX = mapLeft + rivetSpacing
                                                        while (currentX < mapRight - rivetSpacing) {
                                                                drawRivet(currentX, mapTop / 2)
                                                                drawRivet(
                                                                        currentX,
                                                                        size.height - 30f
                                                                )
                                                                currentX += rivetSpacing
                                                        }

                                                        // 4. Hazard Striping
                                                        if (hasHazardStriping) {
                                                                val stripeWidth = 15f
                                                                val stripeHeight = 25f
                                                                val stripeSpacing = 30f
                                                                val stripeColor =
                                                                        Color(0xFFFBC02D)
                                                                                .copy(
                                                                                        alpha = 0.8f
                                                                                ) // Industrial
                                                                // Yellow

                                                                // Bottom Right Corner
                                                                val stripeStartX = mapRight - 120f
                                                                val stripeY = mapBottom + 15f

                                                                for (i in 0 until 6) {
                                                                        val path =
                                                                                androidx.compose.ui
                                                                                        .graphics
                                                                                        .Path()
                                                                                        .apply {
                                                                                                val x =
                                                                                                        stripeStartX +
                                                                                                                (i *
                                                                                                                        stripeSpacing)
                                                                                                moveTo(
                                                                                                        x,
                                                                                                        stripeY
                                                                                                )
                                                                                                lineTo(
                                                                                                        x +
                                                                                                                stripeWidth,
                                                                                                        stripeY
                                                                                                )
                                                                                                lineTo(
                                                                                                        x +
                                                                                                                stripeWidth -
                                                                                                                10f,
                                                                                                        stripeY +
                                                                                                                stripeHeight
                                                                                                )
                                                                                                lineTo(
                                                                                                        x -
                                                                                                                10f,
                                                                                                        stripeY +
                                                                                                                stripeHeight
                                                                                                )
                                                                                                close()
                                                                                        }
                                                                        drawPath(path, stripeColor)
                                                                        drawPath(
                                                                                path,
                                                                                Color.Black.copy(
                                                                                        alpha = 0.3f
                                                                                ),
                                                                                style =
                                                                                        Stroke(
                                                                                                width =
                                                                                                        1f
                                                                                        )
                                                                        )
                                                                }
                                                        }

                                                        // 5. Tactical HUD (On top of map area)
                                                        clipRect(
                                                                mapLeft,
                                                                mapTop,
                                                                mapRight,
                                                                mapBottom
                                                        ) {
                                                                // Grid lines
                                                                val gridCount = 8
                                                                val gridColor =
                                                                        artColor.copy(alpha = 0.15f)
                                                                for (i in 1 until gridCount) {
                                                                        val gx =
                                                                                mapLeft +
                                                                                        (mapWidth /
                                                                                                gridCount) *
                                                                                                i
                                                                        val gy =
                                                                                mapTop +
                                                                                        (mapHeight /
                                                                                                gridCount) *
                                                                                                i
                                                                        drawLine(
                                                                                gridColor,
                                                                                Offset(gx, mapTop),
                                                                                Offset(
                                                                                        gx,
                                                                                        mapBottom
                                                                                ),
                                                                                0.5f
                                                                        )
                                                                        drawLine(
                                                                                gridColor,
                                                                                Offset(mapLeft, gy),
                                                                                Offset(
                                                                                        mapRight,
                                                                                        gy
                                                                                ),
                                                                                0.5f
                                                                        )
                                                                }

                                                                // Border highlight
                                                                drawRect(
                                                                        artColor.copy(alpha = 0.2f),
                                                                        Offset(mapLeft, mapTop),
                                                                        androidx.compose.ui.geometry
                                                                                .Size(
                                                                                        mapWidth,
                                                                                        mapHeight
                                                                                ),
                                                                        style = Stroke(width = 1f)
                                                                )

                                                                // Corner Reticles
                                                                val retSize = 20f
                                                                val retThickness = 2f
                                                                // Top-Left
                                                                drawLine(
                                                                        artColor,
                                                                        Offset(mapLeft, mapTop),
                                                                        Offset(
                                                                                mapLeft + retSize,
                                                                                mapTop
                                                                        ),
                                                                        retThickness
                                                                )
                                                                drawLine(
                                                                        artColor,
                                                                        Offset(mapLeft, mapTop),
                                                                        Offset(
                                                                                mapLeft,
                                                                                mapTop + retSize
                                                                        ),
                                                                        retThickness
                                                                )
                                                                // Bottom-Right
                                                                drawLine(
                                                                        artColor,
                                                                        Offset(mapRight, mapBottom),
                                                                        Offset(
                                                                                mapRight - retSize,
                                                                                mapBottom
                                                                        ),
                                                                        retThickness
                                                                )
                                                                drawLine(
                                                                        artColor,
                                                                        Offset(mapRight, mapBottom),
                                                                        Offset(
                                                                                mapRight,
                                                                                mapBottom - retSize
                                                                        ),
                                                                        retThickness
                                                                )
                                                        }
                                                }
                                                "CYBER_GLITCH" -> {
                                                        // Neon Accent lines
                                                        drawRect(
                                                                color = artColor,
                                                                topLeft =
                                                                        Offset(
                                                                                mapLeft - 4f,
                                                                                mapTop - 4f
                                                                        ),
                                                                size =
                                                                        androidx.compose.ui.geometry
                                                                                .Size(
                                                                                        mapWidth +
                                                                                                8f,
                                                                                        mapHeight +
                                                                                                8f
                                                                                ),
                                                                style = Stroke(width = 2f)
                                                        )
                                                        drawRect(
                                                                color = artColor.copy(alpha = 0.2f),
                                                                topLeft =
                                                                        Offset(
                                                                                mapLeft - 1f,
                                                                                mapTop - 1f
                                                                        ),
                                                                size =
                                                                        androidx.compose.ui.geometry
                                                                                .Size(
                                                                                        mapWidth +
                                                                                                2f,
                                                                                        mapHeight +
                                                                                                2f
                                                                                ),
                                                                style = Stroke(width = 1f)
                                                        )
                                                }
                                        }

                                        // 2. Inner Border (Metallic / Glass Reflection)
                                        if (polaroidInnerBorder > 0) {
                                                val borderColor =
                                                        when (polaroidFrameStyle) {
                                                                "CLASSIC_BLACK", "CYBER_GLITCH" ->
                                                                        Color.White.copy(
                                                                                alpha = 0.2f
                                                                        )
                                                                "LIQUID_GLASS" ->
                                                                        Color.White.copy(
                                                                                alpha = 0.5f
                                                                        )
                                                                "HEAVY_MECHANICAL" ->
                                                                        Color(0xFFB0BEC5)
                                                                "ACOUSTIC_WOOD" ->
                                                                        Color(0xFF8D6E63)
                                                                                .copy(alpha = 0.4f)
                                                                else ->
                                                                        Color.Black.copy(
                                                                                alpha = 0.15f
                                                                        )
                                                        }
                                                drawRect(
                                                        borderColor,
                                                        topLeft = Offset(mapLeft, mapTop),
                                                        size =
                                                                androidx.compose.ui.geometry.Size(
                                                                        mapWidth,
                                                                        mapHeight
                                                                ),
                                                        style = Stroke(width = polaroidInnerBorder)
                                                )
                                        }

                                        // 3. Inner Shadow / Ambient Occlusion
                                        val shadowSize = 8f
                                        drawRect(
                                                brush =
                                                        androidx.compose.ui.graphics.Brush
                                                                .verticalGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        Color.Black
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.15f
                                                                                                ),
                                                                                        Color.Transparent
                                                                                ),
                                                                        startY = mapTop,
                                                                        endY = mapTop + shadowSize
                                                                ),
                                                topLeft = Offset(mapLeft, mapTop),
                                                size =
                                                        androidx.compose.ui.geometry.Size(
                                                                mapWidth,
                                                                shadowSize
                                                        )
                                        )

                                        // 4. Global Tint (Color Grading)
                                        val filterColor =
                                                when (polaroidFrameStyle) {
                                                        "ACOUSTIC_WOOD" ->
                                                                Color(0xFFFF9800)
                                                                        .copy(alpha = 0.05f)
                                                        "CYBER_GLITCH" ->
                                                                artColor.copy(alpha = 0.05f)
                                                        "HEAVY_MECHANICAL" ->
                                                                Color(0xFF90A4AE)
                                                                        .copy(alpha = 0.05f)
                                                        else -> Color.Transparent
                                                }
                                        if (filterColor != Color.Transparent) {
                                                drawRect(filterColor, size = size)
                                        }

                                        // 5. Canvas Grain Overlay (Noise on map)
                                        if (canvasGrain > 0f) {
                                                val grainAlpha = 0.15f * canvasGrain
                                                drawRect(
                                                        color =
                                                                Color.Black.copy(
                                                                        alpha = grainAlpha
                                                                ),
                                                        topLeft = Offset(mapLeft, mapTop),
                                                        size =
                                                                androidx.compose.ui.geometry.Size(
                                                                        mapWidth,
                                                                        mapHeight
                                                                ),
                                                        blendMode =
                                                                androidx.compose.ui.graphics
                                                                        .BlendMode.Softlight
                                                )
                                        }
                                }

                                // 4. Polaroid Watermark & Typography
                                Column(
                                        modifier =
                                                Modifier.align(Alignment.BottomCenter)
                                                        .padding(
                                                                bottom = 32.dp,
                                                                start = 24.dp,
                                                                end = 24.dp
                                                        )
                                                        .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        val titleColor =
                                                when (polaroidFrameStyle) {
                                                        "CLASSIC_BLACK", "HEAVY_MECHANICAL" ->
                                                                Color.White.copy(alpha = 0.9f)
                                                        "CYBER_GLITCH" -> artColor
                                                        "ACOUSTIC_WOOD" ->
                                                                Color(0xFF3E2723)
                                                                        .copy(
                                                                                alpha = 0.8f
                                                                        ) // Burnt wood color
                                                        else -> actualTextColor
                                                }
                                        val titleStyle =
                                                MaterialTheme.typography.headlineSmall.copy(
                                                        color = titleColor,
                                                        fontWeight =
                                                                if (isCalligraphy) FontWeight.Normal
                                                                else FontWeight.Bold,
                                                        fontFamily = fontFamily,
                                                        fontStyle = fontStyle
                                                )
                                        Text(
                                                artName.ifBlank { "时光足迹" }.let {
                                                        if (polaroidFrameStyle == "HEAVY_MECHANICAL"
                                                        )
                                                                "[ $it ]"
                                                        else it
                                                },
                                                style = titleStyle,
                                                modifier =
                                                        if (polaroidFrameStyle == "ACOUSTIC_WOOD")
                                                                Modifier.graphicsLayer(
                                                                        alpha = 0.95f
                                                                )
                                                        else if (polaroidFrameStyle ==
                                                                        "HEAVY_MECHANICAL"
                                                        )
                                                                Modifier.border(
                                                                                1.dp,
                                                                                titleColor.copy(
                                                                                        alpha = 0.3f
                                                                                ),
                                                                                CircleShape
                                                                        )
                                                                        .padding(
                                                                                horizontal = 16.dp,
                                                                                vertical = 4.dp
                                                                        )
                                                        else Modifier
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Horizontal Divider line (Holographic style)
                                        Box(
                                                modifier =
                                                        Modifier.width(40.dp)
                                                                .height(1.dp)
                                                                .background(
                                                                        if (polaroidFrameStyle ==
                                                                                        "CLASSIC_BLACK"
                                                                        )
                                                                                Color.White.copy(
                                                                                        alpha = 0.3f
                                                                                )
                                                                        else
                                                                                Color.Black.copy(
                                                                                        alpha = 0.2f
                                                                                )
                                                                )
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Bottom Row: [Date/Distance] --- [Nickname/Coord Stamp]
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Bottom
                                        ) {
                                                // Left Side: Meta Data
                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                if (polaroidFrameStyle ==
                                                                                "HEAVY_MECHANICAL"
                                                                )
                                                                        dateRange.replace(".", "-")
                                                                else dateRange,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall.copy(
                                                                                fontFamily =
                                                                                        if (polaroidFrameStyle ==
                                                                                                        "HEAVY_MECHANICAL"
                                                                                        )
                                                                                                androidx.compose
                                                                                                        .ui
                                                                                                        .text
                                                                                                        .font
                                                                                                        .FontFamily
                                                                                                        .Monospace
                                                                                        else
                                                                                                fontFamily
                                                                        ),
                                                                color = Color.Gray,
                                                        )
                                                        Text(
                                                                "TOTAL DISTANCE: %.2f KM".format(
                                                                        distanceKm
                                                                ),
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall.copy(
                                                                                fontSize = 8.sp,
                                                                                letterSpacing = 1.sp
                                                                        ),
                                                                color =
                                                                        Color.Gray.copy(
                                                                                alpha = 0.7f
                                                                        ),
                                                                fontFamily =
                                                                        androidx.compose.ui.text
                                                                                .font.FontFamily
                                                                                .Monospace
                                                        )
                                                }

                                                // Right Side: Nickname Stamp & Coordinates
                                                Column(
                                                        horizontalAlignment = Alignment.End,
                                                        modifier = Modifier.weight(1f)
                                                ) {
                                                        // Fake Coordinates based on some hash or
                                                        // fixed for preview
                                                        Text(
                                                                "COORD: 31.23°N, 121.47°E",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall.copy(
                                                                                fontSize = 7.sp
                                                                        ),
                                                                color =
                                                                        Color.Gray.copy(
                                                                                alpha = 0.6f
                                                                        ),
                                                                fontFamily =
                                                                        androidx.compose.ui.text
                                                                                .font.FontFamily
                                                                                .Monospace
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        // Red Stamp / Signature
                                                        Box(
                                                                modifier =
                                                                        Modifier.border(
                                                                                        1.dp,
                                                                                        Color(
                                                                                                        0xFFFF453A
                                                                                                )
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.6f
                                                                                                ),
                                                                                        RoundedCornerShape(
                                                                                                2.dp
                                                                                        )
                                                                                )
                                                                                .padding(
                                                                                        horizontal =
                                                                                                4.dp,
                                                                                        vertical =
                                                                                                1.dp
                                                                                )
                                                        ) {
                                                                Text(
                                                                        userNickname.uppercase(),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelSmall
                                                                                        .copy(
                                                                                                fontSize =
                                                                                                        9.sp,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .Bold,
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFFFF453A
                                                                                                        )
                                                                                        )
                                                                )
                                                        }
                                                }
                                        }
                                }
                        }
                }
                ArtLayout.GEEK_STATS -> {
                        Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
                                Column(
                                        modifier =
                                                Modifier.align(Alignment.TopEnd)
                                                        .background(
                                                                when (polaroidFrameStyle) {
                                                                        "ACOUSTIC_WOOD" ->
                                                                                woodBaseColor
                                                                        "HEAVY_MECHANICAL" ->
                                                                                Color(0xFF263238)
                                                                        "CYBER_GLITCH" ->
                                                                                Color.Black
                                                                        "CLASSIC_BLACK" ->
                                                                                Color(0xFF1A1A1A)
                                                                        else ->
                                                                                Color.Black.copy(
                                                                                        alpha = 0.7f
                                                                                )
                                                                },
                                                                RoundedCornerShape(8.dp)
                                                        )
                                                        .run {
                                                                if (polaroidFrameStyle ==
                                                                                "CYBER_GLITCH"
                                                                ) {
                                                                        border(
                                                                                2.dp,
                                                                                artColor,
                                                                                RoundedCornerShape(
                                                                                        8.dp
                                                                                )
                                                                        )
                                                                } else if (polaroidFrameStyle ==
                                                                                "HEAVY_MECHANICAL"
                                                                ) {
                                                                        border(
                                                                                1.dp,
                                                                                Color.White.copy(
                                                                                        alpha = 0.3f
                                                                                ),
                                                                                RoundedCornerShape(
                                                                                        8.dp
                                                                                )
                                                                        )
                                                                } else {
                                                                        this
                                                                }
                                                        }
                                                        .padding(16.dp)
                                ) {
                                        val baseStyle =
                                                MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight =
                                                                if (isCalligraphy) FontWeight.Normal
                                                                else FontWeight.Bold,
                                                        letterSpacing = 2.sp
                                                )
                                        val statsTextColor =
                                                when (polaroidFrameStyle) {
                                                        "ACOUSTIC_WOOD" -> Color(0xFF3E2723)
                                                        "CYBER_GLITCH" -> artColor
                                                        "HEAVY_MECHANICAL" -> Color.White
                                                        "CLASSIC_WHITE" -> Color.Black
                                                        else -> Color.White
                                                }

                                        Box {
                                                if (hasBorder &&
                                                                polaroidFrameStyle != "CYBER_GLITCH"
                                                ) {
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
                                                                        color = statsTextColor
                                                                ),
                                                        fontFamily = fontFamily,
                                                        fontStyle = fontStyle
                                                )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                "DATE: $dateRange • %.2f KM".format(distanceKm),
                                                color = statsTextColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = fontFamily
                                        )
                                        Text(
                                                "MODE: TRACKING",
                                                color = statsTextColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily =
                                                        androidx.compose.ui.text.font.FontFamily
                                                                .Monospace
                                        )
                                }

                                // Decorative elements
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                        val decorColor =
                                                when (polaroidFrameStyle) {
                                                        "CYBER_GLITCH" -> artColor
                                                        "ACOUSTIC_WOOD" -> Color(0xFF3E2723)
                                                        "HEAVY_MECHANICAL" -> Color.White
                                                        else -> artColor
                                                }.copy(alpha = 0.5f)

                                        // Corner brackets
                                        val length = 40.dp.toPx()
                                        val stroke = 2.dp.toPx()

                                        // Top Left
                                        drawLine(
                                                decorColor,
                                                Offset(0f, 0f),
                                                Offset(length, 0f),
                                                stroke
                                        )
                                        drawLine(
                                                decorColor,
                                                Offset(0f, 0f),
                                                Offset(0f, length),
                                                stroke
                                        )

                                        // Bottom Right
                                        drawLine(
                                                decorColor,
                                                Offset(size.width, size.height),
                                                Offset(size.width - length, size.height),
                                                stroke
                                        )
                                        drawLine(
                                                decorColor,
                                                Offset(size.width, size.height),
                                                Offset(size.width, size.height - length),
                                                stroke
                                        )
                                }
                        }
                }
        }
}
