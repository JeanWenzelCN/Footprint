package com.footprint.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object WeatherResonanceManager {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null
    
    // State to hold the partner's current weather
    private val _partnerWeatherStatus = MutableStateFlow(WeatherStatus.UNKNOWN)
    val partnerWeatherStatus = _partnerWeatherStatus.asStateFlow()

    fun startSync(partnerCity: String) {
        if (syncJob?.isActive == true) return
        
        syncJob = scope.launch {
            while (isActive) {
                // Mocking the weather API call for the partner's city
                val weather = fetchWeatherForCity(partnerCity)
                if (weather != _partnerWeatherStatus.value) {
                    _partnerWeatherStatus.value = weather
                    Log.d("WeatherResonance", "Partner city $partnerCity weather updated to: $weather")
                    // Notify Flutter UI via MethodChannel in FlutterMapView or MainActivity
                }
                
                // Polling interval (e.g. 15 minutes) - keeping brief for demo
                delay(15 * 60 * 1000L)
            }
        }
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
    }

    private suspend fun fetchWeatherForCity(city: String): WeatherStatus {
        Log.d("WeatherResonance", "Fetching weather for $city (mock)")
        delay(1000) // Simulate network delay
        return WeatherStatus.RAIN // Hardcoded to RAIN to trigger the beautiful shader
    }

    // "当雨滴滑落到底部时，手指甚至能感觉到极其细微的酥麻感"
    fun triggerVirtualRaindropHaptic(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(10) // 10ms light buzz
            }
        }
    }

    enum class WeatherStatus {
        UNKNOWN, SUNNY, CLOUDY, RAIN, SNOW
    }
}
