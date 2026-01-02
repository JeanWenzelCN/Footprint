package com.footprint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    val backgroundColors = if (isDark) {
        listOf(
            Color(0xFF0D1117), // 深色背景 1
            Color(0xFF161B22), // 深色背景 2
            Color(0xFF010409)  // 深色背景 3
        )
    } else {
        listOf(
            Color(0xFFE0F7FA), // 淡青色
            Color(0xFFE1F5FE), // 淡蓝色
            Color(0xFFF3E5F5)  // 淡紫色
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = backgroundColors,
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // 叠加一个径向渐变以增加层次感
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(0.5f, 0.3f), // 偏上的光源
                        radius = 1000f
                    )
                )
        )
        content()
    }
}