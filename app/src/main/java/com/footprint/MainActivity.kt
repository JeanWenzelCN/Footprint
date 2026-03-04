package com.footprint

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import java.io.InputStreamReader
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FlutterActivity() {
    private val CHANNEL_DATA = "com.footprint/data"
    private val CHANNEL_STREAM = "com.footprint/stream"

    private val EXPORT_REQUEST_CODE = 1001
    private val IMPORT_REQUEST_CODE = 1002

    private var pendingResult: MethodChannel.Result? = null

    private val gson =
            GsonBuilder()
                    .registerTypeAdapter(
                            LocalDate::class.java,
                            com.google.gson.JsonSerializer<LocalDate> { src, _, _ ->
                                com.google.gson.JsonPrimitive(src.toString())
                            }
                    )
                    .registerTypeAdapter(
                            LocalDate::class.java,
                            com.google.gson.JsonDeserializer { json, _, _ ->
                                LocalDate.parse(json.asString)
                            }
                    )
                    .create()

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        flutterEngine.platformViewsController.registry.registerViewFactory(
                "com.footprint/amap",
                FlutterMapViewFactory(flutterEngine.dartExecutor.binaryMessenger)
        )

        val repository = (application as FootprintApplication).repository

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_DATA)
                .setMethodCallHandler { call, result ->
                    when (call.method) {
                        "getAllEntries" -> {
                            lifecycleScope.launch {
                                val entries =
                                        withContext(Dispatchers.IO) { repository.getAllEntries() }
                                val json = withContext(Dispatchers.Default) { gson.toJson(entries) }
                                result.success(json)
                            }
                        }
                        "getAllGoals" -> {
                            lifecycleScope.launch {
                                val goals = withContext(Dispatchers.IO) { repository.getAllGoals() }
                                val json = withContext(Dispatchers.Default) { gson.toJson(goals) }
                                result.success(json)
                            }
                        }
                        "getStats" -> {
                            lifecycleScope.launch {
                                val entries =
                                        withContext(Dispatchers.IO) { repository.getAllEntries() }
                                val stats =
                                        withContext(Dispatchers.Default) {
                                            mapOf(
                                                    "totalDistance" to
                                                            entries.sumOf { it.distanceKm },
                                                    "totalCount" to entries.size
                                            )
                                        }
                                result.success(gson.toJson(stats))
                            }
                        }
                        "getTrackPoints" -> {
                            val args = call.arguments as Map<String, Any>
                            val startTime = (args["startTime"] as Number).toLong()
                            val endTime = (args["endTime"] as Number).toLong()
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val points = repository.getTrackPointsOnce(startTime, endTime)
                                    val list =
                                            points.map {
                                                mapOf(
                                                        "latitude" to it.latitude,
                                                        "longitude" to it.longitude,
                                                        "timestamp" to it.timestamp
                                                )
                                            }
                                    val json = gson.toJson(list)
                                    withContext(Dispatchers.Main) { result.success(json) }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        result.error("QUERY_FAILED", e.message, null)
                                    }
                                }
                            }
                        }
                        // 获取所有去重位置点（用于迷雾探索掩码）
                        "getAllFogPoints" -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    // 1. 从 track_points 表获取去重后的坐标
                                    val trackLocations = repository.getAllDistinctLocations()
                                    // 2. 从 footprints 表获取足迹坐标
                                    val entries = repository.getAllEntries()
                                    
                                    // 合并两个数据源
                                    val allPoints = mutableListOf<Map<String, Double>>()
                                    trackLocations.forEach {
                                        allPoints.add(mapOf("lat" to it.latitude, "lng" to it.longitude))
                                    }
                                    entries.filter { it.latitude != null && it.longitude != null }.forEach {
                                        allPoints.add(mapOf("lat" to it.latitude!!, "lng" to it.longitude!!))
                                    }
                                    
                                    val json = gson.toJson(allPoints)
                                    withContext(Dispatchers.Main) { result.success(json) }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        result.error("QUERY_FAILED", e.message, null)
                                    }
                                }
                            }
                        }
                        "getTrackingState" -> {
                            val service = com.footprint.service.LocationTrackingService
                            val state =
                                    mapOf(
                                            "isTracking" to service.isTracking.value,
                                            "totalDistance" to service.totalDistance.value,
                                            "sessionStartTime" to service.sessionStartTime,
                                            "path" to
                                                    service.trackingPath.value.map {
                                                        mapOf(
                                                                "latitude" to it.latitude,
                                                                "longitude" to it.longitude
                                                        )
                                                    }
                                    )
                            result.success(gson.toJson(state))
                        }
                        "startTracking" -> {
                            com.footprint.service.LocationTrackingService.startTracking(
                                    this@MainActivity
                            )
                            result.success(true)
                        }
                        "stopTracking" -> {
                            com.footprint.service.LocationTrackingService.stopTracking(
                                    this@MainActivity
                            )
                            result.success(true)
                        }
                        "openUrl" -> {
                            val url = call.arguments as? String
                            if (url != null) {
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                startActivity(intent)
                                result.success(true)
                            } else {
                                result.error("INVALID_URL", "URL is null", null)
                            }
                        }
                        "generateAIStory" -> {
                            lifecycleScope.launch {
                                val entries = repository.getAllEntries()
                                val lastEntry = entries.firstOrNull()
                                val story =
                                        if (lastEntry != null)
                                                com.footprint.utils.AIStoryGenerator.generateStory(
                                                        lastEntry.location,
                                                        lastEntry.mood,
                                                        lastEntry.happenedOn
                                                )
                                        else "目前还没有足迹数据。"
                                result.success(story)
                            }
                        }
                        "getSettings" -> {
                            val prefs = com.footprint.utils.PreferenceManager(this)
                            val settings =
                                    mapOf(
                                            "nickname" to prefs.nickname,
                                            "avatarId" to prefs.avatarId,
                                            "themeMode" to prefs.themeMode.name,
                                            "themeStyle" to prefs.themeStyle.name,
                                            "hapticEnabled" to prefs.hapticFeedbackEnabled
                                    )
                            result.success(gson.toJson(settings))
                        }
                        "updateNickname" -> {
                            com.footprint.utils.PreferenceManager(this).nickname =
                                    call.arguments as String
                            result.success(true)
                        }
                        "updateAvatar" -> {
                            com.footprint.utils.PreferenceManager(this).avatarId =
                                    call.arguments as String
                            result.success(true)
                        }
                        "updateThemeMode" -> {
                            com.footprint.utils.PreferenceManager(this).themeMode =
                                    com.footprint.ui.theme.ThemeMode.valueOf(
                                            call.arguments as String
                                    )
                            result.success(true)
                        }
                        "updateThemeStyle" -> {
                            com.footprint.utils.PreferenceManager(this).themeStyle =
                                    com.footprint.ui.theme.AppThemeStyle.valueOf(
                                            call.arguments as String
                                    )
                            result.success(true)
                        }
                        "updateHaptic" -> {
                            com.footprint.utils.PreferenceManager(this).hapticFeedbackEnabled =
                                    call.arguments as Boolean
                            result.success(true)
                        }
                        "getAppCredentials" -> {
                            val map =
                                    mapOf(
                                            "packageName" to packageName,
                                            "sha1" to
                                                    com.footprint.utils.AppUtils.getAppSignature(
                                                            this@MainActivity
                                                    ),
                                            "amapKey" to
                                                    (com.footprint.utils.ApiKeyManager.getApiKey(
                                                            this@MainActivity
                                                    )
                                                            ?: "")
                                    )
                            result.success(gson.toJson(map))
                        }
                        "saveAmapKey" -> {
                            val key = call.arguments as String
                            com.footprint.utils.ApiKeyManager.setApiKey(this@MainActivity, key)
                            result.success(true)
                        }

                        // --- 1:1 实现备份与导入 ---
                        "exportData" -> {
                            pendingResult = result
                            val intent =
                                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                        putExtra(
                                                Intent.EXTRA_TITLE,
                                                "footprint_backup_${System.currentTimeMillis()}.zip"
                                        )
                                    }
                            startActivityForResult(intent, EXPORT_REQUEST_CODE)
                        }
                        "importData" -> {
                            pendingResult = result
                            val intent =
                                    Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "*/*"
                                    }
                            startActivityForResult(intent, IMPORT_REQUEST_CODE)
                        }

                        // --- Flutter 追踪：保存单个轨迹点 ---
                        "saveTrackPoint" -> {
                            val args = call.arguments as Map<String, Any>
                            val lat = (args["latitude"] as Number).toDouble()
                            val lng = (args["longitude"] as Number).toDouble()
                            val alt = (args["altitude"] as Number).toDouble()
                            val acc = (args["accuracy"] as Number).toFloat()
                            val spd = (args["speed"] as Number).toFloat()
                            val ts = (args["timestamp"] as Number).toLong()
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    repository.saveTrackPointRaw(lat, lng, alt, acc, spd, ts)
                                    withContext(Dispatchers.Main) { result.success(true) }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        result.error("SAVE_FAILED", e.message, null)
                                    }
                                }
                            }
                        }
                        // --- Flutter 追踪：保存完整追踪会话为足迹记录 ---
                        "saveTrackingSession" -> {
                            val args = call.arguments as Map<String, Any>
                            val totalDistanceM = (args["totalDistanceM"] as Number).toDouble()
                            val pointCount = (args["pointCount"] as Number).toInt()
                            val address = args["address"] as? String ?: "未知地点"
                            val lat = (args["latitude"] as? Number)?.toDouble()
                            val lng = (args["longitude"] as? Number)?.toDouble()
                            val alt = (args["altitude"] as? Number)?.toDouble()
                            val durationMin = (args["durationMinutes"] as Number).toInt()
                            val entry =
                                    com.footprint.data.model.FootprintEntry(
                                            title = "自动追踪",
                                            location = address.ifEmpty { "未知地点" },
                                            detail = "通过自动追踪记录：共 $pointCount 个点，耗时 $durationMin 分钟",
                                            mood = com.footprint.data.model.Mood.RELAXED,
                                            tags = listOf("自动追踪"),
                                            distanceKm = totalDistanceM / 1000.0,
                                            photos = emptyList(),
                                            energyLevel = 5,
                                            happenedOn = java.time.LocalDate.now(),
                                            latitude = lat,
                                            longitude = lng,
                                            altitude = alt,
                                            weather = null,
                                            temperature = null,
                                            transportType =
                                                    com.footprint.data.model.TransportType.WALK,
                                            carbonSavedKg = 0.0,
                                            icon = "RunCircle"
                                    )
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    repository.saveEntry(entry)
                                    withContext(Dispatchers.Main) { result.success(true) }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        result.error("SAVE_FAILED", e.message, null)
                                    }
                                }
                            }
                        }
                        "openNativeScreen" -> {
                            val args = call.arguments as Map<String, Any>
                            val screenType = args["screen_type"] as String
                            val intent =
                                    Intent(
                                                    this@MainActivity,
                                                    com.footprint.ui.NativeScreenActivity::class
                                                            .java
                                            )
                                            .apply {
                                                putExtra("screen_type", screenType)
                                                (args["initial_year"] as? Number)?.let {
                                                    putExtra("initial_year", it.toInt())
                                                }
                                            }
                            startActivity(intent)
                            result.success(true)
                        }
                        "saveFootprint" -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val json = call.arguments as String
                                    val entry =
                                            gson.fromJson(
                                                    json,
                                                    com.footprint.data.model.FootprintEntry::class
                                                            .java
                                            )

                                    val processedPhotos =
                                            entry.photos?.mapNotNull { path ->
                                                try {
                                                    val originalFile = java.io.File(path)
                                                    if (!originalFile.exists())
                                                            return@mapNotNull path

                                                    val appImagesDir =
                                                            java.io.File(
                                                                    filesDir,
                                                                    "footprint_images"
                                                            )
                                                    if (!appImagesDir.exists())
                                                            appImagesDir.mkdirs()

                                                    if (path.startsWith(appImagesDir.absolutePath)
                                                    ) {
                                                        path
                                                    } else {
                                                        val destFile =
                                                                java.io.File(
                                                                        appImagesDir,
                                                                        "footprint_${java.util.UUID.randomUUID()}.jpg"
                                                                )
                                                        originalFile.copyTo(
                                                                destFile,
                                                                overwrite = true
                                                        )
                                                        destFile.absolutePath
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    path
                                                }
                                            }
                                                    ?: emptyList()
                                    val finalEntry = entry.copy(photos = processedPhotos)

                                    repository.saveEntry(finalEntry)
                                    withContext(Dispatchers.Main) { result.success(true) }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        result.error("SAVE_FAILED", e.message, null)
                                    }
                                }
                            }
                        }
                        "deleteFootprint" -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val id = (call.arguments as Number).toLong()
                                    repository.deleteEntry(id)
                                    withContext(Dispatchers.Main) { result.success(true) }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        result.error("DELETE_FAILED", e.message, null)
                                    }
                                }
                            }
                        }
                        "saveGoal" -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val json = call.arguments as String
                                    val goal =
                                            gson.fromJson(
                                                    json,
                                                    com.footprint.data.model.TravelGoal::class.java
                                            )
                                    repository.saveGoal(goal)
                                    withContext(Dispatchers.Main) { result.success(true) }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        result.error("SAVE_FAILED", e.message, null)
                                    }
                                }
                            }
                        }
                        "deleteGoal" -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val id = (call.arguments as Number).toLong()
                                    repository.deleteGoal(id)
                                    withContext(Dispatchers.Main) { result.success(true) }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        result.error("DELETE_FAILED", e.message, null)
                                    }
                                }
                            }
                        }
                        "getUnlockedBadgeIds" -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val db = com.footprint.data.local.FootprintDatabase.getInstance(this@MainActivity)
                                    val ids = db.userBadgesDao().getAllUnlockedBadgeIds()
                                    withContext(Dispatchers.Main) { result.success(gson.toJson(ids)) }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { result.error("DB_ERROR", e.message, null) }
                                }
                            }
                        }
                        "getBadgeDictionary" -> {
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    val input = assets.open("badges_config.json")
                                    val jsonString = input.bufferedReader().use { it.readText() }
                                    withContext(Dispatchers.Main) { result.success(jsonString) }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) { result.error("IO_ERROR", e.message, null) }
                                }
                            }
                        }
                        else -> result.notImplemented()
                    }
                }

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_STREAM)
                .setStreamHandler(
                        object : EventChannel.StreamHandler {
                            private var locationJob: Job? = null
                            private var statusJob: Job? = null
                            private var distanceJob: Job? = null

                            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                                locationJob =
                                        lifecycleScope.launch {
                                            com.footprint.service.LocationTrackingService
                                                    .currentLocation
                                                    .collect { loc ->
                                                        loc?.let {
                                                            events.success(
                                                                    gson.toJson(
                                                                            mapOf(
                                                                                    "type" to
                                                                                            "location",
                                                                                    "data" to
                                                                                            mapOf(
                                                                                                    "latitude" to
                                                                                                            it.latitude,
                                                                                                    "longitude" to
                                                                                                            it.longitude,
                                                                                                    "address" to
                                                                                                            (it.address
                                                                                                                    ?: "")
                                                                                            )
                                                                            )
                                                                    )
                                                            )
                                                        }
                                                    }
                                        }
                                statusJob =
                                        lifecycleScope.launch {
                                            com.footprint.service.LocationTrackingService.isTracking
                                                    .collect { isTracking ->
                                                        events.success(
                                                                gson.toJson(
                                                                        mapOf(
                                                                                "type" to "status",
                                                                                "isTracking" to
                                                                                        isTracking
                                                                        )
                                                                )
                                                        )
                                                    }
                                        }
                                distanceJob =
                                        lifecycleScope.launch {
                                            com.footprint.service.LocationTrackingService
                                                    .totalDistance
                                                    .collect { distance ->
                                                        events.success(
                                                                gson.toJson(
                                                                        mapOf(
                                                                                "type" to
                                                                                        "distance",
                                                                                "distance" to
                                                                                        distance
                                                                        )
                                                                )
                                                        )
                                                    }
                                        }
                            }
                            override fun onCancel(arguments: Any?) {
                                locationJob?.cancel()
                                statusJob?.cancel()
                                distanceJob?.cancel()
                            }
                        }
                )

        EventChannel(flutterEngine.dartExecutor.binaryMessenger, "com.footprint/badge_events")
                .setStreamHandler(
                        object : EventChannel.StreamHandler {
                            private var badgeJob: Job? = null
                            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                                badgeJob = lifecycleScope.launch {
                                    val app = application as FootprintApplication
                                    app.badgeEngine.badgeEvents.collect { event ->
                                        events.success(
                                            gson.toJson(
                                                mapOf(
                                                    "badgeId" to event.badgeId,
                                                    "title" to event.title,
                                                    "description" to event.description,
                                                    "icon" to event.icon
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                            override fun onCancel(arguments: Any?) {
                                badgeJob?.cancel()
                            }
                        }
                )

        // ── Badge Poster Compositing Channel ──
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.footprint/badge_poster")
                .setMethodCallHandler { call, result ->
                    when (call.method) {
                        "composePoster" -> {
                            val args = call.arguments as Map<String, Any>
                            val pngBytes = args["badge_png_bytes"] as ByteArray
                            val title = args["badge_title"] as? String ?: ""
                            val colorHex = args["badge_color"] as? String ?: "#FFFFFF"
                            val material = args["material_type"] as? String ?: "Base"

                            lifecycleScope.launch(Dispatchers.IO) {
                                val path = com.footprint.badge.BadgePosterCompositor.compose(
                                    context = this@MainActivity,
                                    badgePngBytes = pngBytes,
                                    badgeTitle = title,
                                    badgeColorHex = colorHex,
                                    materialType = material,
                                )
                                withContext(Dispatchers.Main) {
                                    if (path != null) result.success(path)
                                    else result.error("COMPOSE_FAILED", "海报合成失败", null)
                                }
                            }
                        }
                        "sharePoster" -> {
                            val args = call.arguments as Map<String, Any>
                            val path = args["path"] as? String
                            if (path != null) {
                                com.footprint.badge.BadgePosterCompositor.share(this@MainActivity, path)
                                result.success(true)
                            } else {
                                result.error("NO_PATH", "海报路径为空", null)
                            }
                        }
                        else -> result.notImplemented()
                    }
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) {
            pendingResult?.error("CANCELLED", "User cancelled the operation", null)
            return
        }

        val uri = data.data ?: return
        val repository = (application as FootprintApplication).repository

        when (requestCode) {
            EXPORT_REQUEST_CODE -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val backupData = repository.prepareBackup()

                        val tempDir =
                                java.io.File(cacheDir, "backup_temp_${System.currentTimeMillis()}")
                        if (tempDir.exists())
                                com.footprint.utils.FileUtils.deleteRecursively(tempDir)
                        tempDir.mkdirs()
                        val imagesDir = java.io.File(tempDir, "images")
                        imagesDir.mkdirs()

                        // Copy images and remap paths
                        val processedFootprints =
                                backupData.footprints.map { footprint ->
                                    val newPhotos =
                                            footprint.photos.mapNotNull { photoPath ->
                                                try {
                                                    val originalFile = java.io.File(photoPath)
                                                    if (originalFile.exists()) {
                                                        val destFile =
                                                                java.io.File(
                                                                        imagesDir,
                                                                        originalFile.name
                                                                )
                                                        com.footprint.utils.FileUtils.copyFile(
                                                                originalFile,
                                                                destFile
                                                        )
                                                        "images/${originalFile.name}"
                                                    } else null
                                                } catch (_: Exception) {
                                                    null
                                                }
                                            }
                                    footprint.copy(photos = newPhotos)
                                }

                        val processedBackup = backupData.copy(footprints = processedFootprints)
                        val json = gson.toJson(processedBackup)

                        val jsonFile = java.io.File(tempDir, "backup_data.json")
                        java.io.FileWriter(jsonFile).use { it.write(json) }

                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            com.footprint.utils.FileUtils.zipDirectory(tempDir, outputStream)
                        }

                        com.footprint.utils.FileUtils.deleteRecursively(tempDir)

                        withContext(Dispatchers.Main) { pendingResult?.success(true) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            pendingResult?.error("EXPORT_FAILED", e.message ?: "导出失败", null)
                        }
                    }
                }
            }
            IMPORT_REQUEST_CODE -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val tempDir =
                                java.io.File(cacheDir, "import_temp_${System.currentTimeMillis()}")
                        if (tempDir.exists())
                                com.footprint.utils.FileUtils.deleteRecursively(tempDir)
                        tempDir.mkdirs()

                        var isZip = false
                        try {
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                try {
                                    com.footprint.utils.FileUtils.unzip(inputStream, tempDir)
                                    if (tempDir.listFiles()?.isNotEmpty() == true) {
                                        isZip = true
                                    }
                                } catch (_: Exception) {
                                    isZip = false
                                }
                            }
                        } catch (_: Exception) {
                            isZip = false
                        }

                        val backup: com.footprint.data.model.BackupData

                        if (isZip) {
                            val jsonFile = java.io.File(tempDir, "backup_data.json")
                            if (jsonFile.exists()) {
                                val json = java.io.FileReader(jsonFile).use { it.readText() }
                                backup =
                                        gson.fromJson(
                                                json,
                                                com.footprint.data.model.BackupData::class.java
                                        )
                            } else {
                                isZip = false
                                val json =
                                        contentResolver.openInputStream(uri)?.use {
                                            InputStreamReader(it).use { reader ->
                                                reader.readText()
                                            }
                                        }
                                                ?: throw Exception("无法读取文件")
                                backup =
                                        gson.fromJson(
                                                json,
                                                com.footprint.data.model.BackupData::class.java
                                        )
                            }
                        } else {
                            val json =
                                    contentResolver.openInputStream(uri)?.use {
                                        InputStreamReader(it).use { reader -> reader.readText() }
                                    }
                                            ?: throw Exception("无法读取文件")
                            backup =
                                    gson.fromJson(
                                            json,
                                            com.footprint.data.model.BackupData::class.java
                                    )
                        }

                        // Restore images from ZIP
                        val restoredFootprints =
                                if (isZip) {
                                    val appImagesDir = java.io.File(filesDir, "footprint_images")
                                    if (!appImagesDir.exists()) appImagesDir.mkdirs()

                                    backup.footprints.map { footprint ->
                                        val restoredPhotos =
                                                footprint.photos.map { relativePath ->
                                                    if (relativePath.startsWith("images/")) {
                                                        val imageName =
                                                                relativePath.substringAfter(
                                                                        "images/"
                                                                )
                                                        val sourceFile =
                                                                java.io.File(tempDir, relativePath)
                                                        if (sourceFile.exists()) {
                                                            val destFile =
                                                                    java.io.File(
                                                                            appImagesDir,
                                                                            imageName
                                                                    )
                                                            com.footprint.utils.FileUtils.copyFile(
                                                                    sourceFile,
                                                                    destFile
                                                            )
                                                            destFile.absolutePath
                                                        } else {
                                                            relativePath
                                                        }
                                                    } else relativePath
                                                }
                                        footprint.copy(photos = restoredPhotos)
                                    }
                                } else {
                                    backup.footprints
                                }

                        // Generate track points from entries if missing
                        val finalTrackPoints =
                                if (backup.trackPoints.isEmpty()) {
                                    restoredFootprints
                                            .filter { it.latitude != null && it.longitude != null }
                                            .map { fp ->
                                                com.footprint.data.local.TrackPointEntity(
                                                        latitude = fp.latitude!!,
                                                        longitude = fp.longitude!!,
                                                        timestamp =
                                                                fp.happenedOn
                                                                        .atStartOfDay(
                                                                                java.time.ZoneOffset
                                                                                        .UTC
                                                                        )
                                                                        .toInstant()
                                                                        .toEpochMilli(),
                                                        speed = 0f,
                                                        accuracy = 0f,
                                                        altitude = fp.altitude ?: 0.0
                                                )
                                            }
                                } else {
                                    backup.trackPoints
                                }

                        val finalBackup =
                                backup.copy(
                                        footprints = restoredFootprints,
                                        trackPoints = finalTrackPoints
                                )
                        repository.restoreFromBackup(finalBackup)

                        // Cleanup temp
                        com.footprint.utils.FileUtils.deleteRecursively(tempDir)

                        withContext(Dispatchers.Main) { pendingResult?.success(true) }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            pendingResult?.error("IMPORT_FAILED", e.message ?: "导入失败", null)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}
