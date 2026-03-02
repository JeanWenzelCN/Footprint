package com.footprint.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    // Default to selected year or today
    var startDate by remember {
        mutableStateOf(
                if (initialYear != null) LocalDate.of(initialYear, 1, 1) else LocalDate.now()
        )
    }
    var startHour by remember { mutableStateOf(0) }
    var startMinute by remember { mutableStateOf(0) }

    var endDate by remember {
        mutableStateOf(
                if (initialYear != null) LocalDate.of(initialYear, 12, 31) else LocalDate.now()
        )
    }
    var endHour by remember {
        mutableStateOf(if (initialYear != null) 23 else LocalTime.now().hour)
    }
    var endMinute by remember {
        mutableStateOf(if (initialYear != null) 59 else LocalTime.now().minute)
    }

    // Points state
    var points by remember {
        mutableStateOf<List<com.footprint.data.local.TrackPointEntity>>(emptyList())
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val entriesInRange =
            remember(uiState.entries, startDate, endDate) {
                uiState.entries.filter {
                    !it.happenedOn.isBefore(startDate) && !it.happenedOn.isAfter(endDate)
                }
            }

    // Map lifecycle
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer =
                androidx.lifecycle.LifecycleEventObserver { _, event ->
                    when (event) {
                        androidx.lifecycle.Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                        androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                        else -> {}
                    }
                }
        lifecycle.addObserver(observer)

        // Handle initial state if verified (e.g. if already RESUMED)
        if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }

        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(isDark) {
        mapView.map.mapType = if (isDark) AMap.MAP_TYPE_NIGHT else AMap.MAP_TYPE_NORMAL
    }

    LaunchedEffect(points, uiState.entries, startDate, endDate) {
        mapView.map.clear()
        val validPoints = points.filter { it.latitude != 0.0 && it.longitude != 0.0 }

        val builder = LatLngBounds.builder()
        var hasBounds = false

        if (validPoints.isNotEmpty()) {
            val latLngs = validPoints.map { LatLng(it.latitude, it.longitude) }
            mapView.map.addPolyline(
                    PolylineOptions()
                            .addAll(latLngs)
                            .width(18f)
                            .color(android.graphics.Color.parseColor("#00FF9F"))
            )
            latLngs.forEach {
                builder.include(it)
                hasBounds = true
            }
        }

        val entriesInRange =
                uiState.entries.filter {
                    !it.happenedOn.isBefore(startDate) && !it.happenedOn.isAfter(endDate)
                }
        val validEntries = entriesInRange.filter { it.latitude != null && it.longitude != null }
        validEntries.forEach { entry ->
            val position = LatLng(entry.latitude!!, entry.longitude!!)
            mapView.map.addMarker(
                    com.amap.api.maps.model.MarkerOptions()
                            .position(position)
                            .title(entry.title)
                            .snippet("${entry.location} | ${entry.distanceKm}km")
            )
            builder.include(position)
            hasBounds = true
        }

        if (hasBounds) {
            try {
                mapView.map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
            } catch (e: Exception) {
                if (validPoints.isNotEmpty()) {
                    mapView.map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    LatLng(validPoints[0].latitude, validPoints[0].longitude),
                                    15f
                            )
                    )
                } else if (validEntries.isNotEmpty()) {
                    mapView.map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                    LatLng(validEntries[0].latitude!!, validEntries[0].longitude!!),
                                    15f
                            )
                    )
                }
            }
        }
    }

    // Better approach: State driven.
    val startTimestamp =
            remember(startDate, startHour, startMinute) {
                LocalDateTime.of(startDate, LocalTime.of(startHour, startMinute))
                        .toInstant(
                                ZoneOffset.systemDefault().rules.getOffset(java.time.Instant.now())
                        )
                        .toEpochMilli()
            }
    val endTimestamp =
            remember(endDate, endHour, endMinute) {
                LocalDateTime.of(endDate, LocalTime.of(endHour, endMinute))
                        .toInstant(
                                ZoneOffset.systemDefault().rules.getOffset(java.time.Instant.now())
                        )
                        .toEpochMilli()
            }

    val tracePoints by
            viewModel
                    .getTrackPoints(startTimestamp, endTimestamp)
                    .collectAsStateWithLifecycle(initialValue = emptyList())

    // Update local points when flow emits
    LaunchedEffect(tracePoints) { points = tracePoints }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
                factory = {
                    mapView.apply {
                        map.mapType = if (isDark) AMap.MAP_TYPE_NIGHT else AMap.MAP_TYPE_NORMAL
                    }
                },
                modifier = Modifier.fillMaxSize()
        )

        // Top Bar
        Row(modifier = Modifier.padding(top = 48.dp, start = 16.dp).align(Alignment.TopStart)) {
            SmallFloatingActionButton(
                    onClick = onBack,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Controls
        Card(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor =
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        ),
                border =
                        BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
        ) {
            Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                        "足迹回放",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeSelector(
                            label = "开始",
                            date = startDate,
                            hour = startHour,
                            minute = startMinute,
                            onDateChange = { startDate = it },
                            onTimeChange = { h, m ->
                                startHour = h
                                startMinute = m
                            }
                    )

                    Text("to", color = MaterialTheme.colorScheme.outline)

                    TimeSelector(
                            label = "结束",
                            date = endDate,
                            hour = endHour,
                            minute = endMinute,
                            onDateChange = { endDate = it },
                            onTimeChange = { h, m ->
                                endHour = h
                                endMinute = m
                            }
                    )
                }

                val totalDistanceKm =
                        remember(points) {
                            if (points.size < 2) 0f
                            else {
                                var dist = 0f
                                for (i in 0 until points.size - 1) {
                                    val results = FloatArray(1)
                                    android.location.Location.distanceBetween(
                                            points[i].latitude,
                                            points[i].longitude,
                                            points[i + 1].latitude,
                                            points[i + 1].longitude,
                                            results
                                    )
                                    dist += results[0]
                                }
                                dist / 1000f
                            }
                        }

                Button(
                        onClick = { /* Auto updates via Flow */},
                        modifier = Modifier.fillMaxWidth(),
                        enabled = points.isNotEmpty() || entriesInRange.isNotEmpty()
                ) { Text("显示 ${points.size} 个记录点 | 里程: %.3f km".format(totalDistanceKm)) }

                if (entriesInRange.isNotEmpty()) {
                    HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                            "包含 ${entriesInRange.size} 次足迹记录",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                    )
                    LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entriesInRange, key = { it.id }) { entry ->
                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .background(
                                                            MaterialTheme.colorScheme.surfaceVariant
                                                                    .copy(alpha = 0.5f),
                                                            RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                            entry.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                            "${entry.happenedOn} · ${entry.location}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                        "${String.format("%.3f", entry.distanceKm)} km",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeSelector(
        label: String,
        date: LocalDate,
        hour: Int,
        minute: Int,
        onDateChange: (LocalDate) -> Unit,
        onTimeChange: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                        Modifier.background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        onDateChange(LocalDate.of(y, m + 1, d))
                                                    },
                                                    date.year,
                                                    date.monthValue - 1,
                                                    date.dayOfMonth
                                            )
                                            .show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                        Modifier.background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    android.app.TimePickerDialog(
                                                    context,
                                                    { _, h, m -> onTimeChange(h, m) },
                                                    hour,
                                                    minute,
                                                    true
                                            )
                                            .show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                    text = String.format("%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
