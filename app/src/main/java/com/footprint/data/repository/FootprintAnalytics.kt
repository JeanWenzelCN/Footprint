package com.footprint.data.repository

import com.footprint.data.model.FootprintEntry
import com.footprint.data.model.FootprintSummary
import com.footprint.data.model.Stats
import com.footprint.data.model.Mood
import java.time.LocalDate

object FootprintAnalytics {
    fun buildSummary(entries: List<FootprintEntry>): FootprintSummary {
        val now = LocalDate.now()
        val currentYear = now.year
        val currentMonth = now.monthValue

        val yearly = entries.filter { it.happenedOn.year == currentYear }
        val monthly = yearly.filter { it.happenedOn.monthValue == currentMonth }

        return FootprintSummary(
            yearly = calculateStats(yearly),
            monthly = calculateStats(monthly),
            streakDays = computeStreak(entries.map { it.happenedOn }),
            daysActiveThisYear = yearly.map { it.happenedOn }.distinct().size
        )
    }

    private fun calculateStats(entries: List<FootprintEntry>): Stats {
        if (entries.isEmpty()) return Stats()
        return Stats(
            totalEntries = entries.size,
            totalDistance = entries.sumOf { it.distanceKm },
            uniquePlaces = entries.map { it.location }.distinct().size,
            dominantMood = entries.groupBy { it.mood }.maxByOrNull { it.value.size }?.key,
            energyAverage = entries.map { it.energyLevel }.average().takeIf { !it.isNaN() } ?: 0.0
        )
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