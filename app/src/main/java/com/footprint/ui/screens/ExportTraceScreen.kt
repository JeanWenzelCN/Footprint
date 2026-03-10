package com.footprint.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.border
import androidx.compose.ui.draw.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.vector.ImageVector
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.footprint.FootprintViewModel
import com.footprint.utils.PathInterpolator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Collections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportTraceScreen(viewModel: FootprintViewModel, initialYear: Int? = null, onBack: () -> Unit) {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Selection State
    var startDate by remember {
        mutableStateOf(
            if (initialYear != null) LocalDate.of(initialYear, 1, 1) else LocalDate.now().minusDays(7)
        )
    }
    var endDate by remember {
        mutableStateOf(
            if (initialYear != null) LocalDate.of(initialYear, 12, 31) else LocalDate.now()
        )
    }

    // Playback State
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableFloatStateOf(0f) }
    var currentPlaybackPoint by remember { mutableStateOf<LatLng?>(null) }
    var playbackSpeedKmH by remember { mutableFloatStateOf(120f) }

    // Control Panel State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showControls by remember { mutableStateOf(true) }
    var mapType by remember { mutableStateOf(AMap.MAP_TYPE_NORMAL) }

    // Data points & Smooth Path
    var rawPoints by remember {
        mutableStateOf<List<com.footprint.data.local.TrackPointEntity>>(emptyList())
    }
    val smoothPoints = remember(rawPoints) {
        if (rawPoints.isEmpty()) return@remember emptyList<LatLng>()
        val latLngs = rawPoints.map { LatLng(it.latitude, it.longitude) }
        PathInterpolator.interpolate(latLngs, multiplier = 10)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entriesInRange = remember(uiState.entries, startDate, endDate) {
        uiState.entries.filter {
            !it.happenedOn.isBefore(startDate) && !it.happenedOn.isAfter(endDate)
        }
    }

    // Map LifeCycle
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        try {
            mapView.onCreate(null)
        } catch (e: Exception) {}

        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    isPlaying = false
                }
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Sync Map Type and Theme
    LaunchedEffect(isDark, mapType) {
        mapView.map.apply {
            this.mapType = if (mapType == AMap.MAP_TYPE_NORMAL && isDark) {
                AMap.MAP_TYPE_NIGHT
            } else {
                mapType
            }
            showBuildings(true)
            showIndoorMap(true)
            isTrafficEnabled = false
            uiSettings.apply {
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = true
                isZoomControlsEnabled = false
                isCompassEnabled = false
            }
        }
    }

    // Fetch Track Data
    val startTimestamp = remember(startDate) {
        startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    val endTimestamp = remember(endDate) {
        endDate.atTime(23, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    val tracePointsFlow = remember(startTimestamp, endTimestamp) {
        viewModel.getTrackPoints(startTimestamp, endTimestamp)
    }
    val tracePoints by tracePointsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(tracePoints) {
        rawPoints = tracePoints.filter { it.latitude != 0.0 && it.longitude != 0.0 }
        if (rawPoints.isNotEmpty()) {
            val builder = LatLngBounds.builder()
            rawPoints.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
            entriesInRange.forEach { e ->
                if (e.latitude != null && e.longitude != null) {
                    builder.include(LatLng(e.latitude, e.longitude))
                }
            }
            try {
                mapView.map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 200), 800, null)
            } catch (e: Exception) {}
        }
    }

    val cumulativeDistances = remember(smoothPoints) {
        if (smoothPoints.isEmpty()) return@remember emptyList<Double>()
        val dists = mutableListOf<Double>(0.0)
        var currentTotal = 0.0
        val results = FloatArray(1)
        for (i in 0 until smoothPoints.size - 1) {
            try {
                android.location.Location.distanceBetween(
                    smoothPoints[i].latitude, smoothPoints[i].longitude,
                    smoothPoints[i+1].latitude, smoothPoints[i+1].longitude,
                    results
                )
                currentTotal += results[0] / 1000.0
            } catch (e: Exception) {}
            dists.add(currentTotal)
        }
        dists
    }

    // Overlay Management
    val pathPolyline = remember { mutableStateOf<Polyline?>(null) }
    val roamingMarker = remember { mutableStateOf<Marker?>(null) }
    val entryMarkers = remember { mutableStateListOf<Marker>() }
    val breadcrumbDots = remember { mutableStateListOf<Marker>() }

    // Initialize Map Overlays when data points change
    LaunchedEffect(smoothPoints, entriesInRange) {
        val map = try { mapView.map } catch (e: Exception) { return@LaunchedEffect }
        map.clear()
        entryMarkers.clear()
        breadcrumbDots.clear()

        // Persistent Path
        pathPolyline.value = map.addPolyline(
            PolylineOptions()
                .width(16f)
                .useGradient(true)
                .color(android.graphics.Color.parseColor("#00E5FF"))
                .lineJoinType(PolylineOptions.LineJoinType.LineJoinRound)
        )

        // Static Entry Markers - Start invisible
        entriesInRange.forEach { entry ->
            if (entry.latitude != null && entry.longitude != null) {
                val m = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(entry.latitude, entry.longitude))
                        .title(entry.title)
                        .snippet(entry.location)
                        .visible(false) // Start hidden
                        .alpha(0f)      // Start transparent for fade-in
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
                entryMarkers.add(m)
            }
        }

        // Roaming indicator
        roamingMarker.value = map.addMarker(
            MarkerOptions()
                .position(LatLng(0.0, 0.0))
                .anchor(0.5f, 0.5f)
                .visible(false)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        )
    }

    // Dynamic Updates (Breadcrumbs and Marker reveal)
    // Synchronized Path and Roamer position are now handled in the Master Loop for 1:1 alignment.
    LaunchedEffect(playbackProgress) {
        val map = try { mapView.map } catch (e: Exception) { return@LaunchedEffect }

        if (smoothPoints.isNotEmpty()) {
            // 1. Reveal nearby entry markers
            entryMarkers.forEach { em ->
                if (!em.isVisible && currentPlaybackPoint != null) {
                    val distResults = FloatArray(1)
                    android.location.Location.distanceBetween(
                        em.position.latitude, em.position.longitude,
                        currentPlaybackPoint!!.latitude, currentPlaybackPoint!!.longitude,
                        distResults
                    )
                    if (distResults[0] < 200.0) { 
                        em.isVisible = true
                        em.alpha = 1.0f
                    }
                }
            }

            // 2. Leave Footprints (Breadcrumbs)
            val lastDotPos = breadcrumbDots.lastOrNull()?.position
            val shouldAddDot = lastDotPos == null || run {
                val distResults = FloatArray(1)
                android.location.Location.distanceBetween(
                    lastDotPos.latitude, lastDotPos.longitude,
                    currentPlaybackPoint?.latitude ?: 0.0, currentPlaybackPoint?.longitude ?: 0.0,
                    distResults
                )
                distResults[0] > 45.0 
            }

            if (shouldAddDot && currentPlaybackPoint != null) {
                val dot = map.addMarker(
                    MarkerOptions()
                        .position(currentPlaybackPoint!!)
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromBitmap(
                            android.graphics.Bitmap.createBitmap(14, 14, android.graphics.Bitmap.Config.ARGB_8888).apply {
                                android.graphics.Canvas(this).drawCircle(7f, 7f, 5f, android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#00E5FF")
                                    alpha = 160
                                })
                            }
                        ))
                )
                breadcrumbDots.add(dot)
                if (breadcrumbDots.size > 1000) {
                    breadcrumbDots.removeAt(0).remove()
                }
            }
        }
    }

    // Ultra-Smooth Master Playback Loop (Choreographer Powered)
    LaunchedEffect(isPlaying, smoothPoints, cumulativeDistances, playbackSpeedKmH) {
        if (!isPlaying || smoothPoints.size < 2 || cumulativeDistances.isEmpty()) return@LaunchedEffect
        
        val map = try { mapView.map } catch (e: Exception) { return@LaunchedEffect }
        val poly = pathPolyline.value ?: return@LaunchedEffect
        val marker = roamingMarker.value ?: return@LaunchedEffect
        val totalDist = cumulativeDistances.last()
        if (totalDist <= 0) { isPlaying = false; return@LaunchedEffect }

        var lastFrameTime = System.currentTimeMillis()
        
        while (isPlaying && playbackProgress < 1f) {
            withFrameMillis { _ ->
                val currentTime = System.currentTimeMillis()
                val deltaMs = (currentTime - lastFrameTime).coerceAtMost(50)
                lastFrameTime = currentTime
                
                val dtHours = deltaMs / 1000.0 / 3600.0
                val distanceStep = playbackSpeedKmH * dtHours
                
                val nextDist = (playbackProgress * totalDist) + distanceStep
                playbackProgress = (nextDist / totalDist).toFloat().coerceIn(0f, 1f)
                
                // Binary search for segment
                val searchRes = Collections.binarySearch(cumulativeDistances, nextDist.toDouble())
                val index = when {
                    searchRes >= 0 -> searchRes
                    else -> (-searchRes - 2).coerceIn(0, smoothPoints.size - 2)
                }

                if (index < smoothPoints.size - 1) {
                    val p1 = smoothPoints[index]
                    val p2 = smoothPoints[index+1]
                    val d1 = cumulativeDistances[index]
                    val d2 = cumulativeDistances[index+1]
                    
                    val segmentProgress = if (d2 > d1) (nextDist - d1) / (d2 - d1) else 0.0
                    val targetLatLng = LatLng(
                        p1.latitude + (p2.latitude - p1.latitude) * segmentProgress,
                        p1.longitude + (p2.longitude - p1.longitude) * segmentProgress
                    )
                    
                    // Direct Map Component Updates
                    marker.position = targetLatLng
                    marker.isVisible = true
                    currentPlaybackPoint = targetLatLng

                    // Fixed: Path synchronization - Add current interpolated position to the points list
                    // This creates a list up to the integer index and appends the "head" point
                    val pathPoints = mutableListOf<LatLng>()
                    for (i in 0..index) {
                        pathPoints.add(smoothPoints[i])
                    }
                    pathPoints.add(targetLatLng)
                    poly.points = pathPoints

                    // Fixed Centering: Lock the roamer in the screen center
                    try {
                        val forwardIndex = (index + 12).coerceAtMost(smoothPoints.size - 1)
                        val lookAheadPoint = smoothPoints[forwardIndex]
                        val targetBearing = calculateBearing(targetLatLng, lookAheadPoint)
                        val newBearing = lerpBearing(map.cameraPosition.bearing, targetBearing, 0.08f)

                        val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                            CameraPosition.builder()
                                .target(targetLatLng)
                                .zoom(17.5f) // Slightly closer for better 3D building details
                                .tilt(65f) // Increased tilt for stronger 3D perspective
                                .bearing(newBearing)
                                .build()
                        )
                        map.moveCamera(cameraUpdate) 
                    } catch (e: Exception) {}
                }
            }
        }
        if (playbackProgress >= 1f) { isPlaying = false }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(0.7f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(0.7f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "3D 时光漫游",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            // Map HUD Controls
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MapActionBtn(Icons.Rounded.Layers) {
                    mapType = if (mapType == AMap.MAP_TYPE_NORMAL) AMap.MAP_TYPE_SATELLITE else AMap.MAP_TYPE_NORMAL
                }
                MapActionBtn(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow) {
                    if (smoothPoints.isNotEmpty()) isPlaying = !isPlaying
                    else showControls = true
                }
                MapActionBtn(Icons.Rounded.Settings) {
                    showControls = true
                }
            }

            // Bottom Panel: Info and HUD Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Stats Card (Left)
                    if (smoothPoints.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.width(170.dp),
                            color = MaterialTheme.colorScheme.surface.copy(0.85f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.2f))
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("漫游状态", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                StatItem("总里程", "%.2f km".format(cumulativeDistances.lastOrNull() ?: 0.0))
                                StatItem("记录数", "${entriesInRange.size} 篇")
                            }
                        }
                    }
                }

                // Global Control Console
                if (smoothPoints.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(0.92f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f))
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LargeFloatingActionButton(
                                    onClick = { isPlaying = !isPlaying },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("时光回放", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("${(playbackProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Slider(
                                        value = playbackProgress,
                                        onValueChange = { 
                                            playbackProgress = it
                                            val idx = (it * (smoothPoints.size-1)).toInt().coerceIn(0, smoothPoints.size-1)
                                            currentPlaybackPoint = smoothPoints[idx]
                                            mapView.map.moveCamera(CameraUpdateFactory.newLatLng(currentPlaybackPoint!!))
                                        },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("漫游航速", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Slider(
                                    value = playbackSpeedKmH,
                                    onValueChange = { playbackSpeedKmH = it },
                                    valueRange = 5f..2500f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                                )
                                Text("${playbackSpeedKmH.toInt()} km/h", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (rawPoints.isEmpty()) {
                Card(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.9f))
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Inbox, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(12.dp))
                        Text("暂无时光轨迹", fontWeight = FontWeight.Bold)
                        Text("所选日期范围内没有记录足迹点", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        if (showControls) {
            ModalBottomSheet(
                onDismissRequest = { showControls = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                ControlPanelContent(
                    startDate = startDate,
                    endDate = endDate,
                    onStartDateChange = { startDate = it },
                    onEndDateChange = { endDate = it },
                    mapType = mapType,
                    onMapTypeChange = { mapType = it },
                    entries = entriesInRange,
                    onEntryClick = { entry ->
                        if (entry.latitude != null && entry.longitude != null) {
                            mapView.map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(entry.latitude, entry.longitude), 17f), 1000, null)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MapActionBtn(icon: ImageVector, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface.copy(0.85f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(0.dp)
    ) {
        Icon(icon, null)
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ControlPanelContent(
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    mapType: Int,
    onMapTypeChange: (Int) -> Unit,
    entries: List<com.footprint.data.model.FootprintEntry>,
    onEntryClick: (com.footprint.data.model.FootprintEntry) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    if (showStartPicker) {
        ComposeDatePickerDialog(
            initialDate = startDate,
            onDateSelected = { onStartDateChange(it); showStartPicker = false },
            onDismiss = { showStartPicker = false }
        )
    }
    if (showEndPicker) {
        ComposeDatePickerDialog(
            initialDate = endDate,
            onDateSelected = { onEndDateChange(it); showEndPicker = false },
            onDismiss = { showEndPicker = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("时光回放实验室", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateSelector(modifier = Modifier.weight(1f), label = "起点日期", date = startDate) { showStartPicker = true }
            DateSelector(modifier = Modifier.weight(1f), label = "截止日期", date = endDate) { showEndPicker = true }
        }

        // Map View Settings
        Text("地图视觉设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f).clickable { 
                    onMapTypeChange(if (mapType == AMap.MAP_TYPE_NORMAL) AMap.MAP_TYPE_SATELLITE else AMap.MAP_TYPE_NORMAL)
                },
                color = if (mapType == AMap.MAP_TYPE_SATELLITE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                shape = RoundedCornerShape(16.dp),
                border = if (mapType == AMap.MAP_TYPE_SATELLITE) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SatelliteAlt, null, tint = if (mapType == AMap.MAP_TYPE_SATELLITE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(12.dp))
                    Text("卫星地图", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ViewInAr, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("3D 建筑", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = true, onCheckedChange = {}, modifier = Modifier.scale(0.8f)) // Default ON for "Stereo" request
                }
            }
        }

        if (entries.isNotEmpty()) {
            Text("途经足迹 (${entries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries) { entry ->
                    EntryListCard(entry, onClick = { onEntryClick(entry) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), ZoneOffset.UTC).toLocalDate())
                }
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun DateSelector(modifier: Modifier, label: String, date: LocalDate, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(date.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EntryListCard(entry: com.footprint.data.model.FootprintEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(entry.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
            }
            Text("%.1f".format(entry.distanceKm) + " km", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// Utility for smooth bearing calculation
fun calculateBearing(start: LatLng, end: LatLng): Float {
    val lat1 = Math.toRadians(start.latitude)
    val lon1 = Math.toRadians(start.longitude)
    val lat2 = Math.toRadians(end.latitude)
    val lon2 = Math.toRadians(end.longitude)

    val dLon = lon2 - lon1
    val y = Math.sin(dLon) * Math.cos(lat2)
    val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
    
    return ((Math.toDegrees(Math.atan2(y, x)) + 360) % 360).toFloat()
}

fun lerpBearing(start: Float, end: Float, fraction: Float): Float {
    var diff = end - start
    while (diff < -180f) diff += 360f
    while (diff > 180f) diff -= 360f
    return (start + diff * fraction + 360f) % 360f
}
