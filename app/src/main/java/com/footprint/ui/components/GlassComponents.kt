package com.footprint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassMorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    blurRadius: Dp = 16.dp, // 保留参数兼容性
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    val surfaceColor = if (isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.7f)
    }

    val bottomSurfaceColor = if (isDark) {
        Color.White.copy(alpha = 0.04f)
    } else {
        Color.White.copy(alpha = 0.4f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 0.dp else 8.dp,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.1f),
                ambientColor = Color.Black.copy(alpha = 0.1f)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(surfaceColor, bottomSurfaceColor)
                ),
                shape = shape
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        borderColor,
                        borderColor.copy(alpha = 0.2f)
                    )
                ),
                shape = shape
            )
            .clip(shape)
    ) {
        content()
    }
}