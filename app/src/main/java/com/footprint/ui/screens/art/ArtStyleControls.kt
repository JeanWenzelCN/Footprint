package com.footprint.ui.screens.art

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check

@Composable
fun ArtStyleControls(
        artName: String,
        onArtNameChange: (String) -> Unit,
        fontName: String,
        onFontNameChange: (String) -> Unit,
        coreColorName: String,
        onCoreColorNameChange: (String) -> Unit,
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
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.padding(20.dp).verticalScroll(scrollState), 
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
                "足迹艺术工坊",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

        // Art Title
        Column {
            Text("艺术作品标题", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            OutlinedTextField(
                    value = artName,
                    onValueChange = onArtNameChange,
                    singleLine = true,
                    colors =
                            OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = Color.Gray
                            ),
                    modifier = Modifier.fillMaxWidth()
            )
        }

        // Core Color
        Column {
            Text("核心色调", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 8.dp)
            ) {
                val coreColors =
                        listOf(
                                "Deep Blue" to Color(0xFF007AFF),
                                "Cyber Pink" to Color(0xFFFF2D55),
                                "Neon Green" to Color(0xFF00FF9F),
                                "Gold" to Color(0xFFFFCC00)
                        )
                coreColors.forEach { (cName, cValue) ->
                    val selected = coreColorName == cName
                    Box(
                            modifier =
                                    Modifier.size(40.dp)
                                            .clip(CircleShape)
                                            .background(cValue)
                                            .clickable { onCoreColorNameChange(cName) }
                                            .padding(2.dp)
                    ) {
                        if (selected) {
                            Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (cName == "Gold") Color.Black else Color.White,
                                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                            )
                        }
                    }
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
                val colors =
                        listOf(
                                "White" to "白色",
                                "Black" to "黑色",
                                "Gold" to "金色",
                                "Deep Blue" to "深蓝"
                        )
                colors.forEach { (cName, cLabel) ->
                    FilterChip(
                            selected = textColor == cName,
                            onClick = { onTextColorChange(cName) },
                            label = { Text(cLabel) },
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

        // Calligraphy Font
        Column {
            Text("书法字体", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(9) { index ->
                    val fonts =
                            listOf(
                                    "Default",
                                    "MaShanZheng",
                                    "ZhiMangXing",
                                    "LongCang",
                                    "Serif",
                                    "Monospace",
                                    "Cursive",
                                    "LiuJianMaoCao",
                                    "ZCOOLXiaoWei"
                            )
                    val f = fonts[index]
                    val fontFamily =
                            androidx.compose.runtime.remember(f) {
                                when (f) {
                                    "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                                    "Monospace" ->
                                            androidx.compose.ui.text.font.FontFamily.Monospace
                                    "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
                                    "MaShanZheng" ->
                                            androidx.compose.ui.text.font.FontFamily(
                                                    androidx.compose.ui.text.font.Font(
                                                            com.footprint.R.font.ma_shan_zheng,
                                                            FontWeight.Normal
                                                    )
                                            )
                                    "ZhiMangXing" ->
                                            androidx.compose.ui.text.font.FontFamily(
                                                    androidx.compose.ui.text.font.Font(
                                                            com.footprint.R.font.zhi_mang_xing,
                                                            FontWeight.Normal
                                                    )
                                            )
                                    "LongCang" ->
                                            androidx.compose.ui.text.font.FontFamily(
                                                    androidx.compose.ui.text.font.Font(
                                                            com.footprint.R.font.long_cang,
                                                            FontWeight.Normal
                                                    )
                                            )
                                    "LiuJianMaoCao" ->
                                            androidx.compose.ui.text.font.FontFamily(
                                                    androidx.compose.ui.text.font.Font(
                                                            com.footprint.R.font.liu_jian_mao_cao,
                                                            FontWeight.Normal
                                                    )
                                            )
                                    "ZCOOLXiaoWei" ->
                                            androidx.compose.ui.text.font.FontFamily(
                                                    androidx.compose.ui.text.font.Font(
                                                            com.footprint.R.font.zcool_xiao_wei,
                                                            FontWeight.Normal
                                                    )
                                            )
                                    else -> androidx.compose.ui.text.font.FontFamily.Default
                                }
                            }
                    FilterChip(
                            selected = fontName == f,
                            onClick = { onFontNameChange(f) },
                            label = {
                                Text(if (f == "Default") "默认" else f, fontFamily = fontFamily)
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
