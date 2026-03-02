package com.footprint.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.footprint.FootprintViewModel
import com.footprint.data.local.TrackPointEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerativeArtScreen(viewModel: FootprintViewModel, onBack: () -> Unit) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var artStyle by remember { mutableStateOf(ArtStyle.LIQUID_NEON) }

        // Get all track points (or a sample)
        val trackPoints by
                viewModel.getHeatmapPoints().collectAsStateWithLifecycle(initialValue = emptyList())

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("足迹生成艺术", fontWeight = FontWeight.Bold) },
                                navigationIcon = {
                                        IconButton(onClick = onBack) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                        }
                                },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor = Color.Transparent
                                        )
                        )
                },
                containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
                Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                        // Art Preview Area
                        Box(
                                modifier =
                                        Modifier.weight(1f)
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(Color.Black)
                        ) {
                                if (trackPoints.isNotEmpty()) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawArt(this, trackPoints, artStyle, size)
                                        }
                                } else {
                                        Box(
                                                Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                        ) { Text("暂无足迹数据", color = Color.White) }
                                }
                        }

                        // Controls
                        Card(
                                modifier =
                                        Modifier.padding(16.dp)
                                                .fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                colors =
                                        CardDefaults.cardColors(
                                                containerColor =
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                                .copy(alpha = 0.8f)
                                        ),
                                border =
                                        BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(
                                                        alpha = 0.5f
                                                )
                                        )
                        ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                        Text("艺术风格", style = MaterialTheme.typography.labelLarge)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                ArtStyle.entries.forEach { style ->
                                                        FilterChip(
                                                                selected = artStyle == style,
                                                                onClick = { artStyle = style },
                                                                label = { Text(style.label) }
                                                        )
                                                }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                                onClick = {
                                                        scope.launch {
                                                                try {
                                                                        val bitmap =
                                                                                captureArtToBitmap(
                                                                                        context,
                                                                                        trackPoints,
                                                                                        artStyle
                                                                                )
                                                                        saveAndShareArt(
                                                                                context,
                                                                                bitmap
                                                                        )
                                                                } catch (e: Exception) {
                                                                        android.widget.Toast
                                                                                .makeText(
                                                                                        context,
                                                                                        "导出失败: ${e.message}",
                                                                                        android.widget
                                                                                                .Toast
                                                                                                .LENGTH_SHORT
                                                                                )
                                                                                .show()
                                                                }
                                                        }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                        ) {
                                                Icon(Icons.Default.Share, null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("导出并分享")
                                        }
                                }
                        }
                }
        }
}

enum class ArtStyle(val label: String) {
        LIQUID_NEON("液态霓虹"),
        INK_SPLASH("水墨流体"),
        GALAXY_DUST("星尘粒子")
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

                if (latRange > 0 && lngRange > 0) {
                        val scaleX = width / lngRange
                        val scaleY = height / latRange
                        val scale = minOf(scaleX, scaleY) * 0.8f

                        val offsetX = (width - lngRange * scale) / 2
                        val offsetY = (height - latRange * scale) / 2

                        val path = Path()
                        trackPoints.forEachIndexed { index, pt ->
                                val x = ((pt.longitude - minLng) * scale + offsetX).toFloat()
                                val y =
                                        (height - ((pt.latitude - minLat) * scale + offsetY))
                                                .toFloat()
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        when (style) {
                                ArtStyle.LIQUID_NEON -> {
                                        drawPath(
                                                path = path,
                                                brush =
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color(0xFF00FFCC),
                                                                                Color(0xFF3300FF),
                                                                                Color(0xFFFF0099)
                                                                        )
                                                        ),
                                                style =
                                                        androidx.compose.ui.graphics.drawscope
                                                                .Stroke(
                                                                        width = 40f,
                                                                        cap = StrokeCap.Round,
                                                                        join = StrokeJoin.Round
                                                                ),
                                                alpha = 0.6f
                                        )
                                        drawPath(
                                                path = path,
                                                color = Color.White,
                                                style =
                                                        androidx.compose.ui.graphics.drawscope
                                                                .Stroke(width = 2f)
                                        )
                                }
                                ArtStyle.INK_SPLASH -> {
                                        drawPath(
                                                path = path,
                                                color = Color.Black,
                                                style =
                                                        androidx.compose.ui.graphics.drawscope
                                                                .Stroke(
                                                                        width = 15f,
                                                                        cap = StrokeCap.Round
                                                                ),
                                                alpha = 0.8f
                                        )
                                }
                                ArtStyle.GALAXY_DUST -> {
                                        trackPoints.forEach { pt ->
                                                val x =
                                                        ((pt.longitude - minLng) * scale + offsetX)
                                                                .toFloat()
                                                val y =
                                                        (height -
                                                                        ((pt.latitude - minLat) *
                                                                                scale + offsetY))
                                                                .toFloat()
                                                drawCircle(
                                                        brush =
                                                                Brush.radialGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        Color.White,
                                                                                        Color.Transparent
                                                                                )
                                                                ),
                                                        radius = (5..15).random().toFloat(),
                                                        center = Offset(x, y),
                                                        alpha = 0.7f
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
