package com.footprint.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.footprint.data.model.Mood
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddFootprintDialog(
    initialEntry: com.footprint.data.model.FootprintEntry? = null,
    onDismiss: () -> Unit,
    onSave: (FootprintDraft) -> Unit
) {
    var title by remember { mutableStateOf(initialEntry?.title ?: "") }
    var location by remember { mutableStateOf(initialEntry?.location ?: "") }
    var detail by remember { mutableStateOf(initialEntry?.detail ?: "") }
    var tags by remember { mutableStateOf(initialEntry?.tags?.joinToString(",") ?: "") }
    var distance by remember { mutableStateOf(initialEntry?.distanceKm?.toString() ?: "5") }
    var energy by remember { mutableStateOf(initialEntry?.energyLevel?.toFloat() ?: 6f) }
    var mood by remember { mutableStateOf(initialEntry?.mood ?: Mood.EXCITED) }
    val datePickerState = rememberDatePickerState(
        initialEntry?.happenedOn?.atStartOfDay(java.time.ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: System.currentTimeMillis()
    )
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val selectedDate = datePickerState.selectedDateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
    } ?: LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val payload = FootprintDraft(
                        title = title,
                        location = location,
                        detail = detail,
                        mood = mood,
                        tags = tags.split(',', '，').mapNotNull { it.trim().takeIf(String::isNotEmpty) },
                        distance = distance.toDoubleOrNull() ?: 0.0,
                        energy = energy.toInt().coerceIn(1, 10),
                        date = selectedDate
                    )
                    onSave(payload)
                },
                enabled = title.isNotBlank() && location.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text(text = "添加新的足迹") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("地点") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("故事和感受") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("标签，用逗号分隔") },
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mood.entries.forEach { option ->
                        FilterChip(
                            selected = mood == option,
                            onClick = { mood = option },
                            label = { Text(option.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = distance,
                    onValueChange = { distance = it },
                    label = { Text("里程 (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text(text = "活力指数: ${energy.toInt()}")
                    Slider(
                        value = energy,
                        onValueChange = { energy = it },
                        steps = 8,
                        valueRange = 1f..10f
                    )
                }
                Button(onClick = { showDatePicker = true }) {
                    Text("选择日期：$selectedDate")
                }
            }
        }
    )
}

data class FootprintDraft(
    val title: String,
    val location: String,
    val detail: String,
    val mood: Mood,
    val tags: List<String>,
    val distance: Double,
    val energy: Int,
    val date: LocalDate
)
