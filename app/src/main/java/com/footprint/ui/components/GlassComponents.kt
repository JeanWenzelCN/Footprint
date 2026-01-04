package com.footprint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassMorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp), // Slightly more rounded for "liquid" look
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    // iOS 26 "Liquid Glass" Style
    // Ultra-high transparency with subtle refraction borders
    val surfaceColor = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.50f)
    } else {
        Color.White.copy(alpha = 0.60f)
    }

    // Refraction border gradient - mimics light catching the edges
    val borderBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f), // Top-Left Highlight
                    Color.White.copy(alpha = 0.05f), // Middle
                    Color.White.copy(alpha = 0.02f)  // Bottom-Right Shadow
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    Color.White.copy(alpha = 0.3f),
                    Color.White.copy(alpha = 0.1f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        }
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 16.dp else 12.dp,
                shape = shape,
                spotColor = if (isDark) Color.Black else Color(0xFF8E8E93).copy(alpha = 0.4f),
                ambientColor = if (isDark) Color.Black else Color(0xFF8E8E93).copy(alpha = 0.2f)
            )
            .background(
                color = surfaceColor,
                shape = shape
            )
            .border(
                width = 1.dp, 
                brush = borderBrush,
                shape = shape
            )
            .clip(shape)
    ) {
        // Inner "Liquid" Shine/Noise Texture Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if(isDark) listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        ) else listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
        )
        content()
    }
}

/**
 * 仿 Telegram 风格的列表项容器
 */
@Composable
fun TelegramListItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .then(clickableModifier)
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        content()
    }
}