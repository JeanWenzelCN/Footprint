package com.footprint.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.footprint.data.model.FootprintEntry
import com.footprint.data.model.Mood
import java.time.LocalDate

@Composable
fun MoodHeatmap(entries: List<FootprintEntry>, year: Int) {
    val moodsByMonth = (1..12).map { month ->
        entries.filter { it.happenedOn.year == year && it.happenedOn.monthValue == month }
            .groupBy { it.mood }
            .maxByOrNull { it.value.size }?.key
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "年度心情色谱", 
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            moodsByMonth.forEachIndexed { index, mood ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(mood?.color?.copy(alpha = 0.6f) ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (mood != null) {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(mood.color))
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1月", style = TextStyle(fontSize = 8.sp), color = MaterialTheme.colorScheme.outline)
            Text("12月", style = TextStyle(fontSize = 8.sp), color = MaterialTheme.colorScheme.outline)
        }
    }
}

private typealias TextStyle = androidx.compose.ui.text.TextStyle