package com.footprint.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.footprint.data.local.TrackPointEntity
import com.footprint.data.model.FootprintEntry
import com.footprint.ui.components.IconUtils
import com.footprint.ui.components.LiquidGlassCard
import com.footprint.ui.components.weather.HolographicWeatherIcon
import com.footprint.ui.components.weather.WeatherType
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootprintDetailScreen(
        entry: FootprintEntry,
        trackPoints: List<TrackPointEntity> = emptyList(),
        onBack: () -> Unit,
        onEdit: (FootprintEntry) -> Unit,
        onUpdateEntry: (FootprintEntry) -> Unit = {}
) {
        val context = LocalContext.current
        val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日") }
        var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }

        BackHandler {
                if (selectedPhotoIndex != null) {
                        selectedPhotoIndex = null
                } else {
                        onBack()
                }
        }

        Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                        topBar = {
                                TopAppBar(
                                        title = {},
                                        navigationIcon = {
                                                IconButton(onClick = onBack) {
                                                        Icon(
                                                                Icons.Default.ArrowBack,
                                                                contentDescription = "返回"
                                                        )
                                                }
                                        },
                                        actions = {
                                                IconButton(onClick = { onEdit(entry) }) {
                                                        Icon(
                                                                Icons.Default.Edit,
                                                                contentDescription = "编辑"
                                                        )
                                                }
                                        },
                                        colors =
                                                TopAppBarDefaults.topAppBarColors(
                                                        containerColor = Color.Transparent,
                                                        navigationIconContentColor =
                                                                MaterialTheme.colorScheme.onSurface,
                                                        actionIconContentColor =
                                                                MaterialTheme.colorScheme.onSurface
                                                )
                                )
                        },
                        containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                        LazyColumn(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .padding(
                                                        bottom =
                                                                innerPadding
                                                                        .calculateBottomPadding()
                                                )
                        ) {
                                // Hero Header
                                item {
                                        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                                                if (entry.photos.isNotEmpty()) {
                                                        AsyncImage(
                                                                model = entry.photos.first(),
                                                                contentDescription = null,
                                                                modifier =
                                                                        Modifier.fillMaxSize()
                                                                                .clickable {
                                                                                        selectedPhotoIndex =
                                                                                                0
                                                                                },
                                                                contentScale = ContentScale.Crop
                                                        )
                                                } else {
                                                        Box(
                                                                modifier =
                                                                        Modifier.fillMaxSize()
                                                                                .background(
                                                                                        Brush.verticalGradient(
                                                                                                listOf(
                                                                                                        entry.mood
                                                                                                                .color
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.6f
                                                                                                                ),
                                                                                                        entry.mood
                                                                                                                .color
                                                                                                )
                                                                                        )
                                                                                )
                                                        )
                                                }

                                                // Gradient Overlay
                                                Box(
                                                        modifier =
                                                                Modifier.fillMaxSize()
                                                                        .background(
                                                                                Brush.verticalGradient(
                                                                                        listOf(
                                                                                                Color.Transparent,
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .background
                                                                                        )
                                                                                )
                                                                        )
                                                )

                                                // Title and Meta in Hero
                                                Column(
                                                        modifier =
                                                                Modifier.align(
                                                                                Alignment
                                                                                        .BottomStart
                                                                        )
                                                                        .padding(24.dp)
                                                ) {
                                                        Surface(
                                                                color =
                                                                        entry.mood.color.copy(
                                                                                alpha = 0.2f
                                                                        ),
                                                                shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                                Text(
                                                                        text = entry.mood.label,
                                                                        modifier =
                                                                                Modifier.padding(
                                                                                        horizontal =
                                                                                                8.dp,
                                                                                        vertical =
                                                                                                4.dp
                                                                                ),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .labelMedium,
                                                                        color = entry.mood.color
                                                                )
                                                        }
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                                text = entry.title,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .headlineLarge,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically,
                                                                horizontalArrangement =
                                                                        Arrangement.spacedBy(8.dp)
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.LocationOn,
                                                                        contentDescription = null,
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        16.dp
                                                                                ),
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                                Text(
                                                                        text = entry.location,
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                                Text(
                                                                        text = " • ",
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .outline
                                                                )
                                                                Text(
                                                                        text =
                                                                                entry.happenedOn
                                                                                        .format(
                                                                                                dateFormatter
                                                                                        ),
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodyMedium,
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .outline
                                                                )
                                                        }
                                                        // Weather Icon (if exists)
                                                        if (!entry.weather.isNullOrEmpty()) {
                                                                val weatherType =
                                                                        WeatherType.values().find {
                                                                                it.name ==
                                                                                        entry.weather
                                                                        }
                                                                if (weatherType != null) {
                                                                        Spacer(
                                                                                modifier =
                                                                                        Modifier.height(
                                                                                                12.dp
                                                                                        )
                                                                        )
                                                                        Surface(
                                                                                color =
                                                                                        weatherType
                                                                                                .color
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.15f
                                                                                                ),
                                                                                shape =
                                                                                        RoundedCornerShape(
                                                                                                16.dp
                                                                                        )
                                                                        ) {
                                                                                Row(
                                                                                        verticalAlignment =
                                                                                                Alignment
                                                                                                        .CenterVertically,
                                                                                        modifier =
                                                                                                Modifier.padding(
                                                                                                        horizontal =
                                                                                                                12.dp,
                                                                                                        vertical =
                                                                                                                6.dp
                                                                                                )
                                                                                ) {
                                                                                        HolographicWeatherIcon(
                                                                                                type =
                                                                                                        weatherType,
                                                                                                size =
                                                                                                        20.dp,
                                                                                                isActive =
                                                                                                        true
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.width(
                                                                                                                8.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                text =
                                                                                                        weatherType
                                                                                                                .label,
                                                                                                style =
                                                                                                        MaterialTheme
                                                                                                                .typography
                                                                                                                .labelLarge,
                                                                                                color =
                                                                                                        weatherType
                                                                                                                .color
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                }
                                        }
                                }

                                // Quick Stats
                                item {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                                StatDrop(
                                                        label = "里程",
                                                        value =
                                                                "${String.format("%.1f", entry.distanceKm)} KM",
                                                        iconContent = {
                                                                com.footprint.ui.components.HolographicMileageIcon(
                                                                        distanceKm = entry.distanceKm,
                                                                        size = 24.dp
                                                                )
                                                        }
                                                )
                                                StatDrop(
                                                        label = "能量",
                                                        value = "${entry.energyLevel}",
                                                        iconContent = {
                                                                com.footprint.ui.components.HolographicEnergyIcon(
                                                                        energyLevel = entry.energyLevel,
                                                                        size = 24.dp
                                                                )
                                                        }
                                                )
                                                val weatherType =
                                                        WeatherType.values().find {
                                                                it.name == entry.weather
                                                        }
                                                val weatherLabel = weatherType?.label ?: "未知"
                                                StatDrop(
                                                        label = "天气",
                                                        value = weatherLabel,
                                                        iconContent = {
                                                                if (weatherType != null) {
                                                                        HolographicWeatherIcon(
                                                                                type = weatherType,
                                                                                size = 24.dp,
                                                                                isActive = true
                                                                        )
                                                                } else {
                                                                        Icon(
                                                                                Icons.Outlined
                                                                                        .Cloud,
                                                                                contentDescription =
                                                                                        null,
                                                                                modifier =
                                                                                        Modifier.size(
                                                                                                24.dp
                                                                                        ),
                                                                                tint =
                                                                                        MaterialTheme
                                                                                                .colorScheme
                                                                                                .primary
                                                                        )
                                                                }
                                                        }
                                                )
                                                StatDrop(
                                                        label = "心情",
                                                        value = entry.mood.label,
                                                        iconContent = {
                                                                com.footprint.ui.components.HolographicMoodIcon(
                                                                        mood = entry.mood,
                                                                        size = 24.dp
                                                                )
                                                        }
                                                )
                                        }
                                }

                                // Article Content
                                item {
                                        Text(
                                                text = entry.detail,
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(horizontal = 24.dp),
                                                style =
                                                        MaterialTheme.typography.bodyLarge.copy(
                                                                lineHeight = 28.sp,
                                                                letterSpacing = 0.5.sp
                                                        ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                }

                                // Photo Gallery
                                if (entry.photos.isNotEmpty()) {
                                        item {
                                                Text(
                                                        text = "瞬间",
                                                        modifier =
                                                                Modifier.padding(
                                                                        start = 24.dp,
                                                                        top = 32.dp,
                                                                        bottom = 12.dp
                                                                ),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold
                                                )
                                                LazyRow(
                                                        contentPadding =
                                                                PaddingValues(horizontal = 24.dp),
                                                        horizontalArrangement =
                                                                Arrangement.spacedBy(12.dp),
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .height(200.dp)
                                                ) {
                                                        items(entry.photos.size) { index ->
                                                                Box {
                                                                        AsyncImage(
                                                                                model =
                                                                                        entry.photos[
                                                                                                index],
                                                                                contentDescription =
                                                                                        null,
                                                                                modifier =
                                                                                        Modifier.width(
                                                                                                        150.dp
                                                                                                )
                                                                                                .fillMaxHeight()
                                                                                                .clip(
                                                                                                        RoundedCornerShape(
                                                                                                                16.dp
                                                                                                        )
                                                                                                )
                                                                                                .clickable {
                                                                                                        selectedPhotoIndex =
                                                                                                                index
                                                                                                },
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
                                                                                                        topStart =
                                                                                                                16.dp,
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
                                                                                                                        8.dp,
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
                                                                }
                                                        }
                                                }
                                        }
                                }

                                // Track Map (If applicable)
                                item {
                                        Column(modifier = Modifier.padding(24.dp)) {
                                                Text(
                                                        text = "足迹轨迹",
                                                        modifier = Modifier.padding(bottom = 12.dp),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold
                                                )
                                                LiquidGlassCard(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .height(250.dp),
                                                        shape = RoundedCornerShape(24.dp)
                                                ) {
                                                        Box(modifier = Modifier.fillMaxSize()) {
                                                                AndroidView(
                                                                        factory = { ctx ->
                                                                                MapView(ctx).apply {
                                                                                        onCreate(
                                                                                                null
                                                                                        )
                                                                                        val amap =
                                                                                                map
                                                                                        amap.uiSettings
                                                                                                .isZoomControlsEnabled =
                                                                                                false
                                                                                        amap.uiSettings
                                                                                                .isMyLocationButtonEnabled =
                                                                                                false

                                                                                        // Set dark
                                                                                        // mode if
                                                                                        // needed
                                                                                        val isDark =
                                                                                                true
                                                                                        amap.mapType =
                                                                                                if (isDark
                                                                                                )
                                                                                                        AMap.MAP_TYPE_NIGHT
                                                                                                else
                                                                                                        AMap.MAP_TYPE_NORMAL

                                                                                        // Draw
                                                                                        // marker
                                                                                        if (entry.latitude !=
                                                                                                        null &&
                                                                                                        entry.longitude !=
                                                                                                                null
                                                                                        ) {
                                                                                                val pos =
                                                                                                        LatLng(
                                                                                                                entry.latitude,
                                                                                                                entry.longitude
                                                                                                        )
                                                                                                amap.addMarker(
                                                                                                        MarkerOptions()
                                                                                                                .position(
                                                                                                                        pos
                                                                                                                )
                                                                                                                .title(
                                                                                                                        entry.title
                                                                                                                )
                                                                                                )
                                                                                                amap.moveCamera(
                                                                                                        CameraUpdateFactory
                                                                                                                .newLatLngZoom(
                                                                                                                        pos,
                                                                                                                        15f
                                                                                                                )
                                                                                                )
                                                                                        }

                                                                                        // Draw
                                                                                        // track
                                                                                        if (trackPoints
                                                                                                        .isNotEmpty()
                                                                                        ) {
                                                                                                val path =
                                                                                                        trackPoints
                                                                                                                .map {
                                                                                                                        LatLng(
                                                                                                                                it.latitude,
                                                                                                                                it.longitude
                                                                                                                        )
                                                                                                                }
                                                                                                amap.addPolyline(
                                                                                                        PolylineOptions()
                                                                                                                .addAll(
                                                                                                                        path
                                                                                                                )
                                                                                                                .width(
                                                                                                                        12f
                                                                                                                )
                                                                                                                .color(
                                                                                                                        android.graphics
                                                                                                                                .Color
                                                                                                                                .parseColor(
                                                                                                                                        "#00FF9F"
                                                                                                                                )
                                                                                                                )
                                                                                                )

                                                                                                // Zoom to fit track
                                                                                                val boundsBuilder =
                                                                                                        LatLngBounds
                                                                                                                .Builder()
                                                                                                path
                                                                                                        .forEach {
                                                                                                                boundsBuilder
                                                                                                                        .include(
                                                                                                                                it
                                                                                                                        )
                                                                                                        }
                                                                                                amap.moveCamera(
                                                                                                        CameraUpdateFactory
                                                                                                                .newLatLngBounds(
                                                                                                                        boundsBuilder
                                                                                                                                .build(),
                                                                                                                        50
                                                                                                                )
                                                                                                )
                                                                                        }
                                                                                }
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxSize(),
                                                                        update = { mv ->
                                                                                mv.onResume()
                                                                        }
                                                                )
                                                        }
                                                }
                                        }
                                }

                                item { Spacer(modifier = Modifier.height(48.dp)) }
                        }
                }

                // Image Viewer Overlay
                AnimatedVisibility(
                        visible = selectedPhotoIndex != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                ) {
                        val index = selectedPhotoIndex ?: 0
                        val photo = if (index < entry.photos.size) entry.photos[index] else ""

                        Box(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.95f))
                                                .clickable { selectedPhotoIndex = null }
                        ) {
                                AsyncImage(
                                        model = photo,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                )

                                // Viewer Controls
                                Column(
                                        modifier =
                                                Modifier.align(Alignment.BottomCenter)
                                                        .padding(bottom = 48.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        if (index != 0) {
                                                Button(
                                                        onClick = {
                                                                val newPhotos =
                                                                        entry.photos.toMutableList()
                                                                val current =
                                                                        newPhotos.removeAt(index)
                                                                newPhotos.add(0, current)
                                                                onUpdateEntry(
                                                                        entry.copy(
                                                                                photos = newPhotos
                                                                        )
                                                                )
                                                                selectedPhotoIndex = 0
                                                        },
                                                        colors =
                                                                ButtonDefaults.buttonColors(
                                                                        containerColor =
                                                                                Color.White.copy(
                                                                                        alpha = 0.2f
                                                                                ),
                                                                        contentColor = Color.White
                                                                ),
                                                        shape = RoundedCornerShape(12.dp)
                                                ) {
                                                        Icon(
                                                                Icons.Default.PhotoSizeSelectActual,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text("设为封面")
                                                }
                                        } else {
                                                Surface(
                                                        color =
                                                                MaterialTheme.colorScheme.primary
                                                                        .copy(alpha = 0.3f),
                                                        shape = RoundedCornerShape(12.dp)
                                                ) {
                                                        Row(
                                                                modifier =
                                                                        Modifier.padding(
                                                                                horizontal = 16.dp,
                                                                                vertical = 8.dp
                                                                        ),
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Icon(
                                                                        Icons.Default.Check,
                                                                        contentDescription = null,
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary,
                                                                        modifier =
                                                                                Modifier.size(18.dp)
                                                                )
                                                                Spacer(Modifier.width(8.dp))
                                                                Text("当前封面", color = Color.White)
                                                        }
                                                }
                                        }

                                        Spacer(Modifier.height(16.dp))

                                        Text(
                                                text = "${index + 1} / ${entry.photos.size}",
                                                color = Color.White.copy(alpha = 0.6f),
                                                style = MaterialTheme.typography.bodyMedium
                                        )
                                }

                                IconButton(
                                        onClick = { selectedPhotoIndex = null },
                                        modifier =
                                                Modifier.align(Alignment.TopEnd)
                                                        .padding(top = 48.dp, end = 24.dp)
                                                        .background(
                                                                Color.White.copy(alpha = 0.1f),
                                                                CircleShape
                                                        )
                                ) {
                                        Icon(
                                                Icons.Default.Close,
                                                contentDescription = "关闭",
                                                tint = Color.White
                                        )
                                }
                        }
                }
        }
}

@Composable
private fun StatDrop(label: String, value: String, iconContent: @Composable () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                        modifier =
                                Modifier.size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                ) { iconContent() }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                )
                Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                )
        }
}

@Composable
private fun StatDrop(
        label: String,
        value: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector
) {
        StatDrop(label, value) {
                Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                )
        }
}
