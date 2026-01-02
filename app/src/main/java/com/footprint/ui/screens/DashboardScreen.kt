package com.footprint.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.footprint.data.model.Mood
import com.footprint.data.model.FootprintEntry
import com.footprint.ui.state.FootprintUiState
import com.footprint.ui.components.AppBackground
import com.footprint.ui.components.GlassMorphicCard
import com.footprint.ui.components.AboutDialog
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.time.LocalDate

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

    var selectedStatType by remember { mutableStateOf<StatType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    val openStatDetail = { type: StatType ->
        selectedStatType = type
        showBottomSheet = true
    }

    AppBackground(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(top = 100.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Search Bar
                item {
                    SearchBar(
                        query = query,
                        onQueryChange = {
                            query = it
                            onSearch(it)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Statistics Grid (Refined)
                item {
                    StatisticsSection(
                        state = state,
                        onStatClick = openStatDetail,
                        df = df
                    )
                }

                // History Trace Action
                item {
                    TelegramActionCard(
                        title = "时光足迹回放",
                        subtitle = "查看历史移动轨迹与时空分布",
                        icon = Icons.Default.History,
                        onClick = onExportTrace
                    )
                }

                item {
                    YearNavigator(
                        year = state.filterState.year,
                        onBack = { onYearShift(-1) },
                        onForward = { onYearShift(1) }
                    )
                }

                // Mood Radar
                item {
                    MoodRadarSection(
                        currentMood = state.summary.yearly.dominantMood,
                        onMoodSelected = onMoodSelected
                    )
                }

                // Sections
                recentFootprintsSection(entries = state.entries, onCreateGoal = onCreateGoal, onEditEntry = onEditEntry)
                
                goalsListSection(goals = state.goals, onEditGoal = onEditGoal)
            }

            // Telegram style Blurred Top Bar
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .blur(if (showBottomSheet) 10.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Footprint",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "探索你的世界",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("设置") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = {
                                showMenu = false
                                onSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("关于") },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                            onClick = {
                                showMenu = false
                                showAboutDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Bottom Sheet
        if (showBottomSheet && selectedStatType != null) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
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
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "搜索地点、标签...", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StatisticsSection(
    state: FootprintUiState,
    onStatClick: (StatType) -> Unit,
    df: DecimalFormat
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "数据概览", 
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatItem(
                label = "足迹",
                value = "${state.summary.yearly.totalEntries}",
                modifier = Modifier.weight(1f),
                onClick = { onStatClick(StatType.TOTAL_RECORDS) }
            )
            StatItem(
                label = "里程",
                value = "${df.format(state.summary.yearly.totalDistance)}",
                unit = "km",
                modifier = Modifier.weight(1f),
                onClick = { onStatClick(StatType.MILEAGE) }
            )
            StatItem(
                label = "地点",
                value = "${state.summary.yearly.uniquePlaces}",
                modifier = Modifier.weight(1f),
                onClick = { onStatClick(StatType.UNIQUE_PLACES) }
            )
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            if (unit.isNotEmpty()) {
                Text(text = unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TelegramActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun MoodRadarSection(currentMood: Mood?, onMoodSelected: (Mood?) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("年度心情偏好", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Mood.entries.forEach { mood ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (currentMood == mood) mood.color else mood.color.copy(alpha = 0.2f))
                        .clickable { onMoodSelected(mood) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mood.label.take(1),
                        color = if (currentMood == mood) Color.White else mood.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(places) { (location, count) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                    Text(location, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("${count} 次", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(moodStats) { (mood, count) ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = mood.color.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(mood.color))
                                    Text(mood.label, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("${count} 次", style = MaterialTheme.typography.labelSmall, color = mood.color)
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sortedEntries = if (type == StatType.ENERGY) entries.sortedByDescending { it.energyLevel } else entries
                    
                    items(sortedEntries) { entry ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditEntry(entry) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(entry.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text(entry.happenedOn.format(formatter), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                if (type == StatType.MILEAGE) {
                                    Text(
                                        "${df.format(entry.distanceKm)} km",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "年份筛选", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Outlined.ArrowBackIosNew, null, modifier = Modifier.size(14.dp))
            }
            Text(text = year.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp))
            IconButton(onClick = onForward, modifier = Modifier.size(24.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, null, modifier = Modifier.size(14.dp))
            }
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
        Text(
            text = "旅行目标", 
            style = MaterialTheme.typography.labelMedium, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, top = 12.dp)
        )
    }
    items(goals) { goal ->
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { onEditGoal(goal) }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (goal.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (goal.isCompleted) Icons.Default.Check else Icons.Default.Flag,
                        null,
                        tint = if (goal.isCompleted) Color.White else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${goal.targetLocation} · ${goal.targetDate.format(formatter)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun LazyListScope.recentFootprintsSection(
    entries: List<com.footprint.data.model.FootprintEntry>, 
    onCreateGoal: () -> Unit,
    onEditEntry: (com.footprint.data.model.FootprintEntry) -> Unit
) {
    if (entries.isEmpty()) return
    val formatter = DateTimeFormatter.ofPattern("MM-dd")
    item {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "最近足迹", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(
                text = "新建 +", 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onCreateGoal() }
            )
        }
    }
    items(entries.take(5)) { entry ->
        TelegramEntryItem(entry, formatter, { onEditEntry(entry) })
    }
}

@Composable
private fun TelegramEntryItem(
    entry: FootprintEntry, 
    dateFormatter: DateTimeFormatter, 
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(entry.mood.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.title.take(1),
                style = MaterialTheme.typography.titleMedium,
                color = entry.mood.color,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.happenedOn.format(dateFormatter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = entry.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
