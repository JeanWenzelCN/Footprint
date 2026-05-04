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
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

private data class StarSpec(val xFactor: Float, val yFactor: Float, val radius: Float, val alpha: Float)
private data class GlobePoint(val lat: Double, val lng: Double)
private data class ProjectedTrackPoint(val position: Vec3)

private val GlobeLandOutlines = listOf(
    listOf(GlobePoint(70.0, -10.0), GlobePoint(75.0, 60.0), GlobePoint(60.0, 180.0), GlobePoint(10.0, 110.0), GlobePoint(10.0, 70.0), GlobePoint(20.0, 30.0)),
    listOf(GlobePoint(35.0, -20.0), GlobePoint(35.0, 50.0), GlobePoint(-35.0, 20.0), GlobePoint(-35.0, 10.0), GlobePoint(5.0, -10.0)),
    listOf(GlobePoint(75.0, -170.0), GlobePoint(75.0, -50.0), GlobePoint(15.0, -90.0), GlobePoint(15.0, -110.0)),
    listOf(GlobePoint(15.0, -80.0), GlobePoint(15.0, -40.0), GlobePoint(-55.0, -70.0), GlobePoint(-10.0, -80.0)),
    listOf(GlobePoint(-10.0, 110.0), GlobePoint(-10.0, 150.0), GlobePoint(-40.0, 150.0), GlobePoint(-40.0, 110.0)),
    listOf(GlobePoint(85.0, -70.0), GlobePoint(85.0, -10.0), GlobePoint(60.0, -40.0))
)

