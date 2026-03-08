package com.footprint.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import com.footprint.R
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.footprint.FootprintViewModel
import com.footprint.data.local.TrackPointEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerativeArtScreen(viewModel: FootprintViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var artStyle by remember { mutableStateOf(ArtStyle.LIQUID_NEON) }
    var timeFilter by remember { mutableFloatStateOf(1f) } // 0.0 to 1.0

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
            TopAppBar(
                title = {
                    Column {
                        Text("足迹工坊 · 时空热力", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(displayDateRange, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Refresh or randomize seed */ }) {
                        Icon(Icons.Default.Refresh, "Randomize")
                    }
                }
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
            ) {
                if (filteredPoints.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArt(this, filteredPoints, artStyle, size)
                        
                        // Overlay decorative text
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 40f
                            alpha = 100
                            isFakeBoldText = true
                        }
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            "FOOTPRINT ART // ${artStyle.label}",
                            40f, size.height - 60f, paint
                        )
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ArtStyle.entries.forEach { style ->
                            val isSelected = artStyle == style
                            Surface(
                                selected = isSelected,
                                onClick = { artStyle = style },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.weight(1f)
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
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val bitmap = captureArtToBitmap(context, filteredPoints, artStyle)
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

enum class ArtStyle(val label: String) {
    LIQUID_NEON("霓虹流光"),
    HEATMAP("时空格点"),
    TEMPORAL_FLOW("时光幻影"),
    GALAXY_DUST("极星尘埃")
}

private fun drawArt(
    drawScope: DrawScope,
    trackPoints: List<TrackPointEntity>,
    style: ArtStyle,
    size: Size
) {
    with(drawScope) {
        val width = size.width
        val height = size.height

        if (trackPoints.isEmpty()) return

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
            minOf(displayWidth / lngRange, displayHeight / latRange)
        } else 1.0

        val offsetX = (width - lngRange * scale) / 2
        val offsetY = (height - latRange * scale) / 2

        fun projectX(lng: Double) = ((lng - minLng) * scale + offsetX).toFloat()
        fun projectY(lat: Double) = (height - ((lat - minLat) * scale + offsetY)).toFloat()

        when (style) {
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
                        colors = listOf(Color(0xFF00E5FF), Color(0xFF651FFF), Color(0xFFFF4081)),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    style = Stroke(width = 24f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    alpha = 0.5f
                )
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                    alpha = 0.8f
                )
            }
            ArtStyle.HEATMAP -> {
                // Actual Heatmap look: glowing radial circles that stack
                trackPoints.forEach { pt ->
                    val x = projectX(pt.longitude)
                    val y = projectY(pt.latitude)
                    drawCircle(
                        brush = Brush.radialGradient(
                            0.0f to Color(0xFFFF5722).copy(alpha = 0.2f),
                            0.5f to Color(0xFFFFC107).copy(alpha = 0.05f),
                            1.0f to Color.Transparent
                        ),
                        radius = 60f,
                        center = Offset(x, y),
                        blendMode = BlendMode.Screen
                    )
                }
            }
            ArtStyle.TEMPORAL_FLOW -> {
                // Time-aware trails with alpha decay
                val total = trackPoints.size
                trackPoints.forEachIndexed { index, pt ->
                    val x = projectX(pt.longitude)
                    val y = projectY(pt.latitude)
                    val progress = index.toFloat() / total
                    val color = Color.hsv(progress * 360f, 0.7f, 1f)
                    
                    if (index > 0) {
                        val prev = trackPoints[index - 1]
                        drawLine(
                            color = color,
                            start = Offset(projectX(prev.longitude), projectY(prev.latitude)),
                            end = Offset(x, y),
                            strokeWidth = 2f + progress * 20f,
                            cap = StrokeCap.Round,
                            alpha = progress * 0.7f
                        )
                    }
                }
            }
            ArtStyle.GALAXY_DUST -> {
                val rand = Random(42)
                trackPoints.forEach { pt ->
                    val x = projectX(pt.longitude)
                    val y = projectY(pt.latitude)
                    val pSize = (2..12).random(rand).toFloat()
                    
                    drawCircle(
                        color = Color.White,
                        radius = pSize / 2,
                        center = Offset(x, y),
                        alpha = rand.nextFloat() * (0.9f - 0.3f) + 0.3f
                    )
                    
                    if (rand.nextFloat() > 0.95f) {
                        // Rare "star burst"
                        val glowSize = pSize * 5f
                        drawCircle(
                            brush = Brush.radialGradient(
                                0f to Color.White.copy(alpha = 0.4f),
                                1f to Color.Transparent
                            ),
                            radius = glowSize,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

private fun captureArtToBitmap(
        context: android.content.Context,
        points: List<TrackPointEntity>,
        style: ArtStyle
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
        ) { drawArt(this, points, style, size) }
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
