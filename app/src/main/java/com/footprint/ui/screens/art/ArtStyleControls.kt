package com.footprint.ui.screens.art

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
        accentColor: Color,
        polaroidFrameStyle: String = "CLASSIC_WHITE",
        onPolaroidFrameStyleChange: (String) -> Unit = {},
        polaroidFramePadding: Float = 0.5f,
        onPolaroidFramePaddingChange: (Float) -> Unit = {},
        polaroidInnerBorder: Float = 1f,
        onPolaroidInnerBorderChange: (Float) -> Unit = {},
        woodType: WoodType = WoodType.ASH,
        onWoodTypeChange: (WoodType) -> Unit = {},
        engravingDepth: Float = 0.5f,
        onEngravingDepthChange: (Float) -> Unit = {},
        canvasGrain: Float = 0.3f,
        onCanvasGrainChange: (Float) -> Unit = {},
        armorType: ArmorType = ArmorType.GUNMETAL,
        onArmorTypeChange: (ArmorType) -> Unit = {},
        mechanicalSeams: Float = 0.5f,
        onMechanicalSeamsChange: (Float) -> Unit = {},
        hasHazardStriping: Boolean = false,
        onHasHazardStripingChange: (Boolean) -> Unit = {},
) {
        val scrollState = rememberScrollState()
        Column(
                modifier = Modifier.padding(20.dp).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                Text(
                        "足迹艺术工坊",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
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
                                                        selectedLabelColor =
                                                                MaterialTheme.colorScheme.onPrimary
                                                )
                                )
                        }
                }

                // Time Scope
                Column {
                        Text(
                                "时间范围",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                        )
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                                val dateFormatter =
                                        java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")
                                TextButton(onClick = onStartDateClick) {
                                        Text(startDate.format(dateFormatter), color = accentColor)
                                }
                                Text("-", color = Color.Gray)
                                TextButton(onClick = onEndDateClick) {
                                        Text(endDate.format(dateFormatter), color = accentColor)
                                }
                        }
                }

                // Art Title
                Column {
                        Text(
                                "艺术作品标题",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                        )
                        OutlinedTextField(
                                value = artName,
                                onValueChange = onArtNameChange,
                                singleLine = true,
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedTextColor =
                                                        MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor =
                                                        MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor =
                                                        MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor =
                                                        MaterialTheme.colorScheme.outline
                                        ),
                                modifier = Modifier.fillMaxWidth()
                        )
                }

                // Core Color
                Column {
                        Text(
                                "核心色调",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                        )
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
                                                                .clickable {
                                                                        onCoreColorNameChange(cName)
                                                                }
                                                                .padding(2.dp),
                                                contentAlignment =
                                                        androidx.compose.ui.Alignment.Center
                                        ) {
                                                if (selected) {
                                                        Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint =
                                                                        if (cValue.toArgb() ==
                                                                                        Color(
                                                                                                        0xFFFFCC00
                                                                                                )
                                                                                                .toArgb()
                                                                        )
                                                                                Color.Black
                                                                        else Color.White,
                                                        )
                                                }
                                        }
                                }
                        }
                }

                // Layout Selector
                Column {
                        Text(
                                "海报布局",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ArtLayout.entries.forEach { style ->
                                        FilterChip(
                                                selected = layout == style,
                                                onClick = { onLayoutChange(style) },
                                                label = {
                                                        Text(
                                                                when (style) {
                                                                        ArtLayout.FULLCREEN_A24 ->
                                                                                "电影质感"
                                                                        ArtLayout.POLAROID -> "拍立得"
                                                                        ArtLayout.GEEK_STATS ->
                                                                                "极客数据"
                                                                }
                                                        )
                                                },
                                                colors =
                                                        FilterChipDefaults.filterChipColors(
                                                                selectedContainerColor =
                                                                        accentColor,
                                                                selectedLabelColor =
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimary
                                                        )
                                        )
                                }
                        }
                }

                // Polaroid-specific controls
                if (layout == ArtLayout.POLAROID) {
                        // Frame Material Selector
                        Column {
                                Text(
                                        "相框材质",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.labelSmall
                                )
                                Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                        val materials =
                                                listOf(
                                                        "CLASSIC_WHITE" to "纯白",
                                                        "CLASSIC_BLACK" to "纯黑",
                                                        "ACOUSTIC_WOOD" to "原木",
                                                        "HEAVY_MECHANICAL" to "机甲",
                                                        "CYBER_GLITCH" to "赛博"
                                                )
                                        materials.forEach { (key, label) ->
                                                FilterChip(
                                                        selected = polaroidFrameStyle == key,
                                                        onClick = {
                                                                onPolaroidFrameStyleChange(key)
                                                        },
                                                        label = { Text(label) },
                                                        colors =
                                                                FilterChipDefaults.filterChipColors(
                                                                        selectedContainerColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        selectedLabelColor =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onPrimary
                                                                )
                                                )
                                        }
                                }
                        }

                        // Wood Tweaks
                        if (polaroidFrameStyle == "ACOUSTIC_WOOD") {
                                HorizontalDivider(
                                        color =
                                                MaterialTheme.colorScheme.outlineVariant.copy(
                                                        alpha = 0.2f
                                                ),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                )

                                // Wood Type Color Blocks
                                Column {
                                        Text(
                                                "木材种类",
                                                color = Color.Gray,
                                                style = MaterialTheme.typography.labelSmall
                                        )
                                        Row(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.padding(top = 8.dp)
                                        ) {
                                                val woodTypes =
                                                        listOf(
                                                                WoodType.ASH to
                                                                        Color(0xFFE5D3B3), // 明亮白蜡
                                                                WoodType.WALNUT to
                                                                        Color(0xFF5D4037), // 深邃胡桃
                                                                WoodType.VINTAGE_OAK to
                                                                        Color(0xFFD2B48C) // 做旧橡木
                                                        )
                                                woodTypes.forEach { (wType, color) ->
                                                        val selected = woodType == wType
                                                        Box(
                                                                modifier =
                                                                        Modifier.size(32.dp)
                                                                                .clip(CircleShape)
                                                                                .background(color)
                                                                                .border(
                                                                                        width =
                                                                                                if (selected
                                                                                                )
                                                                                                        2.dp
                                                                                                else
                                                                                                        0.dp,
                                                                                        color =
                                                                                                if (selected
                                                                                                )
                                                                                                        Color.White
                                                                                                else
                                                                                                        Color.Transparent,
                                                                                        shape =
                                                                                                CircleShape
                                                                                )
                                                                                .clickable {
                                                                                        onWoodTypeChange(
                                                                                                wType
                                                                                        )
                                                                                }
                                                        )
                                                }
                                        }
                                }

                                // Engraving Depth Slider
                                Column {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text(
                                                        "雕刻深度 (Engraving)",
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        style = MaterialTheme.typography.labelSmall
                                                )
                                                Text(
                                                        "${(engravingDepth * 100).toInt()}%",
                                                        color = accentColor,
                                                        style = MaterialTheme.typography.labelSmall
                                                )
                                        }
                                        Slider(
                                                value = engravingDepth,
                                                onValueChange = onEngravingDepthChange,
                                                colors =
                                                        SliderDefaults.colors(
                                                                thumbColor = accentColor,
                                                                activeTrackColor = accentColor
                                                        )
                                        )
                                }

                                // Canvas Grain Slider
                                Column {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                Text(
                                                        "纸张质感 (Grain)",
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        style = MaterialTheme.typography.labelSmall
                                                )
                                                Text(
                                                        "${(canvasGrain * 100).toInt()}%",
                                                        color = accentColor,
                                                        style = MaterialTheme.typography.labelSmall
                                                )
                                        }
                                        Slider(
                                                value = canvasGrain,
                                                onValueChange = onCanvasGrainChange,
                                                colors =
                                                        SliderDefaults.colors(
                                                                thumbColor = accentColor,
                                                                activeTrackColor = accentColor
                                                        )
                                        )
                                }

                                HorizontalDivider(
                                        color =
                                                MaterialTheme.colorScheme.outlineVariant.copy(
                                                        alpha = 0.2f
                                                ),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                )
                        }

                        // Heavy Mechanical Tweaks
                        if (polaroidFrameStyle == "HEAVY_MECHANICAL") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Armor Plating Selector
                                        Column {
                                                Text(
                                                        "装甲涂装 (Armor Plating)",
                                                        color =
                                                                MaterialTheme.colorScheme
                                                                        .onSurfaceVariant,
                                                        style = MaterialTheme.typography.labelSmall
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Row(
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(12.dp)
                                                ) {
                                                        val armorTypes =
                                                                listOf(
                                                                        ArmorType.GUNMETAL to
                                                                                Color(
                                                                                        0xFF2C2C2E
                                                                                ), // 拉丝黑钛
                                                                        ArmorType.CARBON_FIBER to
                                                                                Color(
                                                                                        0xFF1C1C1E
                                                                                ), // 碳纤维
                                                                        ArmorType.WORN_OLIVE to
                                                                                Color(
                                                                                        0xFF4B5320
                                                                                ) // 战损绿
                                                                )
                                                        armorTypes.forEach { (aType, color) ->
                                                                val selected = armorType == aType
                                                                Box(
                                                                        modifier =
                                                                                Modifier.size(32.dp)
                                                                                        .clip(
                                                                                                CircleShape
                                                                                        )
                                                                                        .background(
                                                                                                color
                                                                                        )
                                                                                        .border(
                                                                                                width =
                                                                                                        if (selected
                                                                                                        )
                                                                                                                2.dp
                                                                                                        else
                                                                                                                0.dp,
                                                                                                color =
                                                                                                        if (selected
                                                                                                        )
                                                                                                                accentColor
                                                                                                        else
                                                                                                                Color.Transparent,
                                                                                                shape =
                                                                                                        CircleShape
                                                                                        )
                                                                                        .clickable {
                                                                                                onArmorTypeChange(
                                                                                                        aType
                                                                                                )
                                                                                        }
                                                                )
                                                        }
                                                }
                                        }

                                        // Rivets & Seams Slider
                                        Column {
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween
                                                ) {
                                                        Text(
                                                                "铆钉与接缝 (Rivets & Seams)",
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall
                                                        )
                                                        Text(
                                                                "${(mechanicalSeams * 100).toInt()}%",
                                                                color = accentColor,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall
                                                        )
                                                }
                                                Slider(
                                                        value = mechanicalSeams,
                                                        onValueChange = onMechanicalSeamsChange,
                                                        colors =
                                                                SliderDefaults.colors(
                                                                        thumbColor = accentColor,
                                                                        activeTrackColor =
                                                                                accentColor
                                                                )
                                                )
                                        }

                                        // Hazard Striping Toggle
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment =
                                                        androidx.compose.ui.Alignment
                                                                .CenterVertically
                                        ) {
                                                Column {
                                                        Text(
                                                                "警戒涂装 (Hazard)",
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall
                                                        )
                                                        Text(
                                                                "黄黑斜纹警告线",
                                                                color = Color.DarkGray,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .labelSmall
                                                        )
                                                }
                                                Switch(
                                                        checked = hasHazardStriping,
                                                        onCheckedChange = onHasHazardStripingChange,
                                                        colors =
                                                                SwitchDefaults.colors(
                                                                        checkedThumbColor =
                                                                                accentColor,
                                                                        checkedTrackColor =
                                                                                accentColor.copy(
                                                                                        alpha = 0.5f
                                                                                )
                                                                )
                                                )
                                        }

                                        HorizontalDivider(
                                                color = Color.White.copy(alpha = 0.1f),
                                                modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                }
                        }

                        // Frame Padding Slider
                        Column {
                                Text(
                                        "外框留白  (底宽顶窄)",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.labelSmall
                                )
                                Slider(
                                        value = polaroidFramePadding,
                                        onValueChange = onPolaroidFramePaddingChange,
                                        valueRange = 0.2f..0.8f,
                                        colors =
                                                SliderDefaults.colors(
                                                        thumbColor = accentColor,
                                                        activeTrackColor = accentColor
                                                )
                                )
                        }

                        // Inner Border Slider
                        Column {
                                Text(
                                        "内衬描边",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.labelSmall
                                )
                                Slider(
                                        value = polaroidInnerBorder,
                                        onValueChange = onPolaroidInnerBorderChange,
                                        valueRange = 0f..4f,
                                        colors =
                                                SliderDefaults.colors(
                                                        thumbColor = accentColor,
                                                        activeTrackColor = accentColor
                                                )
                                )
                        }
                }

                // Typography modifiers
                Column {
                        Text(
                                "字体颜色",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                        )
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
                                                                selectedContainerColor =
                                                                        accentColor,
                                                                selectedLabelColor =
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimary
                                                        )
                                        )
                                }
                        }
                }

                // Calligraphy Font
                Column {
                        Text(
                                "书法字体",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                        )
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
                                        val context =
                                                androidx.compose.ui.platform.LocalContext.current
                                        val fontFamily =
                                                com.footprint.utils.FontManager.getFontFamily(
                                                        context,
                                                        f
                                                )
                                        FilterChip(
                                                selected = fontName == f,
                                                onClick = { onFontNameChange(f) },
                                                label = {
                                                        Text(
                                                                if (f == "Default") "默认" else f,
                                                                fontFamily = fontFamily
                                                        )
                                                },
                                                colors =
                                                        FilterChipDefaults.filterChipColors(
                                                                selectedContainerColor =
                                                                        accentColor,
                                                                selectedLabelColor =
                                                                        MaterialTheme.colorScheme
                                                                                .onPrimary
                                                        )
                                        )
                                }
                        }
                }

                Column {
                        Text(
                                "字体效果",
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                        selected = isItalic,
                                        onClick = { onItalicChange(!isItalic) },
                                        label = { Text("斜体 (Italic)") },
                                        colors =
                                                FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor =
                                                                MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor =
                                                                MaterialTheme.colorScheme.onPrimary
                                                )
                                )
                                FilterChip(
                                        selected = hasBorder,
                                        onClick = { onBorderChange(!hasBorder) },
                                        label = { Text("黑边 (Black Border)") },
                                        colors =
                                                FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor =
                                                                MaterialTheme.colorScheme.primary,
                                                        selectedLabelColor =
                                                                MaterialTheme.colorScheme.onPrimary
                                                )
                                )
                        }
                }

                // Sliders
                Column {
                        Text(
                                "线条粗细",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                        )
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
                        Text(
                                "霓虹光晕",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                        )
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
