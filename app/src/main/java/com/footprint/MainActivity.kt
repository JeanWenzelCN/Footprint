package com.footprint

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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job

class MainActivity : FlutterActivity() {
    private val CHANNEL_DATA = "com.footprint/data"
    private val CHANNEL_STREAM = "com.footprint/stream"
    
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
        
        // --- 注册高德地图原生视图 ---
        flutterEngine
            .platformViewsController
            .registry
            .registerViewFactory("com.footprint/amap", FlutterMapViewFactory(flutterEngine.dartExecutor.binaryMessenger))

        val repository = (application as FootprintApplication).repository
        
        // --- 1. 业务逻辑请求 (MethodChannel) ---
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_DATA).setMethodCallHandler { call, result ->
            when (call.method) {
                "getAllEntries" -> {
                    lifecycleScope.launch {
                        val entries: List<com.footprint.data.model.FootprintEntry> = repository.getAllEntries()
                        result.success(gson.toJson(entries))
                    }
                }
                "getStats" -> {
                    lifecycleScope.launch {
                        val entries: List<com.footprint.data.model.FootprintEntry> = repository.getAllEntries()
                        val stats: Map<String, Any> = mapOf(
                            "totalDistance" to entries.sumOf { it.distanceKm },
                            "totalCount" to entries.size
                        )
                        result.success(gson.toJson(stats))
                    }
                }
                "startTracking" -> {
                    com.footprint.service.LocationTrackingService.startTracking(this)
                    result.success(true)
                }
                "stopTracking" -> {
                    com.footprint.service.LocationTrackingService.stopTracking(this)
                    result.success(true)
                }
                "generateAIStory" -> {
                    lifecycleScope.launch {
                        val entries: List<com.footprint.data.model.FootprintEntry> = repository.getAllEntries()
                        val lastEntry = entries.firstOrNull()
                        val story = if (lastEntry != null) {
                            com.footprint.utils.AIStoryGenerator.generateStory(
                                lastEntry.location,
                                lastEntry.mood,
                                lastEntry.happenedOn
                            )
                        } else {
                            "目前还没有足迹数据，快去开启你的第一场赛博探索吧。"
                        }
                        result.success(story)
                    }
                }
                "getSettings" -> {
                    val prefs = com.footprint.utils.PreferenceManager(this)
                    val settings = mapOf(
                        "nickname" to prefs.nickname,
                        "avatarId" to prefs.avatarId,
                        "themeMode" to prefs.themeMode.name,
                        "themeStyle" to prefs.themeStyle.name,
                        "hapticEnabled" to prefs.hapticFeedbackEnabled
                    )
                    result.success(gson.toJson(settings))
                }
                "updateNickname" -> {
                    val nickname = call.arguments as String
                    com.footprint.utils.PreferenceManager(this).nickname = nickname
                    result.success(true)
                }
                "updateAvatar" -> {
                    val avatarId = call.arguments as String
                    com.footprint.utils.PreferenceManager(this).avatarId = avatarId
                    result.success(true)
                }
                "updateThemeMode" -> {
                    val mode = call.arguments as String
                    com.footprint.utils.PreferenceManager(this).themeMode = com.footprint.ui.theme.ThemeMode.valueOf(mode)
                    result.success(true)
                }
                "updateThemeStyle" -> {
                    val style = call.arguments as String
                    com.footprint.utils.PreferenceManager(this).themeStyle = com.footprint.ui.theme.AppThemeStyle.valueOf(style)
                    result.success(true)
                }
                "updateHaptic" -> {
                    val enabled = call.arguments as Boolean
                    com.footprint.utils.PreferenceManager(this).hapticFeedbackEnabled = enabled
                    result.success(true)
                }
                "exportData" -> {
                    // TODO: 接入原生的 FileUtils.exportData
                    result.success(true)
                }
                "importData" -> {
                    // TODO: 接入原生的 ActivityResultContracts.OpenDocument
                    result.success(true)
                }
                else -> result.notImplemented()
            }
        }
        
        // --- 2. 实时定位流 (EventChannel) ---
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_STREAM).setStreamHandler(
            object : EventChannel.StreamHandler {
                private var locationJob: Job? = null
                private var trackingStatusJob: Job? = null

                override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                    // 推送位置更新
                    locationJob = lifecycleScope.launch {
                        com.footprint.service.LocationTrackingService.currentLocation.collect { loc ->
                            loc?.let {
                                val locMap = mapOf(
                                    "latitude" to it.latitude,
                                    "longitude" to it.longitude,
                                    "speed" to it.speed,
                                    "accuracy" to it.accuracy,
                                    "address" to (it.address ?: "")
                                )
                                events.success(gson.toJson(mapOf("type" to "location", "data" to locMap)))
                            }
                        }
                    }
                    
                    // 推送追踪状态更新
                    trackingStatusJob = lifecycleScope.launch {
                        com.footprint.service.LocationTrackingService.isTracking.collect { isTracking ->
                            events.success(gson.toJson(mapOf("type" to "status", "isTracking" to isTracking)))
                        }
                    }
                }

                override fun onCancel(arguments: Any?) {
                    locationJob?.cancel()
                    trackingStatusJob?.cancel()
                }
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 沉浸式体验
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}