package com.footprint.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.footprint.data.model.Mood
import com.footprint.service.LocationTrackingService
import com.footprint.utils.AIStoryGenerator
import com.footprint.utils.ImageUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddFootprintDialog(
        initialEntry: com.footprint.data.model.FootprintEntry? = null,
        onDismiss: () -> Unit,
        onSave: (FootprintDraft) -> Unit
) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val scrollState = rememberScrollState()
        var title by remember { mutableStateOf(initialEntry?.title ?: "") }
        var location by remember { mutableStateOf(initialEntry?.location ?: "") }
        var detail by remember { mutableStateOf(initialEntry?.detail ?: "") }
        var tags by remember { mutableStateOf(initialEntry?.tags?.joinToString(",") ?: "") }
        var distance by remember { mutableStateOf(initialEntry?.distanceKm?.toString() ?: "5") }
        var energy by remember { mutableStateOf(initialEntry?.energyLevel?.toFloat() ?: 6f) }
        var mood by remember { mutableStateOf(initialEntry?.mood ?: Mood.EXCITED) }
        var selectedIcon by remember { mutableStateOf(initialEntry?.icon ?: "LocationOn") }
        var photoPaths by remember { mutableStateOf(initialEntry?.photos ?: emptyList<String>()) }

        val photoPickerLauncher =
                rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetMultipleContents()
                ) { uris ->
                        scope.launch(Dispatchers.IO) {
                                val newPaths =
                                        uris.mapNotNull { uri ->
                                                ImageUtils.saveFootprintImage(context, uri)
                                        }
                                withContext(Dispatchers.Main) { photoPaths = photoPaths + newPaths }
                        }
                }

        val datePickerState =
                rememberDatePickerState(
                        initialSelectedDateMillis =
                                initialEntry
                                        ?.happenedOn
                                        ?.atStartOfDay(ZoneId.systemDefault())
                                        ?.toInstant()
                                        ?.toEpochMilli()
                                        ?: System.currentTimeMillis()
                )
        var showDatePicker by remember { mutableStateOf(false) }

        val availableIcons =
                listOf(
                        "LocationOn",
                        "Restaurant",
                        "LocalCafe",
                        "Park",
                        "Flight",
                        "Train",
                        "DirectionsBike",
                        "ShoppingBag",
                        "CameraAlt",
                        "MusicNote",
                        "Movie",
                        "DirectionsRun",
                        "Pets",
                        "School",
                        "Work"
                )

        val currentLocation by LocationTrackingService.currentLocation.collectAsState()

        if (showDatePicker) {
                DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                                TextButton(onClick = { showDatePicker = false }) { Text("确定") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
                        }
                ) { DatePicker(state = datePickerState) }
        }

        val selectedDate =
                datePickerState.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                        ?: LocalDate.now()

        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
                LiquidGlassCard(
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth().padding(8.dp).heightIn(max = 600.dp)
                ) {
                        Column(
                                modifier = Modifier.padding(24.dp).verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                Text(
                                        text = if (initialEntry != null) "编辑足迹" else "添加新的足迹",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                )

                                OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("标题") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                )

                                // Icon Picker
                                Column {
                                        Text(
                                                "选择图标",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                        )
                                        LazyRow(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                                items(availableIcons) { iconName ->
                                                        val isSelected = selectedIcon == iconName
                                                        val icon = IconUtils.getIconByName(iconName)
                                                        Surface(
                                                                color =
                                                                        if (isSelected)
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                        else
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .surfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.3f
                                                                                        ),
                                                                shape = CircleShape,
                                                                modifier =
                                                                        Modifier.size(44.dp)
                                                                                .clip(CircleShape)
                                                                                .clickable {
                                                                                        selectedIcon =
                                                                                                iconName
                                                                                }
                                                        ) {
                                                                Box(
                                                                        contentAlignment =
                                                                                Alignment.Center
                                                                ) {
                                                                        Icon(
                                                                                icon,
                                                                                null,
                                                                                tint =
                                                                                        if (isSelected
                                                                                        )
                                                                                                Color.White
                                                                                        else
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurfaceVariant,
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                20.dp
                                                                                        )
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }

                                // Photo Picker
                                Column {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        "足迹图片",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                )
                                                TextButton(
                                                        onClick = {
                                                                photoPickerLauncher.launch(
                                                                        "image/*"
                                                                )
                                                        }
                                                ) {
                                                        Icon(
                                                                Icons.Default.AddPhotoAlternate,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("添加图片")
                                                }
                                        }

                                        if (photoPaths.isNotEmpty()) {
                                                LazyRow(
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(8.dp),
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .height(100.dp)
                                                ) {
                                                        itemsIndexed(photoPaths) { index, path ->
                                                                Box(
                                                                        modifier =
                                                                                Modifier.size(
                                                                                                100.dp
                                                                                        )
                                                                                        .clip(
                                                                                                RoundedCornerShape(
                                                                                                        8.dp
                                                                                                )
                                                                                        )
                                                                                        .clickable {
                                                                                                val newPaths =
                                                                                                        photoPaths
                                                                                                                .toMutableList()
                                                                                                val current =
                                                                                                        newPaths.removeAt(
                                                                                                                index
                                                                                                        )
                                                                                                newPaths.add(
                                                                                                        0,
                                                                                                        current
                                                                                                )
                                                                                                photoPaths =
                                                                                                        newPaths
                                                                                        }
                                                                ) {
                                                                        AsyncImage(
                                                                                model = path,
                                                                                contentDescription =
                                                                                        null,
                                                                                modifier =
                                                                                        Modifier.fillMaxSize(),
                                                                                contentScale =
                                                                                        ContentScale
                                                                                                .Crop
                                                                        )
                                                                        if (index == 0) {
                                                                                Surface(
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .primary
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.8f
                                                                                                        ),
                                                                                        shape =
                                                                                                RoundedCornerShape(
                                                                                                        bottomEnd =
                                                                                                                8.dp
                                                                                                ),
                                                                                        modifier =
                                                                                                Modifier.align(
                                                                                                        Alignment
                                                                                                                .TopStart
                                                                                                )
                                                                                ) {
                                                                                        Text(
                                                                                                "封面",
                                                                                                modifier =
                                                                                                        Modifier.padding(
                                                                                                                horizontal =
                                                                                                                        6.dp,
                                                                                                                vertical =
                                                                                                                        2.dp
                                                                                                        ),
                                                                                                style =
                                                                                                        MaterialTheme
                                                                                                                .typography
                                                                                                                .labelSmall,
                                                                                                color =
                                                                                                        Color.White
                                                                                        )
                                                                                }
                                                                        }
                                                                        IconButton(
                                                                                onClick = {
                                                                                        photoPaths =
                                                                                                photoPaths -
                                                                                                        path
                                                                                },
                                                                                modifier =
                                                                                        Modifier.align(
                                                                                                        Alignment
                                                                                                                .TopEnd
                                                                                                )
                                                                                                .size(
                                                                                                        24.dp
                                                                                                )
                                                                                                .background(
                                                                                                        Color.Black
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.5f
                                                                                                                ),
                                                                                                        CircleShape
                                                                                                )
                                                                        ) {
                                                                                Icon(
                                                                                        Icons.Default
                                                                                                .Close,
                                                                                        contentDescription =
                                                                                                null,
                                                                                        tint =
                                                                                                Color.White,
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                        14.dp
                                                                                                )
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }

                                OutlinedTextField(
                                        value = location,
                                        onValueChange = { location = it },
                                        label = { Text("地点") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                        value = detail,
                                        onValueChange = { detail = it },
                                        label = { Text("故事和感受") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        minLines = 3
                                )

                                Row {
                                        TextButton(
                                                onClick = {
                                                        scope.launch {
                                                                val story =
                                                                        AIStoryGenerator
                                                                                .generateStory(
                                                                                        location,
                                                                                        mood,
                                                                                        selectedDate
                                                                                )
                                                                detail = story
                                                        }
                                                }
                                        ) {
                                                Icon(
                                                        Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("帮我写")
                                        }
                                }

                                Button(
                                        onClick = {
                                                onSave(
                                                        FootprintDraft(
                                                                title = title,
                                                                location = location,
                                                                detail = detail,
                                                                mood = mood,
                                                                tags =
                                                                        tags.split(",").filter {
                                                                                it.isNotBlank()
                                                                        },
                                                                distance = distance.toDoubleOrNull()
                                                                                ?: 0.0,
                                                                photos = photoPaths,
                                                                date = selectedDate,
                                                                energy = energy.toInt(),
                                                                icon = selectedIcon,
                                                                latitude = initialEntry?.latitude
                                                                                ?: currentLocation
                                                                                        ?.latitude,
                                                                longitude = initialEntry?.longitude
                                                                                ?: currentLocation
                                                                                        ?.longitude
                                                        )
                                                )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                ) { Text(if (initialEntry != null) "保存修改" else "记录足迹") }
                        }
                }
        }
}

data class FootprintDraft(
        val title: String,
        val location: String,
        val detail: String,
        val mood: Mood,
        val tags: List<String>,
        val distance: Double,
        val photos: List<String>,
        val date: LocalDate,
        val energy: Int,
        val icon: String,
        val latitude: Double? = null,
        val longitude: Double? = null
)
