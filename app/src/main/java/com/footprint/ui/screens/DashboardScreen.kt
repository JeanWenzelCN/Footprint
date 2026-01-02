package com.footprint.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.footprint.data.model.Mood
import com.footprint.data.model.FootprintEntry
import com.footprint.ui.state.FootprintUiState
import com.footprint.ui.components.AppBackground
import com.footprint.ui.components.GlassMorphicCard
import com.footprint.ui.components.AboutDialog
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.time.LocalDate

enum class StatType(val label: String, val icon: ImageVector) {
    TOTAL_RECORDS("年度足迹记录", Icons.Default.Description),
    MILEAGE("里程详情", Icons.Default.Route),
    UNIQUE_PLACES("探索地点清单", Icons.Default.Place),
    MONTHLY_RECORDS("本月详细记录", Icons.Default.Description),
    ENERGY("能量/活力分布", Icons.Default.Bolt),
    MOOD("心情分布统计", Icons.Default.EmojiEmotions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    state: FootprintUiState,
    onSearch: (String) -> Unit,
    onYearShift: (Int) -> Unit,
    onMoodSelected: (Mood?) -> Unit,
    onCreateGoal: () -> Unit,
    onExportTrace: () -> Unit,
    onSettings: () -> Unit,
    onEditEntry: (com.footprint.data.model.FootprintEntry) -> Unit,
    onEditGoal: (com.footprint.data.model.TravelGoal) -> Unit
) {
    var query by rememberSaveable { mutableStateOf(state.filterState.searchQuery) }
    val df = remember { DecimalFormat("0.0") }

    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    // Bottom Sheet State
    var selectedStatType by remember { mutableStateOf<StatType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val openStatDetail = { type: StatType ->
        selectedStatType = type
        showBottomSheet = true
    }

    AppBackground(modifier = modifier) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "年度·月度足迹雷达",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Box {
                        androidx.compose.material3.IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("设置") },
                                onClick = {
                                    showMenu = false
                                    onSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("反馈") },
                                onClick = {
                                    showMenu = false
                                    uriHandler.openUri("https://github.com/StarsUnsurpass/Footprint/issues")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("关于") },
                                onClick = {
                                    showMenu = false
                                    showAboutDialog = true
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearch(it)
                    },
                    label = { Text("搜索地点/标签/故事") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // History Trace Card
            item {
                GlassMorphicCard(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onExportTrace)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = "历史足迹",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "回放你的时空轨迹",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                YearNavigator(
                    year = state.filterState.year,
                    onBack = { onYearShift(-1) },
                    onForward = { onYearShift(1) }
                )
            }

            item {
                SummaryRow(
                    title = "年度足迹",
                    stats = listOf(
                        Triple("总记录", "${state.summary.yearly.totalEntries} 次") { openStatDetail(StatType.TOTAL_RECORDS) },
                        Triple("里程", "${df.format(state.summary.yearly.totalDistance)} km") { openStatDetail(StatType.MILEAGE) },
                        Triple("独特地点", "${state.summary.yearly.uniquePlaces}") { openStatDetail(StatType.UNIQUE_PLACES) }
                    )
                )
            }
            item {
                SummaryRow(
                    title = "本月亮点",
                    stats = listOf(
                        Triple("记录", "${state.summary.monthly.totalEntries}") { openStatDetail(StatType.MONTHLY_RECORDS) },
                        Triple("活力", (state.summary.monthly.energyAverage.takeIf { it > 0 }?.let { df.format(it) } ?: "-")) { openStatDetail(StatType.ENERGY) },
                        Triple("主情绪", (state.summary.monthly.dominantMood?.label ?: "待发现")) { openStatDetail(StatType.MOOD) }
                    )
                )
            }

            item {
                MoodDistributionSection(mood = state.summary.yearly.dominantMood, onMoodSelected = onMoodSelected)
            }

            // Recent Footprints (No limit)
            recentFootprintsSection(entries = state.entries, onCreateGoal = onCreateGoal, onEditEntry = onEditEntry)
            
            // Goals Section (No limit)
            goalsListSection(goals = state.goals, onEditGoal = onEditGoal)
            
            item {
                Spacer(modifier = Modifier.height(80.dp)) // Bottom padding for nav bar
            }
        }

        // Detail Bottom Sheet
        if (showBottomSheet && selectedStatType != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                StatDetailContent(
                    type = selectedStatType!!,
                    state = state,
                    onEditEntry = { 
                        onEditEntry(it)
                        showBottomSheet = false
                    }
                )
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun StatDetailContent(
    type: StatType,
    state: FootprintUiState,
    onEditEntry: (FootprintEntry) -> Unit
) {
    val df = remember { DecimalFormat("0.0") }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    
    val entries = when (type) {
        StatType.TOTAL_RECORDS, StatType.MILEAGE, StatType.UNIQUE_PLACES -> state.visibleEntries
        StatType.MONTHLY_RECORDS, StatType.ENERGY, StatType.MOOD -> {
            val now = LocalDate.now()
            state.entries.filter { it.happenedOn.year == now.year && it.happenedOn.monthValue == now.monthValue }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                type.icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = type.label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        when (type) {
            StatType.UNIQUE_PLACES -> {
                val places = entries.groupBy { it.location }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(places) { (location, count) ->
                        GlassMorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Text(location, style = MaterialTheme.typography.titleMedium)
                                }
                                Text("${count} 次记录", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            StatType.MOOD -> {
                val moodStats = entries.groupBy { it.mood }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(moodStats) { (mood, count) ->
                        GlassMorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(mood.color))
                                    Text(mood.label, style = MaterialTheme.typography.titleMedium)
                                }
                                Text("${count} 次记录", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sortedEntries = if (type == StatType.ENERGY) entries.sortedByDescending { it.energyLevel } else entries
                    
                    items(sortedEntries) { entry ->
                        GlassMorphicCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditEntry(entry) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(entry.happenedOn.format(formatter), style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Place, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text(entry.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                if (type == StatType.MILEAGE) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "里程: ${df.format(entry.distanceKm)} km",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (type == StatType.ENERGY) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("活力值:", style = MaterialTheme.typography.labelMedium)
                                        LinearProgressIndicator(
                                            progress = { entry.energyLevel / 10f },
                                            modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        Text("${entry.energyLevel}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun YearNavigator(year: Int, onBack: () -> Unit, onForward: () -> Unit) {
    GlassMorphicCard(shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "聚焦年份", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(icon = Icons.Outlined.ArrowBackIosNew, onClick = onBack)
                Text(text = year.toString(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 12.dp))
                IconButton(icon = Icons.AutoMirrored.Outlined.ArrowForwardIos, onClick = onForward)
            }
        }
    }
}

@Composable
private fun IconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(icon, contentDescription = null)
    }
}

@Composable
private fun SummaryRow(title: String, stats: List<Triple<String, String, () -> Unit>>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            stats.forEachIndexed { index, stat ->
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label = stat.first,
                    value = stat.second,
                    onClick = stat.third,
                    accent = when (index) {
                        0 -> MaterialTheme.colorScheme.primary
                        1 -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, label: String, value: String, accent: Color, onClick: () -> Unit) {
    GlassMorphicCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value, 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun MoodDistributionSection(mood: Mood?, onMoodSelected: (Mood?) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "心情雷达", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Mood.entries.forEach { option ->
                FilterChip(
                    selected = mood == option,
                    onClick = { onMoodSelected(option) },
                    label = { Text(option.label) },
                    leadingIcon = {
                        Spacer(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(option.color)
                        )
                    }
                )
            }
            FilterChip(
                selected = mood == null,
                onClick = { onMoodSelected(null) },
                label = { Text("全部") }
            )
        }
    }
}

private fun LazyListScope.goalsListSection(
    goals: List<com.footprint.data.model.TravelGoal>,
    onEditGoal: (com.footprint.data.model.TravelGoal) -> Unit
) {
    if (goals.isEmpty()) return
    
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    item {
        Text(text = "我的目标", style = MaterialTheme.typography.titleMedium)
    }
    items(goals) { goal ->
        GlassMorphicCard(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.clickable { onEditGoal(goal) }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = goal.title, 
                        style = MaterialTheme.typography.titleMedium, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    if (goal.isCompleted) {
                        Text(
                            text = "已完成", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "目的地: ${goal.targetLocation} · 预计: ${goal.targetDate.format(formatter)}", 
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun LazyListScope.recentFootprintsSection(
    entries: List<com.footprint.data.model.FootprintEntry>, 
    onCreateGoal: () -> Unit,
    onEditEntry: (com.footprint.data.model.FootprintEntry) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MM-dd")
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "最近的灵感", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onCreateGoal) { Text("新的计划") }
        }
    }
    items(entries) { entry ->
        GlassMorphicCard(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.clickable { onEditEntry(entry) }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${entry.location} · ${entry.happenedOn.format(formatter)}", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = entry.detail, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}