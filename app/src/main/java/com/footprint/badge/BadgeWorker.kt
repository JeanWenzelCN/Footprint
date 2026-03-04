package com.footprint.badge

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.footprint.FootprintApplication
import com.footprint.data.local.UserBadgeEntity
import com.footprint.data.model.FootprintEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Cold Path - depth analysis during device idle and charging.
 * Designed to perform large dataset scanning across years of location logs 
 * to award timeline-heavy badges (like traversing 30 days consecutively).
 */
class BadgeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val application = applicationContext as FootprintApplication
        val badgeEngine = application.badgeEngine

        try {
            Log.d("BadgeWorker", "Starting Cold Path Deep Mining...")
            
            // Retroactively evaluate all badges based on historical data
            badgeEngine.evaluateColdPath()
            
            Result.success()
        } catch (e: Exception) {
            Log.e("BadgeWorker", "Cold path mining failed: ${e.message}")
            Result.retry()
        }
    }
}
