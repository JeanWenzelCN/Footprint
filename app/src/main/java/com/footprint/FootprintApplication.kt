package com.footprint

import android.app.Application
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
import com.footprint.data.local.FootprintDatabase
import com.footprint.data.repository.FootprintRepository
import com.footprint.utils.ApiKeyManager

class FootprintApplication : Application() {
    lateinit var badgeEngine: com.footprint.badge.BadgeEngine
        private set

    lateinit var repository: FootprintRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // 设置自定义 API Key
        ApiKeyManager.getApiKey(this)?.let { key ->
            if (key.isNotBlank()) {
                MapsInitializer.setApiKey(key)
                AMapLocationClient.setApiKey(key)
            }
        }

        // --- 核心修复：全局最早期隐私确认 ---
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        AMapLocationClient.updatePrivacyShow(this, true, true)
        AMapLocationClient.updatePrivacyAgree(this, true)

        val database = FootprintDatabase.getInstance(this)
        val preferenceManager = com.footprint.utils.PreferenceManager(this)
        
        badgeEngine = com.footprint.badge.BadgeEngine(
            this,
            database.userBadgesDao(),
            database.userStatsDao()
        )
        
        repository =
                FootprintRepository(
                        database,
                        database.footprintDao(),
                        database.travelGoalDao(),
                        database.trackPointDao(),
                        database.timeCapsuleDao(),
                        database.userStatsDao(),
                        database.userBadgesDao(),
                        badgeEngine,
                        preferenceManager
                )
                
        // Schedule deep mining cold path
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiresBatteryNotLow(true)
            .build()
            
        val badgeWorkRequest = androidx.work.PeriodicWorkRequestBuilder<com.footprint.badge.BadgeWorker>(
            3, java.util.concurrent.TimeUnit.DAYS,
            1, java.util.concurrent.TimeUnit.DAYS
        )
        .setConstraints(constraints)
        .build()
        
        val calibrationRequest = androidx.work.PeriodicWorkRequestBuilder<com.footprint.badge.StatsCalibrationWorker>(
            3, java.util.concurrent.TimeUnit.DAYS,
            1, java.util.concurrent.TimeUnit.DAYS
        )
        .setConstraints(constraints)
        .build()
        
        androidx.work.WorkManager.getInstance(this).apply {
            enqueueUniquePeriodicWork(
                "ColdPathBadgeWorker",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                badgeWorkRequest
            )
            enqueueUniquePeriodicWork(
                "StatsCalibrationWorker",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                calibrationRequest
            )
        }
    }
}
