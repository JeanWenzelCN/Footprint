package com.footprint.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.footprint.data.model.FootprintEntry
import com.footprint.data.model.Mood
import com.footprint.ui.state.FilterState
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    modifier: Modifier = Modifier,
    entries: List<FootprintEntry>,
    filterState: FilterState,
    onMoodFilterChange: (Mood?) -> Unit,
    onSearch: (String) -> Unit,
    onEditEntry: (FootprintEntry) -> Unit
) {
    var query by rememberSaveable { mutableStateOf(filterState.searchQuery) }
    val grouped = entries.groupBy { it.happenedOn.withDayOfMonth(1) }
        .toSortedMap(compareByDescending { it })
    val formatter = DateTimeFormatter.ofPattern("MM月dd日")
    val headerFormatter = DateTimeFormatter.ofPattern("yyyy年MM月")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ... (省略部分代码)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            grouped.forEach { (month, items) ->
                stickyHeader {
                    // ... (省略部分代码)
                }
                items(items) { entry ->
                    TimelineCard(entry = entry, formatter = formatter, onClick = { onEditEntry(entry) })
                }
            }
        }
    }
}

@Composable
private fun TimelineCard(entry: FootprintEntry, formatter: DateTimeFormatter, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // ... (内部内容保持不变)

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = entry.happenedOn.format(formatter), style = MaterialTheme.typography.labelMedium)
            Text(text = entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = entry.location, style = MaterialTheme.typography.bodyMedium)
            Text(text = entry.detail, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(text = "情绪：${entry.mood.label} · 里程 ${String.format("%.1f", entry.distanceKm)} km", style = MaterialTheme.typography.labelSmall)
        }
    }
}