private val GlobeCityLights = List(150) {
    val rand = Random(42 + it)
    GlobePoint(
        lat = ((rand.nextFloat() * 140) - 60).toDouble(),
        lng = ((rand.nextFloat() * 360) - 180).toDouble()
    )
}

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
    // 1. Set GLOBE_HEATMAP as default style
    var artStyle by remember { mutableStateOf(ArtStyle.GLOBE_HEATMAP) }
    var timeFilter by remember { mutableFloatStateOf(1f) } 
    var selectedPalette by remember { mutableStateOf(ArtPalette.CYBERPUNK) }
    var visualDensity by remember { mutableFloatStateOf(0.5f) } 
    var randomSeed by remember { mutableLongStateOf(42L) }

    // Globe rotation and scale state
    var globeRotateX by remember { mutableFloatStateOf(0f) }
    var globeRotateY by remember { mutableFloatStateOf(0f) }
    var globeScale by remember { mutableFloatStateOf(1.2f) }
    var isUserInteracting by remember { mutableStateOf(false) }

    // Auto-rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "GlobeRotation")
    val autoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AutoRotation"
    )
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanLine"
    )

    // Combine auto and manual rotation
    val currentRotateY = if (isUserInteracting || artStyle != ArtStyle.GLOBE_HEATMAP) globeRotateY else globeRotateY + autoRotation

    // Get all track points
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
    val starField = remember(randomSeed) {
        val rand = Random(randomSeed)
        List(120) {
            StarSpec(
                xFactor = rand.nextFloat(),
                yFactor = rand.nextFloat(),
                radius = 0.5f + rand.nextFloat() * 1.5f,
                alpha = rand.nextFloat() * 0.8f
            )
        }
    }
    val overlayPaint = remember {
        AndroidPaint().apply {
            color = AndroidColor.WHITE
            textSize = 32f
            alpha = 100
            isFakeBoldText = true
            letterSpacing = 0.1f
        }
    }

    // 2. Move pointerInput to the outermost Box to ensure it captures all gestures
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(artStyle) {
                if (artStyle == ArtStyle.GLOBE_HEATMAP) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        isUserInteracting = true
                        globeScale = (globeScale * zoom).coerceIn(0.5f, 10f)
                        globeRotateY += pan.x * 0.5f / globeScale
                        globeRotateX -= pan.y * 0.5f / globeScale
                    }
                }
            }
    ) {
        // Full Screen Art Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArt(
                drawScope = this,
                trackPoints = filteredPoints,
                artStyle = artStyle,
                palette = selectedPalette,
                density = visualDensity,
                seed = randomSeed,
                size = size,
                rotateX = globeRotateX,
                rotateY = currentRotateY,
                scale = globeScale,
                scanLineProgress = scanLineProgress,
                starField = starField
            )
            
            if (artStyle == ArtStyle.GLOBE_HEATMAP) {
                drawGlobeOverlays(this, size, filteredPoints.size, globeScale, overlayPaint)
            }
        }

        // UI Layer (Controls)
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("足迹工坊 · 时空地球", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color.White)
                        Text(displayDateRange, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        randomSeed = System.currentTimeMillis()
                        globeScale = 1.2f
                        globeRotateX = 0f
                        globeRotateY = 0f
                        isUserInteracting = false
                    }) {
                        Icon(Icons.Default.Refresh, "Reset", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Controls - Floating Glassmorphism
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("时空演进", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        Spacer(Modifier.weight(1f))
                        Text("${(timeFilter * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = timeFilter,
                        onValueChange = { timeFilter = it },
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("视觉浓度", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        Spacer(Modifier.weight(1f))
                        Text("${(visualDensity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Slider(
                        value = visualDensity,
                        onValueChange = { visualDensity = it },
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.tertiary)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArtStyle.entries.forEach { style ->
                            val isSelected = artStyle == style
                            InputChip(
                                selected = isSelected,
                                onClick = { artStyle = style },
                                label = { Text(style.label) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ArtPalette.entries.take(4).forEach { palette ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Brush.sweepGradient(palette.colors), CircleShape)
                                    .clickable { selectedPalette = palette }
                                    .border(if(selectedPalette == palette) 2.dp else 0.dp, Color.White, CircleShape)
                            )
                        }
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    val bitmap = captureArtToBitmap(context, filteredPoints, artStyle, selectedPalette, visualDensity, randomSeed, globeRotateX, currentRotateY, globeScale)
                                    saveAndShareArt(context, bitmap)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(painterResource(R.drawable.ic_share), null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("导出轨迹", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun drawGlobeOverlays(
    drawScope: DrawScope,
    size: Size,
    pointCount: Int,
    scale: Float,
    overlayPaint: AndroidPaint
) {
    with(drawScope) {
        val margin = 60f
        drawContext.canvas.nativeCanvas.drawText("SYSTEM_STATUS: ACTIVE", margin, size.height - margin - 80f, overlayPaint)
        drawContext.canvas.nativeCanvas.drawText("DATA_POINTS: ${pointCount}", margin, size.height - margin - 40f, overlayPaint)
        drawContext.canvas.nativeCanvas.drawText("ZOOM_LEVEL: ${String.format("%.1f", scale)}x", margin, size.height - margin, overlayPaint)
        
        // Circular Scanner Effect
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = size.width / 2f,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = 2f)
        )
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
    rotateY: Float = 0f,
    scale: Float = 1f,
    scanLineProgress: Float = 0f,
    starField: List<StarSpec> = emptyList()
) {
    with(drawScope) {
        val width = size.width
        val height = size.height
        
        // --- 1. Space Background Gradient ---
        drawRect(
            brush = Brush.radialGradient(
                0.0f to Color(0xFF0D0D1A),
                1.0f to Color.Black,
                center = Offset(width / 2f, height / 2f)
            )
        )

        when (artStyle) {
            ArtStyle.GLOBE_HEATMAP -> {
                val baseRadius = minOf(width, height) * 0.45f
                val radius = baseRadius * scale
                val centerX = width / 2f
                val centerY = height / 2f
                
                // --- 2. Rich Star Field ---
                starField.forEach { star ->
                    drawCircle(
                        color = Color.White,
                        radius = star.radius,
                        center = Offset(star.xFactor * width, star.yFactor * height),
                        alpha = star.alpha
                    )
                }

                // --- 3. Atmosphere / Bloom ---
                drawCircle(
                    brush = Brush.radialGradient(
                        0.8f to Color.Transparent,
                        1.0f to palette.colors.first().copy(alpha = 0.35f)
                    ),
                    radius = radius * 1.45f,
                    center = Offset(centerX, centerY)
                )

                // --- 4. The Core Sphere ---
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color(0xFF20204A), // Distinct blue base
                        0.8f to Color(0xFF0A0A15),
                        1.0f to palette.colors.first().copy(alpha = 0.6f)
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )

                // --- 5. High-Detail Landmasses & Country Outlines ---
                val landAlpha = 0.4f * scale.coerceIn(0.5f, 2f)
                val landColor = palette.colors.getOrElse(1) { Color.Cyan }
                val outlineColor = Color.White.copy(alpha = 0.15f * scale.coerceIn(0.5f, 2f))
                
                // Detailed boundary paths (Simplified major landmasses & islands)
                GlobeLandOutlines.forEach { points ->
                    val path = Path()
                    points.forEach { point ->
                        val pos = projectGlobe(point.lat, point.lng, radius, rotateX, rotateY)
                        if (pos.z > 0) {
                            val x = centerX + pos.x; val y = centerY + pos.y
                            if (path.isEmpty) path.moveTo(x, y) else path.lineTo(x, y)
                        } else if (!path.isEmpty) {
                            drawPath(path, landColor, alpha = landAlpha * 0.5f)
                            drawPath(path, outlineColor, style = Stroke(1f))
                            path.reset()
                        }
                    }
                    if (!path.isEmpty) {
                        drawPath(path, landColor, alpha = landAlpha * 0.5f)
                        drawPath(path, outlineColor, style = Stroke(1.5f))
                    }
                }

                // --- 5.1 City Lights (Static Decorative Points) ---
                GlobeCityLights.forEach { city ->
                    val pos = projectGlobe(city.lat, city.lng, radius, rotateX, rotateY)
                    if (pos.z > 0) {
                        val depthScale = (pos.z / radius).coerceIn(0f, 1f)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.3f * depthScale),
                            radius = 1.5f * scale,
                            center = Offset(centerX + pos.x, centerY + pos.y)
                        )
                    }
                }

                // --- 6. Grid Lines ---
                val gridColor = palette.colors.first().copy(alpha = 0.3f)
                for (lat in -80..80 step 20) {
                    val path = Path()
                    for (lng in -180..180 step 10) {
                        val pos = projectGlobe(lat.toDouble(), lng.toDouble(), radius, rotateX, rotateY)
                        if (pos.z > 0) {
                            val x = centerX + pos.x; val y = centerY + pos.y
                            if (path.isEmpty) path.moveTo(x, y) else path.lineTo(x, y)
                        } else if (!path.isEmpty) { drawPath(path, gridColor, style = Stroke(1.2f)); path.reset() }
                    }
                    drawPath(path, gridColor, style = Stroke(1.2f))
                }

                // --- 7. Data Points (Pillars) ---
                if (trackPoints.isNotEmpty()) {
                    val projectedTrackPoints =
                        trackPoints
                            .asSequence()
                            .map { ProjectedTrackPoint(projectGlobe(it.latitude, it.longitude, radius, rotateX, rotateY)) }
                            .filter { it.position.z > 0 }
                            .sortedBy { it.position.z }
                            .toList()

                    projectedTrackPoints.forEach { projected ->
                        val pos = projected.position
                        val x = centerX + pos.x
                        val y = centerY + pos.y
                        val depthScale = (pos.z / radius).coerceIn(0f, 1f)
                        val pillarHeight = (20f + 120f * density) * scale * depthScale

                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(palette.colors.last(), Color.Transparent),
                                startY = y, endY = y - pillarHeight
                            ),
                            start = Offset(x, y),
                            end = Offset(x, y - pillarHeight),
                            strokeWidth = 5f * scale * depthScale,
                            cap = StrokeCap.Round,
                            blendMode = BlendMode.Plus
                        )
                        drawCircle(
                            brush = Brush.radialGradient(0f to palette.colors.last().copy(alpha = 0.8f), 1f to Color.Transparent),
                            radius = 15f * scale * depthScale * density,
                            center = Offset(x, y),
                            blendMode = BlendMode.Plus
                        )
                    }
                }

                // --- 8. Tech Scan Line ---
                val scanLineY = scanLineProgress * height
                drawLine(palette.colors.first().copy(alpha = 0.2f), Offset(0f, scanLineY), Offset(width, scanLineY), 4f)
            }
            else -> {
                if (trackPoints.isEmpty()) return
                // ... (Keep existing 2D art styles)
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
        rotY: Float = 0f,
        scale: Float = 1f
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
        ) { drawArt(this, points, artStyle, palette, density, seed, size, rotX, rotY, scale) }
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
