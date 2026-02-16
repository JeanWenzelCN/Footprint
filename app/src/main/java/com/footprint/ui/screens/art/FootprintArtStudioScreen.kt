package com.footprint.ui.screens.art

import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.footprint.FootprintViewModel
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootprintArtStudioScreen(
    viewModel: FootprintViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    val mapView = remember { 
        MapView(context).apply { 
            onCreate(null) 
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
        
        // Handle initial state if verified (e.g. if already RESUMED)
        // AMap requires onResume to be called to start rendering
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    
    // State for controls
    var lineWeight by remember { mutableFloatStateOf(5f) }
    var glowRadius by remember { mutableFloatStateOf(15f) }
    var selectedColor by remember { mutableStateOf(Color(0xFF00FF9F)) } // Neon Green
    var mapStyle by remember { mutableStateOf(ArtMapStyle.DARK) }
    var selectedLayout by remember { mutableStateOf(ArtLayout.FULLCREEN_A24) }
    var showControls by remember { mutableStateOf(true) }
    
    // Time Scope (Default to this year)
    var startDate by remember { mutableStateOf(LocalDate.now().withDayOfYear(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    val startTimestamp = remember(startDate) {
        LocalDateTime.of(startDate, LocalTime.MIN).toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    val endTimestamp = remember(endDate) {
        LocalDateTime.of(endDate, LocalTime.MAX).toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    
    val tracePoints by viewModel.getTrackPoints(startTimestamp, endTimestamp)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    
    // Calc distance
    val totalDistanceKm = remember(tracePoints) {
        if (tracePoints.size < 2) 0.0
        else {
            var dist = 0.0
            for (i in 0 until tracePoints.size - 1) {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    tracePoints[i].latitude, tracePoints[i].longitude,
                    tracePoints[i+1].latitude, tracePoints[i+1].longitude,
                    results
                )
                dist += results[0]
            }
            dist / 1000.0
        }
    }

    // ... (Data Fetching code unchanged) ...

    // ... (Map Lifecycle & Style updates & Glow Effect unchanged) ...

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Base Map Layer
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Void Mode Logic
        // ...

        // 2. Art Canvas Layer -> Removed

        // 3. Layout Overlay
        ArtLayoutOverlay(
            layout = selectedLayout,
            distanceKm = totalDistanceKm,
            dateRange = "${startDate.year}.${startDate.monthValue} - ${endDate.year}.${endDate.monthValue}",
            modifier = Modifier.fillMaxSize()
        )

        // 4. UI Controls Overlay
        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ArtStyleControls(
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
                            { _, y, m, d -> startDate = LocalDate.of(y, m + 1, d) },
                            startDate.year, startDate.monthValue - 1, startDate.dayOfMonth
                        ).show()
                    },
                    onEndDateClick = {
                        android.app.DatePickerDialog(
                            context,
                            { _, y, m, d -> endDate = LocalDate.of(y, m + 1, d) },
                            endDate.year, endDate.monthValue - 1, endDate.dayOfMonth
                        ).show()
                    },
                    layout = selectedLayout,
                    onLayoutChange = { selectedLayout = it }
                )
            }
        }

        // Top Bar & Export Button
        if (showControls) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 48.dp, start = 16.dp, end = 16.dp)) {
                SmallFloatingActionButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.TopStart),
                    containerColor = Color.Black.copy(alpha = 0.5f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                
                ExtendedFloatingActionButton(
                    onClick = {
                        showControls = false
                        // Use a handler to delay capture until UI updates
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            val activity = context as? android.app.Activity ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
                            activity?.let { act ->
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    com.footprint.utils.ExportUtils.captureWindow(act) { bitmap ->
                                        // Save in background
                                        Thread {
                                            val path = com.footprint.utils.ExportUtils.saveBitmapToGallery(context, bitmap)
                                            activity.runOnUiThread {
                                                showControls = true
                                                if (path != null) {
                    android.widget.Toast.makeText(context, "已保存到相册", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "保存失败", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }.start()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "导出功能需要 Android 8.0 以上版本", android.widget.Toast.LENGTH_SHORT).show()
                                    showControls = true
                                }
                            }
                        }, 200)
                    },
                    modifier = Modifier.align(Alignment.TopEnd),
                    containerColor = Color(0xFF00FF9F),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Default.Download, "导出")
                    Spacer(Modifier.width(8.dp))
                    Text("导出海报")
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
    modifier: Modifier = Modifier
) {
    when(layout) {
        ArtLayout.FULLCREEN_A24 -> {
            Box(modifier = modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.BottomCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "漂泊的灵魂",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "深圳 • $dateRange • %.1f KM".format(distanceKm),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 2.sp
                    )
                }
            }
        }
        ArtLayout.POLAROID -> {
             Box(modifier = modifier.fillMaxSize()) {
                 // White Frame
                 Canvas(modifier = Modifier.fillMaxSize()) {
                     // Draw white border with hole
                     val frameWidth = size.width * 0.8f
                     val frameHeight = size.width * 0.8f // Square aspect for the image part
                     val topOffset = size.height * 0.15f
                     val leftOffset = (size.width - frameWidth) / 2
                     
                     // Draw solid white background
                     drawRect(Color.White)
                     
                     // Clear the hole (This is tricky in Compose Canvas without Layer)
                     // Instead, draw 4 white rectangles around the hole.
                     
                     // Top rect
                     drawRect(Color.White, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, topOffset))
                     // Bottom rect
                     drawRect(Color.White, topLeft = Offset(0f, topOffset + frameHeight), size = androidx.compose.ui.geometry.Size(size.width, size.height - (topOffset + frameHeight)))
                     // Left rect
                     drawRect(Color.White, topLeft = Offset(0f, topOffset), size = androidx.compose.ui.geometry.Size(leftOffset, frameHeight))
                     // Right rect
                     drawRect(Color.White, topLeft = Offset(leftOffset + frameWidth, topOffset), size = androidx.compose.ui.geometry.Size(leftOffset, frameHeight))
                 }
                 
                 // Text at bottom
                 Column(
                     modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 180.dp), // Adjust for controls
                     horizontalAlignment = Alignment.CenterHorizontally
                 ) {
                      Text(
                        "我的足迹",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Black,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive
                    )
                    Text(
                        "$dateRange | %.1f km".format(distanceKm),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                 }
                 
                 // Logo/Watermark
                 // Icon(Icons.Default.Fingerprint, null, tint = Color.Black.copy(0.1f), modifier = Modifier.align(Alignment.BottomEnd).padding(32.dp).size(48.dp))
             }
        }
        ArtLayout.GEEK_STATS -> {
             // Side panel implementation
        }
    }
}



