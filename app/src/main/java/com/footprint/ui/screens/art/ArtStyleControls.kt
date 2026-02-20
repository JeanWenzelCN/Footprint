package com.footprint.ui.screens.art

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ArtStyleControls(
        lineWeight: Float,
        onLineWeightChange: (Float) -> Unit,
        glowRadius: Float,
        onGlowRadiusChange: (Float) -> Unit,
        mapStyle: ArtMapStyle,
        onMapStyleChange: (ArtMapStyle) -> Unit,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
        onStartDateClick: () -> Unit,
        onEndDateClick: () -> Unit,
        layout: ArtLayout,
        onLayoutChange: (ArtLayout) -> Unit,
        textColor: String,
        onTextColorChange: (String) -> Unit,
        isItalic: Boolean,
        onItalicChange: (Boolean) -> Unit,
        hasBorder: Boolean,
        onBorderChange: (Boolean) -> Unit,
        accentColor: Color
) {
    com.footprint.ui.components.LiquidGlassCard(
            backgroundColor = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
    ) {
        Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                    "足迹艺术工坊",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
            )

            // Map Style Selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtMapStyle.entries.forEach { style ->
                    FilterChip(
                            selected = mapStyle == style,
                            onClick = { onMapStyleChange(style) },
                            label = {
                                Text(
                                        when (style) {
                                            ArtMapStyle.DARK -> "暗夜"
                                            ArtMapStyle.LIGHT -> "明亮"
                                            ArtMapStyle.SATELLITE -> "卫星"
                                            ArtMapStyle.VOID -> "虚空"
                                        }
                                )
                            },
                            colors =
                                    FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accentColor,
                                            labelColor = Color.White,
                                            selectedLabelColor = Color.Black
                                    )
                    )
                }
            }

            // Time Scope
            Column {
                Text("时间范围", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    TextButton(onClick = onStartDateClick) {
                        Text(startDate.toString(), color = accentColor)
                    }
                    Text("-", color = Color.Gray)
                    TextButton(onClick = onEndDateClick) {
                        Text(endDate.toString(), color = accentColor)
                    }
                }
            }

            // Layout Selector
            Column {
                Text("海报布局", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArtLayout.entries.forEach { style ->
                        FilterChip(
                                selected = layout == style,
                                onClick = { onLayoutChange(style) },
                                label = {
                                    Text(
                                            when (style) {
                                                ArtLayout.FULLCREEN_A24 -> "电影质感"
                                                ArtLayout.POLAROID -> "拍立得"
                                                ArtLayout.GEEK_STATS -> "极客数据"
                                            }
                                    )
                                },
                                colors =
                                        FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = accentColor,
                                                labelColor = Color.White,
                                                selectedLabelColor = Color.Black
                                        )
                        )
                    }
                }
            }

            // Typography modifiers
            Column {
                Text("字体颜色", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colors = listOf("White", "Black", "Gold", "Deep Blue")
                    colors.forEach { cName ->
                        FilterChip(
                                selected = textColor == cName,
                                onClick = { onTextColorChange(cName) },
                                label = { Text(cName) },
                                colors =
                                        FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = accentColor,
                                                labelColor = Color.White,
                                                selectedLabelColor = Color.Black
                                        )
                        )
                    }
                }
            }

            Column {
                Text("字体效果", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                            selected = isItalic,
                            onClick = { onItalicChange(!isItalic) },
                            label = { Text("斜体 (Italic)") },
                            colors =
                                    FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accentColor,
                                            labelColor = Color.White,
                                            selectedLabelColor = Color.Black
                                    )
                    )
                    FilterChip(
                            selected = hasBorder,
                            onClick = { onBorderChange(!hasBorder) },
                            label = { Text("黑边 (Black Border)") },
                            colors =
                                    FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = accentColor,
                                            labelColor = Color.White,
                                            selectedLabelColor = Color.Black
                                    )
                    )
                }
            }

            // Sliders
            Column {
                Text("线条粗细", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Slider(
                        value = lineWeight,
                        onValueChange = onLineWeightChange,
                        valueRange = 1f..30f,
                        colors =
                                SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor
                                )
                )
            }

            Column {
                Text("霓虹光晕", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                Slider(
                        value = glowRadius,
                        onValueChange = onGlowRadiusChange,
                        valueRange = 0f..50f,
                        colors =
                                SliderDefaults.colors(
                                        thumbColor = accentColor,
                                        activeTrackColor = accentColor
                                )
                )
            }
        }
    }
}
