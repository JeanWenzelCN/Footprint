package com.footprint.ui.screens

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.footprint.FootprintViewModel
import com.footprint.ui.components.LiquidGlassCard
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

        var seed by remember { mutableStateOf(0) } // For randomizing strokes

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
                                                .background(Color.Black) // Art canvas background
                        ) {
                                if (trackPoints.isNotEmpty()) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                                // Drawing Logic
                                                val width = size.width
                                                val height = size.height

                                                // Auto-scale to fit
                                                val minLat = trackPoints.minOf { it.latitude }
                                                val maxLat = trackPoints.maxOf { it.latitude }
                                                val minLng = trackPoints.minOf { it.longitude }
                                                val maxLng = trackPoints.maxOf { it.longitude }

                                                val latRange = maxLat - minLat
                                                val lngRange = maxLng - minLng

                                                if (latRange > 0 && lngRange > 0) {
                                                        val scaleX = width / lngRange
                                                        val scaleY = height / latRange
                                                        val scale =
                                                                minOf(scaleX, scaleY) *
                                                                        0.8f // Padding

                                                        val offsetX = (width - lngRange * scale) / 2
                                                        val offsetY =
                                                                (height - latRange * scale) / 2

                                                        val path = Path()
                                                        trackPoints.forEachIndexed { index, pt ->
                                                                val x =
                                                                        ((pt.longitude - minLng) *
                                                                                        scale +
                                                                                        offsetX)
                                                                                .toFloat()
                                                                val y =
                                                                        (height -
                                                                                        ((pt.latitude -
                                                                                                minLat) *
                                                                                                scale +
                                                                                                offsetY))
                                                                                .toFloat() // Y flip
                                                                if (index == 0) path.moveTo(x, y)
                                                                else path.lineTo(x, y)
                                                        }

                                                        // Style Application
                                                        when (artStyle) {
                                                                ArtStyle.LIQUID_NEON -> {
                                                                        drawPath(
                                                                                path = path,
                                                                                brush =
                                                                                        Brush.linearGradient(
                                                                                                colors =
                                                                                                        listOf(
                                                                                                                Color(
                                                                                                                        0xFF00FFCC
                                                                                                                ),
                                                                                                                Color(
                                                                                                                        0xFF3300FF
                                                                                                                ),
                                                                                                                Color(
                                                                                                                        0xFFFF0099
                                                                                                                )
                                                                                                        )
                                                                                        ),
                                                                                style =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .drawscope
                                                                                                .Stroke(
                                                                                                        width =
                                                                                                                40f,
                                                                                                        cap =
                                                                                                                StrokeCap
                                                                                                                        .Round,
                                                                                                        join =
                                                                                                                StrokeJoin
                                                                                                                        .Round
                                                                                                ),
                                                                                alpha = 0.6f
                                                                        )
                                                                        drawPath(
                                                                                path = path,
                                                                                color = Color.White,
                                                                                style =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .drawscope
                                                                                                .Stroke(
                                                                                                        width =
                                                                                                                2f
                                                                                                )
                                                                        )
                                                                }
                                                                ArtStyle.INK_SPLASH -> {
                                                                        drawPath(
                                                                                path = path,
                                                                                color = Color.Black,
                                                                                style =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .drawscope
                                                                                                .Stroke(
                                                                                                        width =
                                                                                                                15f,
                                                                                                        cap =
                                                                                                                StrokeCap
                                                                                                                        .Round
                                                                                                ),
                                                                                alpha = 0.8f
                                                                        )
                                                                }
                                                                ArtStyle.GALAXY_DUST -> {
                                                                        // Dots instead of line
                                                                        trackPoints.forEach { pt ->
                                                                                val x =
                                                                                        ((pt.longitude -
                                                                                                        minLng) *
                                                                                                        scale +
                                                                                                        offsetX)
                                                                                                .toFloat()
                                                                                val y =
                                                                                        (height -
                                                                                                        ((pt.latitude -
                                                                                                                minLat) *
                                                                                                                scale +
                                                                                                                offsetY))
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
                                                                                        radius =
                                                                                                (5..15).random()
                                                                                                        .toFloat(),
                                                                                        center =
                                                                                                Offset(
                                                                                                        x,
                                                                                                        y
                                                                                                ),
                                                                                        alpha = 0.7f
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                } else {
                                        Box(
                                                Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                        ) { Text("暂无足迹数据", color = Color.White) }
                                }
                        }

                        // Controls
                        LiquidGlassCard(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                shape = RoundedCornerShape(24.dp)
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
                                                                // Simulation of high-res generation
                                                                // and sharing
                                                                android.widget.Toast.makeText(
                                                                                context,
                                                                                "正在生成 4K 艺术图...",
                                                                                android.widget.Toast
                                                                                        .LENGTH_SHORT
                                                                        )
                                                                        .show()
                                                                kotlinx.coroutines.delay(1500)
                                                                android.widget.Toast.makeText(
                                                                                context,
                                                                                "已保存到相册并调起分享",
                                                                                android.widget.Toast
                                                                                        .LENGTH_SHORT
                                                                        )
                                                                        .show()
                                                                // In a real implementation, we
                                                                // would capture the Canvas to a
                                                                // Bitmap here.
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
