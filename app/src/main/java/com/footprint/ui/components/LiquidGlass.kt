package com.footprint.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(32.dp),
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Lower alpha for more translucency, letting the blur and noise show through.
    val surfaceColor = if (isDark) {
        Color.Black.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.3f)
    }

    // A more complex border that simulates a sharp highlight and Fresnel effect.
    val borderBrush = remember(isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDark) 0.4f else 0.8f), // Sharp top-left highlight
                Color.White.copy(alpha = if (isDark) 0.05f else 0.1f), // Mid-gradient
                Color.Transparent,
                Color.White.copy(alpha = 0.05f) // Subtle bottom-right highlight
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "WaterDrop")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .hazeChild(hazeState, shape = shape)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.3f),
                ambientColor = Color.Transparent
            )
            .background(surfaceColor, shape)
            .border(
                width = 1.5.dp, // Thicker border for a more pronounced effect
                brush = borderBrush,
                shape = shape
            )
            .clip(shape)
    ) {
        // Inner shine from the top, adding depth.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.1f else 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Inner content
        content()

        // Add the water drop visual effect
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val radius = size.minDimension * 0.15f
                    val x = (size.width / 2) + (size.width / 3) * cos(angle)
                    val y = (size.height / 2) + (size.height / 3) * sin(angle)

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent,
                            ),
                            center = Offset(x, y),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(x, y)
                    )
                }
        )
    }
}
