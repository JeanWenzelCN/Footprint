package com.footprint.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.footprint.ui.components.weather.HolographicWeatherIcon
import com.footprint.ui.components.weather.WeatherType
import com.footprint.ui.components.weather.WeatherCategory
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
        var selectedWeather by remember { mutableStateOf<String?>(initialEntry?.weather) }
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

        val selectedWeatherType =
                remember(selectedWeather) {
                        WeatherType.values().find { it.name == selectedWeather }
                }
        val backgroundColorTint by
                animateColorAsState(
                        targetValue = selectedWeatherType?.color?.copy(alpha = 0.08f)
                                        ?: MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        animationSpec = tween(1000)
                )

        androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
                LiquidGlassCard(
                        shape = RoundedCornerShape(28.dp),
                        backgroundColor = backgroundColorTint,
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

                                // Weather Carousel
                                Column {
                                        Text(
                                                "天气胶囊",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        WeatherCarousel(
                                                selectedWeather = selectedWeather,
                                                onWeatherSelected = { selectedWeather = it.name }
                                        )
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

                                // Stat Controls: Distance, Energy, Mood
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Distance
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            HolographicMileageIcon(
                                                distanceKm = distance.toDoubleOrNull() ?: 0.0,
                                                size = 48.dp
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = distance,
                                                onValueChange = { distance = it },
                                                label = { Text("里程 (KM)") },
                                                modifier = Modifier.width(100.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )
                                        }

                                        // Energy
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            HolographicEnergyIcon(
                                                energyLevel = energy.toInt(),
                                                size = 48.dp
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                "能量: ${energy.toInt()}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Slider(
                                                value = energy,
                                                onValueChange = { energy = it },
                                                valueRange = 1f..10f,
                                                steps = 8,
                                                modifier = Modifier.width(100.dp)
                                            )
                                        }

                                        // Mood
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            HolographicMoodIcon(
                                                mood = mood,
                                                size = 48.dp
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            var expandedMood by remember { mutableStateOf(false) }
                                            ExposedDropdownMenuBox(
                                                expanded = expandedMood,
                                                onExpandedChange = { expandedMood = !expandedMood }
                                            ) {
                                                OutlinedTextField(
                                                    value = mood.label,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("心情") },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMood) },
                                                    modifier = Modifier.menuAnchor().width(100.dp)
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = expandedMood,
                                                    onDismissRequest = { expandedMood = false }
                                                ) {
                                                    Mood.values().forEach { m ->
                                                        DropdownMenuItem(
                                                            text = { Text(m.label) },
                                                            onClick = {
                                                                mood = m
                                                                expandedMood = false
                                                            }
                                                        )
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
                                                                weather = selectedWeather,
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
        val weather: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun WeatherCarousel(selectedWeather: String?, onWeatherSelected: (com.footprint.ui.components.weather.WeatherType) -> Unit) {
        val listState = rememberLazyListState()
        val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        val context = LocalContext.current
        val vibrator = remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager =
                                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as
                                        VibratorManager
                        vibratorManager.defaultVibrator
                } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
        }

        val allWeathers = remember { com.footprint.ui.components.weather.WeatherType.values() }
        val categories = remember { com.footprint.ui.components.weather.WeatherCategory.values() }
        
        var selectedCategory by remember { 
            mutableStateOf(
                selectedWeather?.let { wName -> allWeathers.find { it.name == wName }?.category } 
                ?: categories.first()
            ) 
        }

        val weathersInCategory = remember(selectedCategory) {
            allWeathers.filter { it.category == selectedCategory }
        }

        // Setup initial scroll position based on selection or middle
        val initialIndex = remember(selectedCategory) {
                selectedWeather
                        ?.let { weatherName -> weathersInCategory.indexOfFirst { it.name == weatherName } }
                        ?.takeIf { it >= 0 }
                        ?: (Int.MAX_VALUE / 2) // Middle of infinitely scrolling list
        }

        LaunchedEffect(selectedCategory) {
                if (selectedWeather == null || allWeathers.find { it.name == selectedWeather }?.category != selectedCategory) {
                        // Pre-select first item in new category if the current weather doesn't match
                        val first = weathersInCategory.firstOrNull()
                        if (first != null) {
                            onWeatherSelected(first)
                        }
                        listState.scrollToItem(Int.MAX_VALUE / 2)
                } else {
                        listState.scrollToItem(initialIndex)
                }
        }

        // Determine the currently focused item and trigger haptics
        LaunchedEffect(listState.isScrollInProgress, selectedCategory) {
                if (!listState.isScrollInProgress) {
                        // After settling from a scroll, find center item
                        val layoutInfo = listState.layoutInfo
                        val visibleItemsInfo = layoutInfo.visibleItemsInfo
                        if (visibleItemsInfo.isNotEmpty() && weathersInCategory.isNotEmpty()) {
                                val viewportCenter =
                                        layoutInfo.viewportStartOffset +
                                                (layoutInfo.viewportEndOffset -
                                                        layoutInfo.viewportStartOffset) / 2
                                val closestItem =
                                        visibleItemsInfo.minByOrNull {
                                                Math.abs(it.offset + (it.size / 2) - viewportCenter)
                                        }
                                closestItem?.let {
                                        val actualIndex = it.index % weathersInCategory.size
                                        val activatedWeather = weathersInCategory[actualIndex]
                                        if (selectedWeather != activatedWeather.name) {
                                                onWeatherSelected(activatedWeather)

                                                // Haptic Sync custom vibrations
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        when (activatedWeather.category) {
                                                                com.footprint.ui.components.weather.WeatherCategory.EXTREME -> {
                                                                        vibrator.vibrate(VibrationEffect.createOneShot(50, 255))
                                                                }
                                                                com.footprint.ui.components.weather.WeatherCategory.SNOW_ICE -> {
                                                                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 50, 50), intArrayOf(0, 80, 0, 80), -1))
                                                                }
                                                                else -> {
                                                                        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                                                                }
                                                        }
                                                } else {
                                                        @Suppress("DEPRECATION")
                                                        vibrator.vibrate(20)
                                                }
                                        }
                                }
                        }
                }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
                // Tier 1: Category Selector
                ScrollableTabRow(
                        selectedTabIndex = categories.indexOf(selectedCategory),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        containerColor = Color.Transparent,
                        divider = {}, // Remove default divider
                        edgePadding = 16.dp // Tidy padding
                ) {
                        categories.forEachIndexed { index, category ->
                                Tab(
                                        selected = selectedCategory == category,
                                        onClick = { selectedCategory = category },
                                        text = {
                                                Text(
                                                        text = category.title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = if (selectedCategory == category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                        }
                                )
                        }
                }

                // Tier 2: Weather Carousel
                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .height(100.dp)
                                        .background(
                                                Color.Black.copy(alpha = 0.05f),
                                                RoundedCornerShape(16.dp)
                                        ),
                        contentAlignment = Alignment.Center
                ) {
                        // Focus indicator bracket
                        Box(
                                modifier =
                                        Modifier.size(80.dp)
                                                .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.5f
                                                        ),
                                                        RoundedCornerShape(20.dp)
                                                )
                        )

                        LazyRow(
                                state = listState,
                                flingBehavior = flingBehavior,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding =
                                        PaddingValues(
                                                horizontal = 140.dp
                                        ), // Adjust based on exact screen size for perfect centering
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                if (weathersInCategory.isNotEmpty()) {
                                        items(count = Int.MAX_VALUE) { index ->
                                                val actualIndex = index % weathersInCategory.size
                                                val weather = weathersInCategory[actualIndex]
                                                val isSelected = weather.name == selectedWeather
        
                                                Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier =
                                                                Modifier.clickable {
                                                                        onWeatherSelected(weather)
                                                                }
                                                ) {
                                                        HolographicWeatherIcon(
                                                                type = weather,
                                                                isActive = isSelected,
                                                                size = if (isSelected) 64.dp else 48.dp
                                                        )
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(
                                                                weather.label,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color =
                                                                        if (isSelected)
                                                                                MaterialTheme.colorScheme.onSurface
                                                                        else
                                                                                MaterialTheme.colorScheme
                                                                                        .onSurfaceVariant.copy(
                                                                                        alpha = 0.5f
                                                                                )
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}
