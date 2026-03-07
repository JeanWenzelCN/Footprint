package com.footprint.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.footprint.ui.effects.GlassCard
import com.footprint.ui.effects.MetaballBox
import com.footprint.ui.effects.noiseTexture
import com.footprint.ui.theme.LocalHazeState

/**
 * Compatibility wrapper for the old LiquidGlassCard.
 * Maps to the new GlassCard implementation.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = Color.Unspecified,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.White.copy(alpha = 0.5f),
    noiseOpacity: Float = 0.05f,
    content: @Composable BoxScope.() -> Unit
) {
    // LocalHazeState is guaranteed to be present (has default value)
    val hazeState = LocalHazeState.current

    val effectiveBackgroundColor = if (backgroundColor == Color.Unspecified) {
        // Adapt tint to theme: Light -> White/Surface (Alpha), Dark -> Black/Surface (Alpha)
        // Using surface color ensures it blends with the app theme
        androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.3f) 
    } else {
        backgroundColor
    }

    GlassCard(
        modifier = modifier,
        hazeState = hazeState,
        shape = shape,
        backgroundColor = effectiveBackgroundColor,
        borderWidth = borderWidth,
        borderColor = borderColor,
        noiseOpacity = noiseOpacity,
        content = content
    )
}

/**
 * Compatibility alias for noiseGrain -> noiseTexture
 */
fun Modifier.noiseGrain(opacity: Float = 0.05f): Modifier = this.noiseTexture(opacity)

/**
 * Compatibility wrapper for MetaballAnimation.
 * Maps to MetaballBox.
 */
@Composable
fun MetaballAnimation(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    MetaballBox(
        modifier = modifier,
        content = content
    )
}
