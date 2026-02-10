package com.footprint.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.footprint.ui.effects.GlassCard
import com.footprint.ui.effects.LiquidDrop
import com.footprint.ui.effects.MetaballBox
import com.footprint.ui.theme.LocalHazeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

@Composable
fun GlassmorphismEffectScreen() {
        // We expect LocalHazeState to be provided by Activity/Root, but we fallback safely just in
        // case
        // for Preview
        val hazeState = LocalHazeState.current

        // Scrollable container
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(Color(0xFF1E1E2E)) // Dark background to make glass pop
                                .haze(hazeState) // Apply haze source to the background container
        ) {
                // --- Background Pattern (to demonstrate blur) ---
                BackgroundBlobs()

                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                                text = "Liquid Glass UI",
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.White
                        )

                        // --- SECTION A: GLASSMORPHISM ---
                        Text(
                                text = "A. Glass Look",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.Start)
                        )

                        GlassCard(
                                modifier = Modifier.fillMaxWidth().height(220.dp),
                                hazeState = hazeState,
                                noiseOpacity = 0.03f, // Subtle noise
                                borderColor = Color.White.copy(alpha = 0.4f)
                        ) {
                                Column(
                                        modifier = Modifier.fillMaxSize().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                ) {
                                        Text(
                                                text = "Premium Glass",
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                                text =
                                                        "Real-time Blur • Noise Texture • Specular Border",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Box(
                                                modifier =
                                                        Modifier.size(60.dp)
                                                                .background(
                                                                        Color.White.copy(
                                                                                alpha = 0.1f
                                                                        ),
                                                                        CircleShape
                                                                )
                                                                .alpha(0.5f)
                                        )
                                }
                        }

                        // --- SECTION B: LIQUID MOTION ---
                        Text(
                                text = "B. Liquid Motion (Metaballs)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.align(Alignment.Start)
                        )

                        // Container for liquid effect
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(250.dp)
                                                .background(
                                                        Color.Black.copy(alpha = 0.3f),
                                                        MaterialTheme.shapes.medium
                                                ),
                                contentAlignment = Alignment.Center
                        ) { MetaballAnimation() }

                        Spacer(modifier = Modifier.height(40.dp))
                }
        }
}

@Composable
fun BackgroundBlobs() {
        val infiniteTransition = rememberInfiniteTransition(label = "blobs")

        val offset1 by
                infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(8000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "offset1"
                )

        Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Blob 1: Vibrant Purple/Pink (Moving)
                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors = listOf(Color(0xFFD300C5), Color.Transparent),
                                        center =
                                                Offset(
                                                        width * 0.2f + (width * 0.5f * offset1),
                                                        height * 0.3f
                                                ),
                                        radius = 500f
                                ),
                        radius = 500f,
                        center = Offset(width * 0.2f + (width * 0.5f * offset1), height * 0.3f)
                )

                // Blob 2: Cyan/Blue (Moving opposite)
                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors = listOf(Color(0xFF00C2FF), Color.Transparent),
                                        center =
                                                Offset(
                                                        width * 0.8f - (width * 0.3f * offset1),
                                                        height * 0.6f + (height * 0.1f * offset1)
                                                ),
                                        radius = 450f
                                ),
                        radius = 450f,
                        center =
                                Offset(
                                        width * 0.8f - (width * 0.3f * offset1),
                                        height * 0.6f + (height * 0.1f * offset1)
                                )
                )

                // Blob 3: Deep Blue (Static anchor)
                drawCircle(
                        brush =
                                Brush.radialGradient(
                                        colors =
                                                listOf(
                                                        Color(0xFF3B27BA).copy(alpha = 0.6f),
                                                        Color.Transparent
                                                ),
                                        center = Offset(width * 0.5f, height * 0.9f),
                                        radius = 600f
                                ),
                        radius = 600f,
                        center = Offset(width * 0.5f, height * 0.9f)
                )
        }
}

@Composable
fun MetaballAnimation() {
        val infiniteTransition = rememberInfiniteTransition(label = "liquid")

        // Animate position of two drops
        val moveX by
                infiniteTransition.animateFloat(
                        initialValue = -80f,
                        targetValue = 80f,
                        animationSpec =
                                infiniteRepeatable(
                                        animation = tween(2000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                ),
                        label = "moveX"
                )

        MetaballBox(
                modifier = Modifier.fillMaxSize(),
                blurRadius = 40f
                // alphaCutoff is handled internally in our custom implementation
                ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // Static Drop in Center
                        LiquidDrop(size = 100.dp, color = Color.Cyan)

                        // Moving Drop
                        LiquidDrop(
                                modifier =
                                        Modifier.offset(
                                                x = moveX.dp,
                                                y = (moveX / 2).dp
                                        ), // Diagonal movement
                                size = 80.dp,
                                color = Color.Cyan
                        )

                        // Another Moving Drop
                        LiquidDrop(
                                modifier = Modifier.offset(x = (-moveX).dp, y = (-moveX / 3).dp),
                                size = 60.dp,
                                color = Color.Cyan
                        )
                }
        }
}

@Preview
@Composable
fun GlassPreview() {
        val hazeState = remember { HazeState() }
        CompositionLocalProvider(LocalHazeState provides hazeState) { GlassmorphismEffectScreen() }
}
