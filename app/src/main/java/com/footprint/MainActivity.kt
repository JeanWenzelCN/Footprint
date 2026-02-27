package com.footprint

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.WindowCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.EventChannel
import androidx.lifecycle.lifecycleScope
import com.google.gson.GsonBuilder
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : FlutterActivity() {
    private val CHANNEL_DATA = "com.footprint/data"
    private val CHANNEL_STREAM = "com.footprint/stream"
    
    private val EXPORT_REQUEST_CODE = 1001
    private val IMPORT_REQUEST_CODE = 1002
    
    private var pendingResult: MethodChannel.Result? = null

    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, com.google.gson.JsonSerializer<LocalDate> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.toString())
        })
        .registerTypeAdapter(LocalDate::class.java, com.google.gson.JsonDeserializer { json, _, _ ->
            LocalDate.parse(json.asString)
        })
        .create()

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        flutterEngine.platformViewsController.registry.registerViewFactory("com.footprint/amap", FlutterMapViewFactory(flutterEngine.dartExecutor.binaryMessenger))

        val repository = (application as FootprintApplication).repository
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_DATA).setMethodCallHandler { call, result ->
            when (call.method) {
                "getAllEntries" -> {
                    lifecycleScope.launch {
                        val entries = repository.getAllEntries()
                        result.success(gson.toJson(entries))
                    }
                }
                "getStats" -> {
                    lifecycleScope.launch {
                        val entries = repository.getAllEntries()
                        val stats = mapOf("totalDistance" to entries.sumOf { it.distanceKm }, "totalCount" to entries.size)
                        result.success(gson.toJson(stats))
                    }
                }
                "startTracking" -> { com.footprint.service.LocationTrackingService.startTracking(this); result.success(true) }
                "stopTracking" -> { com.footprint.service.LocationTrackingService.stopTracking(this); result.success(true) }
                "generateAIStory" -> {
                    lifecycleScope.launch {
                        val entries = repository.getAllEntries()
                        val lastEntry = entries.firstOrNull()
                        val story = if (lastEntry != null) com.footprint.utils.AIStoryGenerator.generateStory(lastEntry.location, lastEntry.mood, lastEntry.happenedOn) else "目前还没有足迹数据。"
                        result.success(story)
                    }
                }
                "getSettings" -> {
                    val prefs = com.footprint.utils.PreferenceManager(this)
                    val settings = mapOf("nickname" to prefs.nickname, "avatarId" to prefs.avatarId, "themeMode" to prefs.themeMode.name, "themeStyle" to prefs.themeStyle.name, "hapticEnabled" to prefs.hapticFeedbackEnabled)
                    result.success(gson.toJson(settings))
                }
                "updateNickname" -> { com.footprint.utils.PreferenceManager(this).nickname = call.arguments as String; result.success(true) }
                "updateAvatar" -> { com.footprint.utils.PreferenceManager(this).avatarId = call.arguments as String; result.success(true) }
                "updateThemeMode" -> { com.footprint.utils.PreferenceManager(this).themeMode = com.footprint.ui.theme.ThemeMode.valueOf(call.arguments as String); result.success(true) }
                "updateThemeStyle" -> { com.footprint.utils.PreferenceManager(this).themeStyle = com.footprint.ui.theme.AppThemeStyle.valueOf(call.arguments as String); result.success(true) }
                "updateHaptic" -> { com.footprint.utils.PreferenceManager(this).hapticFeedbackEnabled = call.arguments as Boolean; result.success(true) }
                
                // --- 1:1 实现备份与导入 ---
                "exportData" -> {
                    pendingResult = result
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        putExtra(Intent.EXTRA_TITLE, "footprint_backup_${System.currentTimeMillis()}.json")
                    }
                    startActivityForResult(intent, EXPORT_REQUEST_CODE)
                }
                "importData" -> {
                    pendingResult = result
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                    }
                    startActivityForResult(intent, IMPORT_REQUEST_CODE)
                }
                else -> result.notImplemented()
            }
        }
        
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_STREAM).setStreamHandler(
            object : EventChannel.StreamHandler {
                private var locationJob: Job? = null
                private var statusJob: Job? = null
                override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                    locationJob = lifecycleScope.launch { com.footprint.service.LocationTrackingService.currentLocation.collect { loc -> loc?.let { events.success(gson.toJson(mapOf("type" to "location", "data" to mapOf("latitude" to it.latitude, "longitude" to it.longitude, "address" to (it.address ?: ""))))) } } }
                    statusJob = lifecycleScope.launch { com.footprint.service.LocationTrackingService.isTracking.collect { isTracking -> events.success(gson.toJson(mapOf("type" to "status", "isTracking" to isTracking))) } }
                }
                override fun onCancel(arguments: Any?) { locationJob?.cancel(); statusJob?.cancel() }
            }
        )
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
                        val json = gson.toJson(backupData)
                        contentResolver.openOutputStream(uri)?.use { os ->
                            OutputStreamWriter(os).use { writer -> writer.write(json) }
                        }
                        withContext(Dispatchers.Main) { pendingResult?.success(true) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { pendingResult?.error("EXPORT_FAILED", e.message, null) }
                    }
                }
            }
            IMPORT_REQUEST_CODE -> {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        contentResolver.openInputStream(uri)?.use { `is` ->
                            InputStreamReader(`is`).use { reader ->
                                val backupData = gson.fromJson(reader, com.footprint.data.model.BackupData::class.java)
                                repository.restoreFromBackup(backupData)
                            }
                        }
                        withContext(Dispatchers.Main) { pendingResult?.success(true) }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { pendingResult?.error("IMPORT_FAILED", e.message, null) }
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
