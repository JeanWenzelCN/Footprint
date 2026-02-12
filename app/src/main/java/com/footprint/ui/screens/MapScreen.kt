package com.footprint.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.location.AMapLocation
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.footprint.FootprintViewModel
import com.footprint.data.local.TimeCapsuleEntity
import com.footprint.data.local.TrackPointEntity
import com.footprint.data.model.FootprintEntry
import com.footprint.service.LocationTrackingService
import com.footprint.ui.components.LiquidGlassCard
import com.footprint.ui.components.LiquidModeSelector
import com.footprint.ui.effects.noiseTexture
import com.footprint.ui.theme.LocalHazeState
import com.footprint.utils.AppUtils
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.flow.flowOf

@Composable
fun MapScreen(
        viewModel: FootprintViewModel,
        onNavigateToDetail: (Long) -> Unit,
        entries: List<FootprintEntry> = emptyList(),
        contentPadding: PaddingValues = PaddingValues(0.dp)
) {
        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current
        val isTracking by LocationTrackingService.isTracking.collectAsStateWithLifecycle()
        val trackingPath by LocationTrackingService.trackingPath.collectAsStateWithLifecycle()

        val mapView = remember { MapView(context) }
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        val bottomPadding = contentPadding.calculateBottomPadding()

        var selectedEntry by remember { mutableStateOf<FootprintEntry?>(null) }
        var selectedCapsule by remember { mutableStateOf<TimeCapsuleEntity?>(null) }

        // Persistent Map State
        var lastLat by rememberSaveable { mutableStateOf(39.9042) } // Beijing default
        var lastLng by rememberSaveable { mutableStateOf(116.4074) }
        var lastZoom by rememberSaveable { mutableStateOf(10f) }

        // State to handle auto-centering
        var isPendingCenter by remember { mutableStateOf(true) }

        // 管理 MapView 生命周期
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle, mapView) {
                val lifecycleObserver = LifecycleEventObserver { _, event ->
                        when (event) {
                                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                                Lifecycle.Event.ON_PAUSE -> {
                                        val camera = mapView.map.cameraPosition
                                        lastLat = camera.target.latitude
                                        lastLng = camera.target.longitude
                                        lastZoom = camera.zoom
                                        mapView.onPause()
                                }
                                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                                else -> {}
                        }
                }
                lifecycle.addObserver(lifecycleObserver)

                mapView.onCreate(Bundle())
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        mapView.onResume()
                }

                onDispose {
                        val camera = mapView.map.cameraPosition
                        lastLat = camera.target.latitude
                        lastLng = camera.target.longitude
                        lastZoom = camera.zoom
                        lifecycle.removeObserver(lifecycleObserver)
                        mapView.onDestroy()
                }
        }

        // 监听主题变化，更新地图样式
        LaunchedEffect(isDark) {
                mapView.map.mapType = if (isDark) AMap.MAP_TYPE_NIGHT else AMap.MAP_TYPE_NORMAL
        }

        // 扩展权限列表
        val permissionsToRequest =
                mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        .apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                        }
                        .toTypedArray()

        var hasPermission by remember {
                mutableStateOf(
                        ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                )
        }

        val launcher =
                rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                ) { hasPermission = it[Manifest.permission.ACCESS_FINE_LOCATION] == true }

        val currentLocation by LocationTrackingService.currentLocation.collectAsState()

        // ... (Existing state)
        var mapMode by remember { mutableStateOf(MapMode.STANDARD) }
        var showBuryCapsuleDialog by remember { mutableStateOf(false) }
        var showApiKeyDialog by remember { mutableStateOf(false) }
        val amapKey by viewModel.amapKey.collectAsStateWithLifecycle()

        // Time Capsule State
        val unlockedCapsules by
                viewModel.unlockedCapsules.collectAsStateWithLifecycle(initialValue = emptyList())
        val lockedCapsules by
                viewModel.lockedCapsules.collectAsStateWithLifecycle(initialValue = emptyList())

        // Radar Animation for Fog Mode
        val infiniteTransition = rememberInfiniteTransition(label = "Radar")
        val pulseValue by
                infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                ),
                        label = "PulseValue"
                )

        // Heatmap Data
        val heatmapPoints by
                remember(mapMode) {
                                if (mapMode == MapMode.HEATMAP || mapMode == MapMode.FOG)
                                        viewModel.getHeatmapPoints()
                                else flowOf(emptyList())
                        }
                        .collectAsStateWithLifecycle(initialValue = emptyList())

        // ... (Existing MapView lifecycle)

        LaunchedEffect(currentLocation) {
                currentLocation?.let { loc ->
                        // Update "Fog" or "Check Unlock"
                        val androidLoc =
                                android.location.Location("").apply {
                                        latitude = loc.latitude
                                        longitude = loc.longitude
                                }
                        viewModel.checkTimeCapsuleUnlock(androidLoc)

                        if (mapMode == MapMode.FOG) {
                                // Reveal fog logic here (Placeholder)
                        }

                        if (loc.latitude > 1.0 && isPendingCenter) {
                                mapView.map.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                                LatLng(loc.latitude, loc.longitude),
                                                17f
                                        )
                                )
                                isPendingCenter = false
                        }
                }
        }

        val hazeState = remember { HazeState() }
        var amapInstance by remember { mutableStateOf<AMap?>(null) }

        CompositionLocalProvider(LocalHazeState provides hazeState) {
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background)
                ) {
                        // 1. Background Source Layer (What gets blurred)
                        Box(modifier = Modifier.fillMaxSize().haze(hazeState)) {
                                if (hasPermission) {
                                        AndroidView(
                                                factory = { _ ->
                                                        mapView.apply {
                                                                amapInstance = map
                                                                map.apply {
                                                                        uiSettings
                                                                                .isMyLocationButtonEnabled =
                                                                                false
                                                                        isMyLocationEnabled = true

                                                                        // ... (Existing setup)

                                                                        setOnMarkerClickListener {
                                                                                marker ->
                                                                                val entryId =
                                                                                        marker.snippet
                                                                                                ?.toLongOrNull()
                                                                                if (entryId != null
                                                                                ) {
                                                                                        // Check if
                                                                                        // it's a
                                                                                        // capsule
                                                                                        // or entry
                                                                                        if (marker.title ==
                                                                                                        "Time Capsule"
                                                                                        ) {
                                                                                                // Show
                                                                                                // capsule
                                                                                                // dialog
                                                                                        } else {
                                                                                                onNavigateToDetail(
                                                                                                        entryId
                                                                                                )
                                                                                        }
                                                                                }
                                                                                true
                                                                        }
                                                                        // ...
                                                                }
                                                        }
                                                },
                                                modifier = Modifier.fillMaxSize()
                                        ) { mv ->
                                                mv.map.clear()

                                                // Common: Draw Footprints (Standard Mode)
                                                if (mapMode == MapMode.STANDARD) {
                                                        if (trackingPath.isNotEmpty()) {
                                                                val points =
                                                                        trackingPath.map {
                                                                                LatLng(
                                                                                        it.latitude,
                                                                                        it.longitude
                                                                                )
                                                                        }
                                                                mv.map.addPolyline(
                                                                        PolylineOptions()
                                                                                .addAll(points)
                                                                                .width(18f)
                                                                                .color(
                                                                                        android.graphics
                                                                                                .Color
                                                                                                .parseColor(
                                                                                                        "#00FF9F"
                                                                                                )
                                                                                )
                                                                )
                                                        }
                                                        entries.forEach { entry ->
                                                                if (entry.latitude != null &&
                                                                                entry.longitude !=
                                                                                        null
                                                                ) {
                                                                        mv.map.addMarker(
                                                                                MarkerOptions()
                                                                                        .position(
                                                                                                LatLng(
                                                                                                        entry.latitude,
                                                                                                        entry.longitude
                                                                                                )
                                                                                        )
                                                                                        .title(
                                                                                                entry.title
                                                                                        )
                                                                                        .snippet(
                                                                                                entry.id
                                                                                                        .toString()
                                                                                        )
                                                                                        .icon(
                                                                                                BitmapDescriptorFactory
                                                                                                        .defaultMarker(
                                                                                                                BitmapDescriptorFactory
                                                                                                                        .HUE_AZURE
                                                                                                        )
                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                }

                                                // Capsule Mode
                                                if (mapMode == MapMode.CAPSULE) {
                                                        unlockedCapsules.forEach { capsule ->
                                                                mv.map.addMarker(
                                                                        MarkerOptions()
                                                                                .position(
                                                                                        LatLng(
                                                                                                capsule.latitude,
                                                                                                capsule.longitude
                                                                                        )
                                                                                )
                                                                                .title(
                                                                                        "Time Capsule"
                                                                                )
                                                                                .snippet(
                                                                                        capsule.id
                                                                                                .toString()
                                                                                )
                                                                                .icon(
                                                                                        BitmapDescriptorFactory
                                                                                                .defaultMarker(
                                                                                                        BitmapDescriptorFactory
                                                                                                                .HUE_YELLOW
                                                                                                )
                                                                                )
                                                                )
                                                        }
                                                        lockedCapsules.forEach { capsule ->
                                                                mv.map.addMarker(
                                                                        MarkerOptions()
                                                                                .position(
                                                                                        LatLng(
                                                                                                capsule.latitude,
                                                                                                capsule.longitude
                                                                                        )
                                                                                )
                                                                                .title(
                                                                                        "Locked Capsule"
                                                                                )
                                                                                .snippet(
                                                                                        capsule.id
                                                                                                .toString()
                                                                                )
                                                                                .icon(
                                                                                        BitmapDescriptorFactory
                                                                                                .defaultMarker(
                                                                                                        BitmapDescriptorFactory
                                                                                                                .HUE_RED
                                                                                                )
                                                                                )
                                                                )
                                                        }
                                                }

                                                // 4. Cloud & Mist Overlay (Compose Side)
                                                // We use a dummy overlay here to ensure the AMap
                                                // polygon
                                                // clears
                                                // elements,
                                                // but we will also draw the "Cloud" in Compose.
                                                if (mapMode == MapMode.FOG) {
                                                        // Still use AMap polygon to hide everything
                                                        // underneath
                                                        // efficiently
                                                        val worldCoords =
                                                                listOf(
                                                                        LatLng(85.0, -179.9),
                                                                        LatLng(85.0, 179.9),
                                                                        LatLng(-85.0, 179.9),
                                                                        LatLng(-85.0, -179.9)
                                                                )
                                                        val fogArea =
                                                                com.amap.api.maps.model
                                                                        .PolygonOptions()
                                                                        .addAll(worldCoords)
                                                                        .fillColor(
                                                                                android.graphics
                                                                                        .Color
                                                                                        .parseColor(
                                                                                                "#4D4F4F4F"
                                                                                        )
                                                                        ) // Much more transparent
                                                                        // base
                                                                        .strokeWidth(0f)
                                                                        .zIndex(2f)

                                                        val sampledHistorical =
                                                                heatmapPoints
                                                                        .filterIndexed { index, _ ->
                                                                                index % 20 == 0
                                                                        }
                                                                        .map {
                                                                                LatLng(
                                                                                        it.latitude,
                                                                                        it.longitude
                                                                                )
                                                                        }
                                                        val sampledLive =
                                                                trackingPath
                                                                        .filterIndexed { index, _ ->
                                                                                index % 10 == 0
                                                                        }
                                                                        .map {
                                                                                LatLng(
                                                                                        it.latitude,
                                                                                        it.longitude
                                                                                )
                                                                        }
                                                        (sampledHistorical + sampledLive).forEach {
                                                                latLng ->
                                                                fogArea.addHoles(
                                                                        com.amap.api.maps.model
                                                                                .CircleHoleOptions()
                                                                                .center(latLng)
                                                                                .radius(50.0)
                                                                )
                                                        }
                                                        mv.map.addPolygon(fogArea)
                                                }

                                                // Heatmap Mode
                                                if (mapMode == MapMode.HEATMAP) {
                                                        // Optimized visualization: Larger radius,
                                                        // lower
                                                        // opacity for
                                                        // blending
                                                        heatmapPoints.forEach { pt ->
                                                                mv.map.addCircle(
                                                                        com.amap.api.maps.model
                                                                                .CircleOptions()
                                                                                .center(
                                                                                        LatLng(
                                                                                                pt.latitude,
                                                                                                pt.longitude
                                                                                        )
                                                                                )
                                                                                .radius(50.0)
                                                                                .fillColor(
                                                                                        android.graphics
                                                                                                .Color
                                                                                                .parseColor(
                                                                                                        "#1A33FF00"
                                                                                                )
                                                                                )
                                                                                .strokeWidth(0f)
                                                                )
                                                        }
                                                }
                                        }
                                } else {
                                        PermissionDenyOverlay {
                                                launcher.launch(permissionsToRequest)
                                        }
                                }
                        }

                        // 2. Overlay Layers (hazeChild components)
                        // Note: These are OUTSIDE the .haze() box above to avoid being blurred
                        // themselves.

                        // High-Fidelity Cloud & Mist Layer (Android 12+)
                        if (mapMode == MapMode.FOG) {
                                CloudMistFog(
                                        amap = amapInstance,
                                        heatmapPoints = heatmapPoints,
                                        trackingPath = trackingPath,
                                        pulseValue = pulseValue,
                                        hazeState = hazeState
                                )
                        }

                        // Mode Selector (Top Center) with Liquid Glass & Water Drop effect
                        LiquidModeSelector(
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                                hazeState = hazeState,
                                selectedMode = mapMode,
                                onModeSelected = { mode ->
                                        if (mapMode != mode) {
                                                haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                )
                                                mapMode = mode
                                        }
                                }
                        )

                        // Settings Button (Top End)
                        LiquidGlassCard(
                                modifier =
                                        Modifier.align(Alignment.TopEnd)
                                                .padding(top = 48.dp, end = 16.dp)
                                                .size(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                blurRadius = 12.dp
                        ) {
                                IconButton(
                                        onClick = { showApiKeyDialog = true },
                                        modifier = Modifier.fillMaxSize()
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "Settings",
                                                tint = MaterialTheme.colorScheme.onSurface
                                        )
                                }
                        }

                        // FAB Group (Bottom End)
                        Column(
                                modifier =
                                        Modifier.align(Alignment.BottomEnd)
                                                .padding(
                                                        bottom = bottomPadding + 24.dp,
                                                        end = 24.dp
                                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                                // Location Centering FAB
                                FloatingActionButton(
                                        onClick = {
                                                if (currentLocation != null) {
                                                        mapView.map.animateCamera(
                                                                CameraUpdateFactory.newLatLngZoom(
                                                                        LatLng(
                                                                                currentLocation!!
                                                                                        .latitude,
                                                                                currentLocation!!
                                                                                        .longitude
                                                                        ),
                                                                        17f
                                                                )
                                                        )
                                                } else {
                                                        // Fallback to internal AMap location if
                                                        // service isn't tracking
                                                        amapInstance?.myLocation?.let { loc ->
                                                                mapView.map.animateCamera(
                                                                        CameraUpdateFactory
                                                                                .newLatLngZoom(
                                                                                        LatLng(
                                                                                                loc.latitude,
                                                                                                loc.longitude
                                                                                        ),
                                                                                        17f
                                                                                )
                                                                )
                                                        }
                                                }
                                        },
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ) { Icon(Icons.Filled.MyLocation, "Center Location") }

                                // Recording FAB
                                FloatingActionButton(
                                        onClick = {
                                                haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                )
                                                if (isTracking) {
                                                        LocationTrackingService.stopTracking(
                                                                context
                                                        )
                                                } else {
                                                        LocationTrackingService.startTracking(
                                                                context
                                                        )
                                                }
                                        },
                                        containerColor =
                                                if (isTracking) Color.Red
                                                else MaterialTheme.colorScheme.tertiary,
                                        contentColor = Color.White
                                ) {
                                        Icon(
                                                if (isTracking) Icons.Filled.Stop
                                                else Icons.Filled.PlayArrow,
                                                if (isTracking) "Stop Recording"
                                                else "Start Recording"
                                        )
                                }

                                // Bury Capsule Button (Only in Capsule Mode)
                                AnimatedVisibility(
                                        visible = mapMode == MapMode.CAPSULE,
                                        enter = scaleIn() + fadeIn(),
                                        exit = scaleOut() + fadeOut()
                                ) {
                                        FloatingActionButton(
                                                onClick = { showBuryCapsuleDialog = true },
                                                containerColor =
                                                        MaterialTheme.colorScheme.secondary,
                                                contentColor = MaterialTheme.colorScheme.onSecondary
                                        ) { Icon(Icons.Filled.Timer, "Bury Capsule") }
                                }
                        }
                }
        }

        // Dialogs
        if (showApiKeyDialog) {
                ApiKeyDialog(
                        initialKey = amapKey,
                        onDismiss = { showApiKeyDialog = false },
                        onSave = { key ->
                                viewModel.saveAmapKey(key)
                                showApiKeyDialog = false
                                android.widget.Toast.makeText(
                                                context,
                                                "API Key 已保存，请重启应用生效",
                                                android.widget.Toast.LENGTH_LONG
                                        )
                                        .show()
                        }
                )
        }
        if (showBuryCapsuleDialog) {
                BuryCapsuleDialog(
                        currentLocation = currentLocation,
                        onDismiss = { showBuryCapsuleDialog = false },
                        onConfirm = { msg, uri, duration ->
                                if (currentLocation != null) {
                                        viewModel.buryTimeCapsule(
                                                message = msg,
                                                contentUri = uri,
                                                latitude = currentLocation!!.latitude,
                                                longitude = currentLocation!!.longitude,
                                                unlockDurationMs = duration
                                        )
                                        showBuryCapsuleDialog = false
                                }
                        }
                )
        }

        if (selectedCapsule != null) {
                CapsuleContentDialog(
                        capsule = selectedCapsule!!,
                        onDismiss = { selectedCapsule = null }
                )
        }

        if (selectedEntry != null) {
                // ... Existing Entry Dialog or Logic?
                // mapScreen usually doesn't show full entry detail dialog, but maybe we should?
                // For now, let's assume Entry viewing is handled elsewhere or via a simple dialog
                // if needed.
                // But looking at previous code, selectedEntry was state but not used?
                // usage: var selectedEntry by remember { mutableStateOf<FootprintEntry?>(null) }
                // I will implement a simple entry dialog if it wasn't there, or leave it.
                // The user didn't ask for Entry Viewing fix, just Capsule.
        }

        // ... (Existing dialogs)
}

enum class MapMode(val label: String) {
        STANDARD("标准"),
        FOG("迷雾"),
        HEATMAP("热力"),
        CAPSULE("胶囊")
}

@Composable
fun BuryCapsuleDialog(
        currentLocation: AMapLocation?,
        onDismiss: () -> Unit,
        onConfirm: (String, String?, Long) -> Unit
) {
        var message by remember { mutableStateOf("") }
        var selectedDuration by remember { mutableStateOf(3600_000L) } // 1 Hour

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("埋藏时光胶囊") },
                text = {
                        Column {
                                if (currentLocation == null) {
                                        Text("需要获取当前位置才能埋藏胶囊。")
                                } else {
                                        OutlinedTextField(
                                                value = message,
                                                onValueChange = { message = it },
                                                label = { Text("留言") },
                                                modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("解锁时间：", style = MaterialTheme.typography.labelMedium)
                                        // Simple duration selector
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                                "1分钟" to 60_000L,
                                                                "1小时" to 3600_000L,
                                                                "1年" to 31536000000L
                                                        )
                                                        .forEach { (label, duration) ->
                                                                FilterChip(
                                                                        selected =
                                                                                selectedDuration ==
                                                                                        duration,
                                                                        onClick = {
                                                                                selectedDuration =
                                                                                        duration
                                                                        },
                                                                        label = { Text(label) }
                                                                )
                                                        }
                                        }
                                }
                        }
                },
                confirmButton = {
                        Button(
                                onClick = { onConfirm(message, null, selectedDuration) },
                                enabled = currentLocation != null && message.isNotBlank()
                        ) { Text("埋藏") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
        )
}

