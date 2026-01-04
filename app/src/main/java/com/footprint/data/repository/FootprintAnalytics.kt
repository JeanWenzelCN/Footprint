package com.footprint.data.repository

import com.footprint.data.model.FootprintEntry
import com.footprint.data.model.FootprintSummary
import com.footprint.data.model.Stats
import com.footprint.data.model.Mood
import java.time.LocalDate

object FootprintAnalytics {
    fun buildSummary(
        entries: List<FootprintEntry>, 
        targetYear: Int = LocalDate.now().year,
        yearlyTrackPoints: Int = 0,
        monthlyTrackPoints: Int = 0
    ): FootprintSummary {
        val now = LocalDate.now()
        val currentMonth = now.monthValue

        val yearly = entries.filter { it.happenedOn.year == targetYear }
        val monthly = yearly.filter { it.happenedOn.monthValue == currentMonth }

        return FootprintSummary(
            yearly = calculateStats(yearly, yearlyTrackPoints),
            monthly = calculateStats(monthly, monthlyTrackPoints),
            streakDays = computeStreak(entries.map { it.happenedOn }),
            daysActiveThisYear = yearly.map { it.happenedOn }.distinct().size
        )
    }

    private fun calculateStats(entries: List<FootprintEntry>, trackPoints: Int = 0): Stats {
        if (entries.isEmpty()) return Stats(totalTrackPoints = trackPoints)
        
        val totalDistance = entries.sumOf { it.distanceKm }
        val energyAverage = entries.map { it.energyLevel }.average().takeIf { !it.isNaN() } ?: 0.0
        
        // Vitality Index calculation (0-100)
        // Frequency: more entries = higher vitality (capped at 1 per day for this simple metric)
        val uniqueDays = entries.map { it.happenedOn }.distinct().size
        val frequencyScore = (uniqueDays * 5).coerceAtMost(40) // Int comparison is fine here
        val energyScore = (energyAverage * 4).coerceAtMost(40.0)  // energyAverage is Double, needs 40.0
        val distanceScore = (totalDistance / 2.0).coerceAtMost(20.0) // totalDistance is Double, needs 20.0
        val vitalityIndex = (frequencyScore + energyScore + distanceScore).toInt()

        val topLocations = entries.groupBy { it.location }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        val travelStyle = calculateTravelStyle(entries)

        return Stats(
            totalEntries = entries.size,
            totalDistance = totalDistance,
            uniquePlaces = entries.map { it.location }.distinct().size,
            dominantMood = entries.groupBy { it.mood }.maxByOrNull { it.value.size }?.key,
            energyAverage = energyAverage,
            vitalityIndex = vitalityIndex,
            topLocations = topLocations,
            totalTrackPoints = trackPoints,
            travelStyle = travelStyle
        )
    }

    private fun calculateTravelStyle(entries: List<FootprintEntry>): String {
        if (entries.isEmpty()) return "独行侠"
        val tags = entries.flatMap { it.tags }.groupBy { it }.mapValues { it.value.size }
        val topTag = tags.maxByOrNull { it.value }?.key?.lowercase() ?: ""
        
        return when {
            topTag.contains("美食") || topTag.contains("吃") -> "美食饕餮"
            topTag.contains("自然") || topTag.contains("山") || topTag.contains("水") -> "自然之友"
            topTag.contains("城市") || topTag.contains("街") -> "都市漫游者"
            topTag.contains("运动") || topTag.contains("跑") -> "运动达人"
            topTag.contains("艺术") || topTag.contains("展") -> "文艺青年"
            entries.size > 10 && entries.sumOf { it.distanceKm } > 100 -> "硬核旅者"
            else -> "时光记录者"
        }
    }

    private fun computeStreak(dates: List<LocalDate>): Int {
        if (dates.isEmpty()) return 0
        val uniqueDates = dates.distinct().sortedDescending()
        val now = LocalDate.now()
        
        var streak = 0
        var currentCheck = if (uniqueDates.firstOrNull() == now || uniqueDates.firstOrNull() == now.minusDays(1)) {
            uniqueDates.first()
        } else {
            return 0
        }

        for (date in uniqueDates) {
            if (date == currentCheck) {
                streak++
                currentCheck = currentCheck.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }
}