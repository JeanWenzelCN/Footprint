package com.footprint.badge

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.footprint.data.local.FootprintDatabase
import com.footprint.data.local.UserStatsEntity
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class StatsCalibrationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i("StatsCalibration", "Ghost calibration task started (Self-Healing)")
        
        val db = FootprintDatabase.getInstance(applicationContext)
        val statsDao = db.userStatsDao()
        val footprintDao = db.footprintDao()
        
        var totalMileage = 0.0
        var totalFootprints = 0
        val uniqueDays = mutableSetOf<String>()
        val uniqueCities = mutableSetOf<String>()
        
        val chunkSize = 10000
        var offset = 0

        while (true) {
            val chunk = footprintDao.getFootprintsPaged(chunkSize, offset)
            if (chunk.isEmpty()) break

            for (fp in chunk) {
                totalMileage += fp.distanceKm
                totalFootprints++
                
                // Track unique days
                uniqueDays.add(fp.happenedOn.toString())

                // Track unique cities (assuming 'location' implies city for this metric, 
                // or we extract the city name from 'location'. For now, we'll use location string 
                // until we have a better definition. In many apps location is formatted "City · Place" 
                // or similar. Here we'll just track the raw location string if it's the simplest proxy 
                // for 'City'. Actually, maybe check how Dashboard tracks unique places.)
                // Given "探索地点" uses "location" string simply in Dashboard:
                val loc = fp.location
                if (loc.isNotBlank()) {
                    // Just an example, maybe extract "Beijing" from "Beijing, Chaoyang"
                    // To keep it simple, we use the raw location or first word.
                    val city = loc.split(" ")[0]
                    uniqueCities.add(city)
                }
            }
            
            offset += chunkSize
            Log.d("StatsCalibration", "Scanned $offset footprint records...")
        }

        val calculatedStats = UserStatsEntity(
            id = 1,
            totalMileage = totalMileage,
            totalDays = uniqueDays.size,
            citiesVisitedCount = uniqueCities.size,
            totalFootprints = totalFootprints
        )

        val currentStats = statsDao.getUserStats()
        
        if (currentStats == null) {
            Log.i("StatsCalibration", "No user stats found. Initializing with calibrated values.")
            statsDao.upsertUserStats(calculatedStats)
        } else {
            val isDiff = abs(currentStats.totalMileage - totalMileage) > 0.001 ||
                         currentStats.totalDays != uniqueDays.size ||
                         currentStats.citiesVisitedCount != uniqueCities.size ||
                         currentStats.totalFootprints != totalFootprints
                         
            if (isDiff) {
                Log.w("StatsCalibration", "Data Drift Detected! Expected: $calculatedStats, Found: $currentStats. Committing Overwrite.")
                statsDao.upsertUserStats(calculatedStats)
            } else {
                Log.i("StatsCalibration", "Data holds perfect integrity. No overwrite needed.")
            }
        }

        return Result.success()
    }
}