@Composable
fun LocationErrorDialog(error: String?, onDismiss: () -> Unit, onGoToSettings: () -> Unit) {
        if (error == null) return

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("定位失败") },
                text = { Text("$error\n\n请检查您的网络连接，并确保已在“设置”中正确配置了高德API Key。") },
                confirmButton = { TextButton(onClick = onGoToSettings) { Text("前往设置") } },
                dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
        )
}

@Composable
fun ApiKeyDialog(initialKey: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
        var apiKey by remember { mutableStateOf(initialKey) }
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current
        val sha1 = remember { AppUtils.getAppSignature(context) }

        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
                LiquidGlassCard(
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                        Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                Text(
                                        "设置 API Key",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                        text = "Package: ${context.packageName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color =
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.5f
                                                )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                        text = "SHA1 (点击复制):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color =
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.7f
                                                )
                                )
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .background(
                                                                MaterialTheme.colorScheme.onSurface
                                                                        .copy(alpha = 0.05f),
                                                                RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                                clipboardManager.setText(
                                                                        AnnotatedString(sha1)
                                                                )
                                                                android.widget.Toast.makeText(
                                                                                context,
                                                                                "SHA1 已复制",
                                                                                android.widget.Toast
                                                                                        .LENGTH_SHORT
                                                                        )
                                                                        .show()
                                                        }
                                                        .padding(8.dp)
                                ) {
                                        Text(
                                                text = sha1,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.weight(1f),
                                                fontFamily =
                                                        androidx.compose.ui.text.font.FontFamily
                                                                .Monospace,
                                                color =
                                                        MaterialTheme.colorScheme.onSurface.copy(
                                                                alpha = 0.8f
                                                        )
                                        )
                                        Icon(
                                                Icons.Default.ContentCopy,
                                                "复制",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                        )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                        "请输入您的高德地图 API Key：",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color =
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.7f
                                                )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                        value = apiKey,
                                        onValueChange = { apiKey = it },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors =
                                                OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor =
                                                                MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor =
                                                                MaterialTheme.colorScheme.outline
                                                                        .copy(alpha = 0.5f),
                                                        focusedTextColor =
                                                                MaterialTheme.colorScheme.onSurface,
                                                        unfocusedTextColor =
                                                                MaterialTheme.colorScheme.onSurface
                                                )
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                ) {
                                        TextButton(onClick = onDismiss) {
                                                Text(
                                                        "取消",
                                                        color = MaterialTheme.colorScheme.outline
                                                )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                                onClick = { onSave(apiKey) },
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                        ) { Text("保存") }
                                }
                        }
                }
        }
}

