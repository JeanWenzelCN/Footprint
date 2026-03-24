package com.footprint.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.footprint.FootprintViewModel
import com.footprint.R
import com.footprint.data.local.TrackPointEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

enum class ArtPalette(val label: String, val colors: List<androidx.compose.ui.graphics.Color>) {
    CYBERPUNK("赛博朋克", listOf(androidx.compose.ui.graphics.Color(0xFF00E5FF), androidx.compose.ui.graphics.Color(0xFF651FFF), androidx.compose.ui.graphics.Color(0xFFFF4081))),
    AURORA("极光之境", listOf(androidx.compose.ui.graphics.Color(0xFF00FF9F), androidx.compose.ui.graphics.Color(0xFF00B8FF), androidx.compose.ui.graphics.Color(0xFF001AFF))),
    VOLCANO("熔岩赤红", listOf(androidx.compose.ui.graphics.Color(0xFFFF5722), androidx.compose.ui.graphics.Color(0xFFFFC107), androidx.compose.ui.graphics.Color(0xFFD50000))),
    FOREST("翡翠绿意", listOf(androidx.compose.ui.graphics.Color(0xFF00C853), androidx.compose.ui.graphics.Color(0xFFB2FF59), androidx.compose.ui.graphics.Color(0xFF1B5E20))),
    MONO("水墨黑白", listOf(androidx.compose.ui.graphics.Color.White, androidx.compose.ui.graphics.Color.LightGray, androidx.compose.ui.graphics.Color.DarkGray))
}

