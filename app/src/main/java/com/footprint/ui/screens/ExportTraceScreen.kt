package com.footprint.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.border
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.PolylineOptions
import com.footprint.FootprintViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
    var playbackProgress by remember { mutableStateOf(0f) }
    var currentPlaybackPoint by remember { mutableStateOf<LatLng?>(null) }

    // Control Panel State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showControls by remember { mutableStateOf(true) } // Default to true so it's not "blank"
    var mapType by remember { mutableStateOf(AMap.MAP_TYPE_NORMAL) }

    // Data points
    var points by remember {
        mutableStateOf<List<com.footprint.data.local.TrackPointEntity>>(emptyList())
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
        // CRITICAL: Call onCreate for AMap to display correctly
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
            // Enhance 3D Visuals
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
        startDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
    }
    val endTimestamp = remember(endDate) {
        endDate.atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
    }

    val tracePointsFlow = remember(startTimestamp, endTimestamp) {
        viewModel.getTrackPoints(startTimestamp, endTimestamp)
    }
    val tracePoints by tracePointsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(tracePoints) {
        points = tracePoints.filter { it.latitude != 0.0 && it.longitude != 0.0 }
        if (points.isNotEmpty()) {
            val builder = LatLngBounds.builder()
            points.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
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

    // Playback Speed State
    var playbackSpeed by remember { mutableFloatStateOf(0.005f) }

    // Static Traces Draw
    LaunchedEffect(points, entriesInRange) {
        mapView.map.clear()
        if (points.isNotEmpty()) {
            val latLngs = points.map { LatLng(it.latitude, it.longitude) }
            mapView.map.addPolyline(
                PolylineOptions()
                    .addAll(latLngs)
                    .width(16f)
                    .useGradient(true)
                    .color(android.graphics.Color.parseColor("#4DEEBB"))
                    .lineJoinType(com.amap.api.maps.model.PolylineOptions.LineJoinType.LineJoinRound)
            )
        }
        entriesInRange.forEach { entry ->
            if (entry.latitude != null && entry.longitude != null) {
                mapView.map.addMarker(
                    com.amap.api.maps.model.MarkerOptions()
                        .position(LatLng(entry.latitude, entry.longitude))
                        .title(entry.title)
                        .snippet(entry.location)
                        .icon(com.amap.api.maps.model.BitmapDescriptorFactory.defaultMarker(com.amap.api.maps.model.BitmapDescriptorFactory.HUE_AZURE))
                )
            }
        }
    }

    // Playback Logic
    LaunchedEffect(isPlaying) {
        if (isPlaying && points.size > 1) {
            while (isPlaying && playbackProgress < 1f) {
                playbackProgress += playbackSpeed
                val index = (playbackProgress * (points.size - 1)).toInt()
                val point = points[index]
                val currentLatLng = LatLng(point.latitude, point.longitude)
                currentPlaybackPoint = currentLatLng
                
                // Animate Camera to follow with cinematic feel
                mapView.map.animateCamera(CameraUpdateFactory.newCameraPosition(
                    com.amap.api.maps.model.CameraPosition.builder()
                        .target(currentLatLng)
                        .zoom(16f)
                        .tilt(60f) // Dynamic Tilt
                        .bearing(mapView.map.cameraPosition.bearing + 1.0f) // Cinematic rotation
                        .build()
                ), 200, null)
                
                kotlinx.coroutines.delay(100)
            }
            if (playbackProgress >= 1f) {
                isPlaying = false
                playbackProgress = 0f
            }
        }
    }

    // Playback Marker
    val playbackMarker = remember { 
        mutableStateOf<com.amap.api.maps.model.Marker?>(null) 
    }
    LaunchedEffect(currentPlaybackPoint) {
        currentPlaybackPoint?.let { pos ->
            if (playbackMarker.value == null) {
                playbackMarker.value = mapView.map.addMarker(
                    com.amap.api.maps.model.MarkerOptions()
                        .position(pos)
                        .anchor(0.5f, 0.5f)
                        .icon(com.amap.api.maps.model.BitmapDescriptorFactory.defaultMarker(com.amap.api.maps.model.BitmapDescriptorFactory.HUE_RED))
                )
            } else {
                playbackMarker.value?.position = pos
            }
        }
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "3D 时光漫游",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(0.5f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Right Map Controls
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MapControlBtn(Icons.Rounded.Layers, "图层") {
                    mapType = if (mapType == AMap.MAP_TYPE_NORMAL) AMap.MAP_TYPE_SATELLITE else AMap.MAP_TYPE_NORMAL
                }
                MapControlBtn(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "回放") {
                    if (points.isNotEmpty()) isPlaying = !isPlaying
                    else showControls = true // Open panel to select dates if no data
                }
                MapControlBtn(Icons.Rounded.FilterList, "设置") {
                    showControls = true
                }
            }

            // Empty State Hint
            if (points.isEmpty()) {
                Card(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.9f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Place, null, size = 48.dp, tint = MaterialTheme.colorScheme.primary.copy(0.6f))
                        Text("当前时段暂无漫游轨迹", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "点击右侧 “设置” 按钮调整日期范围，\n或在主页面开启足迹记录。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Stats Overlays
            if (points.isNotEmpty()) {
                Card(
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 48.dp).width(160.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.85f)),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("漫游统计", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                        StatRow("总里程", "%.2f km".format(points.size * 0.05)) 
                        StatRow("记录点", "${points.size}")
                        StatRow("足迹数", "${entriesInRange.size}")
                    }
                }
            }
        }

        if (showControls) {
            ModalBottomSheet(
                onDismissRequest = { showControls = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface.copy(0.95f),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                ControlPanel(
                    startDate = startDate,
                    endDate = endDate,
                    onStartDateChange = { startDate = it },
                    onEndDateChange = { endDate = it },
                    isPlaying = isPlaying,
                    progress = playbackProgress,
                    onTogglePlay = { if (points.isNotEmpty()) isPlaying = !isPlaying },
                    onProgressChange = { 
                        playbackProgress = it
                        if (points.isNotEmpty()) {
                            val idx = (it * (points.size-1)).toInt().coerceIn(0, points.size-1)
                            currentPlaybackPoint = LatLng(points[idx].latitude, points[idx].longitude)
                            mapView.map.moveCamera(CameraUpdateFactory.newLatLng(currentPlaybackPoint!!))
                        }
                    },
                    playbackSpeed = playbackSpeed,
                    onPlaybackSpeedChange = { playbackSpeed = it },
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
fun MapControlBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    LargeFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface.copy(0.8f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        modifier = Modifier.size(56.dp)
    ) {
        Icon(icon, contentDescription = label)
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ControlPanel(
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlay: () -> Unit,
    onProgressChange: (Float) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    entries: List<com.footprint.data.model.FootprintEntry>,
    onEntryClick: (com.footprint.data.model.FootprintEntry) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("时光回放实验室", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)

        // Date Selectors
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DateCard(modifier = Modifier.weight(1f), label = "起点", date = startDate) {
                DatePickerDialog(context, { _, y, m, d -> onStartDateChange(LocalDate.of(y, m + 1, d)) }, startDate.year, startDate.monthValue - 1, startDate.dayOfMonth).show()
            }
            DateCard(modifier = Modifier.weight(1f), label = "终点", date = endDate) {
                DatePickerDialog(context, { _, y, m, d -> onEndDateChange(LocalDate.of(y, m + 1, d)) }, endDate.year, endDate.monthValue - 1, endDate.dayOfMonth).show()
            }
        }

        // Playback Control & Speed
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), RoundedCornerShape(20.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.5f), RoundedCornerShape(20.dp)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePlay, modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) {
                    Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("再生进度", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Slider(
                        value = progress,
                        onValueChange = onProgressChange,
                        modifier = Modifier.height(32.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Speed, null, size = 18.dp, tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(8.dp))
                Text("回放速度", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Slider(
                    value = playbackSpeed,
                    onValueChange = onPlaybackSpeedChange,
                    valueRange = 0.001f..0.02f,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )
                Text("${(playbackSpeed * 1000).toInt()}x", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }

        // Entries List
        if (entries.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("途经足迹 (${entries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { /* Could add filter/sort */ }) {
                    Text("全部轨迹", style = MaterialTheme.typography.labelMedium)
                }
            }
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(entries) { entry ->
                    EntrySmallCard(entry, onClick = { onEntryClick(entry) })
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("该时段内暂无足迹记录", color = MaterialTheme.colorScheme.outline)
            }
        }
        
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun DateCard(modifier: Modifier, label: String, date: LocalDate, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), RoundedCornerShape(16.dp)).padding(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EntrySmallCard(entry: com.footprint.data.model.FootprintEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(entry.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("%.1f".format(entry.distanceKm), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            Text("km", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector, contentDescription, modifier = Modifier.size(size), tint = tint)
}