@Composable
fun PermissionDenyOverlay(onRetry: () -> Unit) {
        Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
        ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                                Icons.Default.Security,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                        )
                        Text(
                                "需要定位与通知权限",
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 16.dp)
                        )
                        Button(onClick = onRetry, modifier = Modifier.padding(top = 24.dp)) {
                                Text("立即授权")
                        }
                }
        }
}

@Composable
fun CloudMistFog(
        amap: AMap?,
        heatmapPoints: List<TrackPointEntity>,
        trackingPath: List<AMapLocation>,
        pulseValue: Float,
        hazeState: HazeState
) {
        val a = amap ?: return
        val zoom = a.cameraPosition.zoom

        // Sample and map to LatLng for unified processing
        val sampledPoints =
                (heatmapPoints.filterIndexed { i, _ -> i % 10 == 0 }.map {
                        LatLng(it.latitude, it.longitude)
                } +
                        trackingPath.filterIndexed { i, _ -> i % 5 == 0 }.map {
                                LatLng(it.latitude, it.longitude)
                        })

        // Calculate dynamic hole size in pixels
        val targetRadiusMeters = 50.0

        // Separate points for clearer path construction
        val heatmapLines = heatmapPoints.filterIndexed { i, _ -> i % 10 == 0 }
        val trackingLines = trackingPath.filterIndexed { i, _ -> i % 3 == 0 }

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .hazeChild(
                                        state = hazeState,
                                        shape = RectangleShape,
                                        style = HazeStyle(blurRadius = 40.dp)
                                )
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .drawWithContent {
                                        // 1. Draw Base "Cloud/Smoke" Atmospheric Texture
                                        // Layer 1: Deep Base
                                        drawRect(
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        Color(0xFF1B2631)
                                                                                .copy(
                                                                                        alpha =
                                                                                                0.96f
                                                                                ),
                                                                        Color(0xFF2E4053)
                                                                                .copy(alpha = 0.98f)
                                                                )
                                                )
                                        )

                                        // Layer 2: Cloud Splotches (Noise-like gradients)
                                        drawRect(
                                                Brush.radialGradient(
                                                        colors =
                                                                listOf(
                                                                        Color.White.copy(
                                                                                alpha = 0.05f
                                                                        ),
                                                                        Color.Transparent
                                                                ),
                                                        center =
                                                                Offset(
                                                                        size.width * 0.3f,
                                                                        size.height * 0.2f
                                                                ),
                                                        radius = size.width * 0.8f
                                                )
                                        )
                                        drawRect(
                                                Brush.radialGradient(
                                                        colors =
                                                                listOf(
                                                                        Color.White.copy(
                                                                                alpha = 0.03f
                                                                        ),
                                                                        Color.Transparent
                                                                ),
                                                        center =
                                                                Offset(
                                                                        size.width * 0.7f,
                                                                        size.height * 0.8f
                                                                ),
                                                        radius = size.width * 0.6f
                                                )
                                        )

                                        // 2. Breathing mist effect
                                        drawRect(Color.White.copy(alpha = 0.05f * pulseValue))

                                        // 3. Punch Holes (Explored Areas)
                                        val projection = a.projection

                                        // A. CONNECTED PATH for tracking trace
                                        if (trackingLines.size > 1) {
                                                val path = Path()
                                                val firstPoint = trackingLines[0]
                                                val firstPos =
                                                        projection.toScreenLocation(
                                                                LatLng(
                                                                        firstPoint.latitude,
                                                                        firstPoint.longitude
                                                                )
                                                        )
                                                path.moveTo(
                                                        firstPos.x.toFloat(),
                                                        firstPos.y.toFloat()
                                                )

                                                trackingLines.drop(1).forEach { loc ->
                                                        val pos =
                                                                projection.toScreenLocation(
                                                                        LatLng(
                                                                                loc.latitude,
                                                                                loc.longitude
                                                                        )
                                                                )
                                                        path.lineTo(
                                                                pos.x.toFloat(),
                                                                pos.y.toFloat()
                                                        )
                                                }

                                                // Calculate pixels for ~60m width
                                                val centerLat =
                                                        trackingLines[trackingLines.size / 2]
                                                                .latitude
                                                val metersPerPixel =
                                                        (156543.03392 *
                                                                        Math.cos(
                                                                                centerLat *
                                                                                        Math.PI /
                                                                                        180.0
                                                                        ) /
                                                                        Math.pow(
                                                                                2.0,
                                                                                zoom.toDouble()
                                                                        ))
                                                                .toFloat()
                                                val pixelRadius =
                                                        (targetRadiusMeters / metersPerPixel)
                                                                .toFloat()

                                                drawPath(
                                                        path = path,
                                                        color = Color.Black,
                                                        style =
                                                                Stroke(
                                                                        width =
                                                                                pixelRadius *
                                                                                        2.2f, // Slightly wider corridor
                                                                        cap = StrokeCap.Round,
                                                                        join = StrokeJoin.Round
                                                                ),
                                                        blendMode = BlendMode.DstOut
                                                )

                                                // Add a softer outer glow/blur to the path
                                                drawPath(
                                                        path = path,
                                                        color = Color.Black.copy(alpha = 0.4f),
                                                        style =
                                                                Stroke(
                                                                        width = pixelRadius * 3.5f,
                                                                        cap = StrokeCap.Round,
                                                                        join = StrokeJoin.Round
                                                                ),
                                                        blendMode = BlendMode.DstOut
                                                )
                                        }

                                        // B. BLOBS for individual heatmap points (also soft)
                                        heatmapLines.forEach { pt ->
                                                val latLng = LatLng(pt.latitude, pt.longitude)
                                                val pos = projection.toScreenLocation(latLng)
                                                val metersPerPixel =
                                                        (156543.03392 *
                                                                        Math.cos(
                                                                                latLng.latitude *
                                                                                        Math.PI /
                                                                                        180.0
                                                                        ) /
                                                                        Math.pow(
                                                                                2.0,
                                                                                zoom.toDouble()
                                                                        ))
                                                                .toFloat()
                                                val pixelRadius =
                                                        (targetRadiusMeters / metersPerPixel)
                                                                .toFloat()

                                                // Soft inner core
                                                drawCircle(
                                                        color = Color.Black,
                                                        radius = pixelRadius,
                                                        center =
                                                                Offset(
                                                                        pos.x.toFloat(),
                                                                        pos.y.toFloat()
                                                                ),
                                                        blendMode = BlendMode.DstOut
                                                )

                                                // Soft outer glow
                                                drawCircle(
                                                        color = Color.Black.copy(alpha = 0.3f),
                                                        radius = pixelRadius * 1.8f,
                                                        center =
                                                                Offset(
                                                                        pos.x.toFloat(),
                                                                        pos.y.toFloat()
                                                                ),
                                                        blendMode = BlendMode.DstOut
                                                )
                                        }
                                }
                                .noiseTexture(0.2f)
        )
}

@Composable
fun CapsuleContentDialog(capsule: TimeCapsuleEntity, onDismiss: () -> Unit) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("开启的时光胶囊") },
                text = {
                        Column {
                                Text(
                                        "埋藏时间: ${AppUtils.formatDate(capsule.creationTime)}",
                                        style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(capsule.message, style = MaterialTheme.typography.bodyLarge)
                                if (capsule.contentUri != null) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        // Image Preview placeholder
                                        Text(
                                                "包含附件: [图片]",
                                                color = MaterialTheme.colorScheme.primary
                                        )
                                }
                        }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
        )
}