enum class ArtStyle(val label: String) {
    LIQUID_NEON("霓虹流光"),
    HEATMAP("时空格点"),
    TEMPORAL_FLOW("时光幻影"),
    GALAXY_DUST("极星尘埃"),
    GLOBE_HEATMAP("时空地球")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerativeArtScreen(viewModel: FootprintViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var artStyle by remember { mutableStateOf(ArtStyle.LIQUID_NEON) }
    var timeFilter by remember { mutableFloatStateOf(1f) } // 0.0 to 1.0
    var selectedPalette by remember { mutableStateOf(ArtPalette.CYBERPUNK) }
    var visualDensity by remember { mutableFloatStateOf(0.5f) } // 0.1 to 1.0
    var randomSeed by remember { mutableLongStateOf(42L) }

    // Globe rotation state
    var globeRotateX by remember { mutableFloatStateOf(0f) }
    var globeRotateY by remember { mutableFloatStateOf(0f) }

    // Get all track points (or a sample)
    val allTrackPoints by viewModel.getHeatmapPoints().collectAsStateWithLifecycle(initialValue = emptyList())

    val filteredPoints = remember(allTrackPoints, timeFilter) {
        if (allTrackPoints.isEmpty()) emptyList()
        else {
            val count = (allTrackPoints.size * timeFilter).toInt().coerceAtLeast(1)
            allTrackPoints.take(count)
        }
    }

    val displayDateRange = remember(filteredPoints) {
        if (filteredPoints.isEmpty()) "暂无数据"
        else {
            val start = Instant.ofEpochMilli(filteredPoints.first().timestamp)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val end = Instant.ofEpochMilli(filteredPoints.last().timestamp)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            "${start.format(formatter)} - ${end.format(formatter)}"
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("足迹工坊 · 时空热力", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White)
                        Text(displayDateRange, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { randomSeed = System.currentTimeMillis() }) {
                        Icon(Icons.Default.Refresh, "Randomize", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.9f),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0A0A0A) // Darker background for art
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Art Preview Area with glowing border
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF1A1A1A), Color.Black),
                            center = Offset.Infinite
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        ),
                        RoundedCornerShape(32.dp)
                    )
                    .pointerInput(artStyle) {
                        if (artStyle == ArtStyle.GLOBE_HEATMAP) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                globeRotateY += dragAmount.x * 0.5f // L-R drag rotates around vertical axis (Y)
                                globeRotateX -= dragAmount.y * 0.5f // U-D drag rotates around horizontal axis (X)
                            }
                        }
                    }
            ) {
                if (filteredPoints.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArt(this, filteredPoints, artStyle, selectedPalette, visualDensity, randomSeed, size, globeRotateX, globeRotateY)
                        
                        // Overlay decorative text
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 36f
                            alpha = 80
                            isFakeBoldText = true
                            letterSpacing = 0.2f
                        }
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            "FOOTPRINT ART // ${artStyle.label} // SEED:${randomSeed.toString().takeLast(4)}",
                            60f, size.height - 80f, paint
                        )

                        if (artStyle == ArtStyle.GLOBE_HEATMAP) {
                            drawContext.canvas.nativeCanvas.drawText(
                                "DRAG TO ROTATE EARTH",
                                size.width / 2f - 180f, 100f, paint
                            )
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Controls
            Card(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E).copy(alpha = 0.98f) // Forced dark card for art controls
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("时空演进", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                        Spacer(Modifier.weight(1f))
                        Text("${(timeFilter * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                    Slider(
                        value = timeFilter,
                        onValueChange = { timeFilter = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("视觉风格", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArtStyle.entries.forEach { style ->
                            val isSelected = artStyle == style
                            Surface(
                                selected = isSelected,
                                onClick = { artStyle = style },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.width(100.dp)
                            ) {
                                Box(Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        style.label, 
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.7f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text("艺术配色", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ArtPalette.entries.forEach { palette ->
                            val isSelected = selectedPalette == palette
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedPalette = palette }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            Brush.sweepGradient(palette.colors),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                        .border(
                                            if (isSelected) 2.dp else 0.dp,
                                            Color.White,
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    palette.label, 
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("视觉浓度", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.9f))
                        Spacer(Modifier.weight(1f))
                        Text("${(visualDensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                    Slider(
                        value = visualDensity,
                        onValueChange = { visualDensity = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.tertiary,
                            activeTrackColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val bitmap = captureArtToBitmap(context, filteredPoints, artStyle, selectedPalette, visualDensity, randomSeed, globeRotateX, globeRotateY)
                                    saveAndShareArt(context, bitmap)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "导出失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(painterResource(R.drawable.ic_share), null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存并分享永恒轨迹", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun drawArt(
    drawScope: DrawScope,
    trackPoints: List<TrackPointEntity>,
    artStyle: ArtStyle,
    palette: ArtPalette,
    density: Float,
    seed: Long,
    size: Size,
    rotateX: Float = 0f,
    rotateY: Float = 0f
) {
    with(drawScope) {
        val width = size.width
        val height = size.height

        if (trackPoints.isEmpty()) return

        when (artStyle) {
            ArtStyle.GLOBE_HEATMAP -> {
                // --- 3D Globe Implementation ---
                val radius = minOf(width, height) * 0.4f
                val centerX = width / 2f
                val centerY = height / 2f

                // Draw background sphere (subtle glowing base)
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color(0xFF151515),
                        0.8f to Color(0xFF0F0F0F),
                        1.0f to palette.colors.first().copy(alpha = 0.3f)
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )

                // Draw Latitude/Longitude grid lines (Holographic effect)
                val gridAlpha = 0.15f

                // Simplified grid rendering
                for (lat in -90..90 step 30) {
                    val path = Path()
                    for (lng in -180..180 step 10) {
                        val pos = projectGlobe(lat.toDouble(), lng.toDouble(), radius, rotateX, rotateY)
                        if (pos.z > 0) { // Only visible side
                            val x = centerX + pos.x
                            val y = centerY + pos.y
                            if (path.isEmpty) path.moveTo(x, y) else path.lineTo(x, y)
                        } else {
                            if (!path.isEmpty) {
                                drawPath(path, palette.colors.first().copy(alpha = gridAlpha), style = Stroke(width = 1f))
                                path.reset()
                            }
                        }
                    }
                    drawPath(path, palette.colors.first().copy(alpha = gridAlpha), style = Stroke(width = 1f))
                }
                
                for (lng in -180..180 step 30) {
                    val path = Path()
                    for (lat in -90..90 step 5) {
                        val pos = projectGlobe(lat.toDouble(), lng.toDouble(), radius, rotateX, rotateY)
                        if (pos.z > 0) {
                            val x = centerX + pos.x
                            val y = centerY + pos.y
                            if (path.isEmpty) path.moveTo(x, y) else path.lineTo(x, y)
                        } else {
                            if (!path.isEmpty) {
                                drawPath(path, palette.colors.first().copy(alpha = gridAlpha), style = Stroke(width = 1f))
                                path.reset()
                            }
                        }
                    }
                    drawPath(path, palette.colors.first().copy(alpha = gridAlpha), style = Stroke(width = 1f))
                }

                // Draw Heat Points on Globe
                // Collect points into buckets for density calculation if needed, but additive blending works well
                trackPoints.forEach { pt ->
                    val pos = projectGlobe(pt.latitude, pt.longitude, radius, rotateX, rotateY)
                    
                    // Only draw if on the visible hemisphere (z > 0)
                    if (pos.z > 0) {
                        val x = centerX + pos.x
                        val y = centerY + pos.y
                        
                        // Distance from the "front" of the sphere can affect size/opacity
                        val depthScale = (pos.z / radius).coerceIn(0f, 1f)
                        val pointSize = (8f + 20f * density) * depthScale
                        
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to palette.colors.last().copy(alpha = 0.8f * density),
                                0.5f to palette.colors.getOrElse(1) { palette.colors.first() }.copy(alpha = 0.3f),
                                1.0f to Color.Transparent
                            ),
                            radius = pointSize,
                            center = Offset(x, y),
                            blendMode = BlendMode.Plus
                        )
                        
                        // Tiny core for "hottest" part
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f * depthScale),
                            radius = 1f + 2f * density,
                            center = Offset(x, y)
                        )
                    }
                }

                // Draw Glossy overlay
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color.Transparent,
                        0.9f to Color.Transparent,
                        1.0f to Color.White.copy(alpha = 0.1f)
                    ),
                    radius = radius + 2f,
                    center = Offset(centerX, centerY)
                )
            }
            else -> {
                val minLat = trackPoints.minOf { it.latitude }
                val maxLat = trackPoints.maxOf { it.latitude }
                val minLng = trackPoints.minOf { it.longitude }
                val maxLng = trackPoints.maxOf { it.longitude }
        
                val latRange = maxLat - minLat
                val lngRange = maxLng - minLng
                
                // Normalize coordinates and add padding
                val padding = 0.15f
                val displayWidth = width * (1 - padding * 2)
                val displayHeight = height * (1 - padding * 2)
                
                val scale = if (latRange > 0 && lngRange > 0) {
                    minOf(displayWidth.toDouble() / lngRange, displayHeight.toDouble() / latRange)
                } else 1.0
        
                val offsetX = (width.toDouble() - lngRange * scale) / 2.0
                val offsetY = (height.toDouble() - latRange * scale) / 2.0
        
                fun projectX(lng: Double) = ((lng - minLng) * scale + offsetX).toFloat()
                fun projectY(lat: Double) = (height.toDouble() - ((lat - minLat) * scale + offsetY)).toFloat()

                when (artStyle) {
                    ArtStyle.LIQUID_NEON -> {
                        val path = Path()
                        trackPoints.forEachIndexed { index, pt ->
                            val x = projectX(pt.longitude)
                            val y = projectY(pt.latitude)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            brush = Brush.linearGradient(
                                colors = palette.colors,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            ),
                            style = Stroke(width = 12f + 40f * density, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            alpha = 0.4f + 0.3f * density
                        )
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(width = 2f + 4f * density, cap = StrokeCap.Round),
                            alpha = 0.6f + 0.3f * density
                        )
                    }
                    ArtStyle.HEATMAP -> {
                        trackPoints.forEach { pt ->
                            val x = projectX(pt.longitude)
                            val y = projectY(pt.latitude)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    0.0f to palette.colors.first().copy(alpha = 0.1f + 0.2f * density),
                                    0.7f to palette.colors.getOrElse(1) { palette.colors.first() }.copy(alpha = 0.05f),
                                    1.0f to Color.Transparent
                                ),
                                radius = 40f + 80f * density,
                                center = Offset(x, y),
                                blendMode = BlendMode.Screen
                            )
                        }
                    }
                    ArtStyle.TEMPORAL_FLOW -> {
                        val total = trackPoints.size
                        trackPoints.forEachIndexed { index, pt ->
                            val x = projectX(pt.longitude)
                            val y = projectY(pt.latitude)
                            val progress = index.toFloat() / total
                            
                            if (index > 0) {
                                val prev = trackPoints[index - 1]
                                drawLine(
                                    brush = Brush.linearGradient(palette.colors),
                                    start = Offset(projectX(prev.longitude), projectY(prev.latitude)),
                                    end = Offset(x, y),
                                    strokeWidth = (2f + progress * 20f) * density * 2f,
                                    cap = StrokeCap.Round,
                                    alpha = progress * (0.4f + 0.5f * density)
                                )
                            }
                        }
                    }
                    ArtStyle.GALAXY_DUST -> {
                        val rand = Random(seed)
                        trackPoints.forEach { pt ->
                            val x = projectX(pt.longitude)
                            val y = projectY(pt.latitude)
                            val pSize = (2..12).random(rand).toFloat() * (0.5f + density)
                            
                            drawCircle(
                                color = palette.colors.random(rand),
                                radius = pSize / 2,
                                center = Offset(x, y),
                                alpha = (rand.nextFloat() * 0.6f + 0.3f) * density
                            )
                            
                            if (rand.nextFloat() > (1.0f - 0.1f * density)) {
                                val glowSize = pSize * (4f + 8f * density)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        0f to palette.colors.random(rand).copy(alpha = 0.4f),
                                        1f to Color.Transparent
                                    ),
                                    radius = glowSize,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

/** 
 * Data class to hold 3D projection result 
 */
private data class Vec3(val x: Float, val y: Float, val z: Float)

/**
 * Projects a Latitude/Longitude coordinate onto a 3D sphere and applies rotation.
 * @param lat Latitude in degrees
 * @param lng Longitude in degrees
 * @param radius Sphere radius
 * @param rotateX Rotation around X-axis (up-down) in degrees
 * @param rotateY Rotation around Y-axis (left-right) in degrees
 */
private fun projectGlobe(lat: Double, lng: Double, radius: Float, rotateX: Float, rotateY: Float): Vec3 {
    // Convert to radians
    val phi = (90.0 - lat) * PI / 180.0
    val theta = (lng + 180.0) * PI / 180.0

    // Sphere coordinates (z is depth, positive is towards viewer)
    var x = (radius * sin(phi) * cos(theta)).toFloat()
    var y = (radius * cos(phi)).toFloat()
    var z = (radius * sin(phi) * sin(theta)).toFloat()

    // Apply rotation around Y-axis (vertical)
    val ry = rotateY * PI.toFloat() / 180f
    val cosRY = cos(ry)
    val sinRY = sin(ry)
    val nx = x * cosRY + z * sinRY
    val nz = -x * sinRY + z * cosRY
    x = nx
    z = nz

    // Apply rotation around X-axis (horizontal)
    val rx = rotateX * PI.toFloat() / 180f
    val cosRX = cos(rx)
    val sinRX = sin(rx)
    val ny = y * cosRX - z * sinRX
    val nz2 = y * sinRX + z * cosRX
    y = ny
    z = nz2

    return Vec3(x, y, z)
}

private fun captureArtToBitmap(
        context: android.content.Context,
        points: List<TrackPointEntity>,
        artStyle: ArtStyle,
        palette: ArtPalette,
        density: Float,
        seed: Long,
        rotX: Float = 0f,
        rotY: Float = 0f
): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(android.graphics.Color.BLACK) // Same as background

        val drawScope = CanvasDrawScope()
        drawScope.draw(
                density = Density(context),
                layoutDirection = LayoutDirection.Ltr,
                canvas = androidx.compose.ui.graphics.Canvas(canvas),
                size = Size(width.toFloat(), height.toFloat())
        ) { drawArt(this, points, artStyle, palette, density, seed, size, rotX, rotY) }
        return bitmap
}

private fun saveAndShareArt(context: android.content.Context, bitmap: Bitmap) {
        val filename = "FootprintArt_${System.currentTimeMillis()}.png"
        val contentValues =
                android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                put(
                                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                                        android.os.Environment.DIRECTORY_PICTURES + "/FootprintArt"
                                )
                                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                }

        val contentResolver = context.contentResolver
        val imageUri =
                contentResolver.insert(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                )

        imageUri?.let { uri ->
                try {
                        contentResolver.openOutputStream(uri)?.use { out ->
                                bitmap.compress(
                                        android.graphics.Bitmap.CompressFormat.PNG,
                                        100,
                                        out
                                )
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                contentValues.clear()
                                contentValues.put(
                                        android.provider.MediaStore.MediaColumns.IS_PENDING,
                                        0
                                )
                                contentResolver.update(uri, contentValues, null, null)
                        }

                        android.widget.Toast.makeText(
                                        context,
                                        "艺术画已保存至相册并调起分享",
                                        android.widget.Toast.LENGTH_SHORT
                                )
                                .show()

                        // Sharing
                        val shareIntent =
                                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(
                                                android.content.Intent
                                                        .FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                }
                        context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "分享足迹艺术")
                        )
                } catch (e: Exception) {
                        android.widget.Toast.makeText(
                                        context,
                                        "保存失败: ${e.message}",
                                        android.widget.Toast.LENGTH_SHORT
                                )
                                .show()
                }
        }
}
