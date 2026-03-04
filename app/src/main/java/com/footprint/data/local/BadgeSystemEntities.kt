package com.footprint.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_badges")
data class UserBadgeEntity(
        @PrimaryKey val badgeId: String,
        val unlockDate: Long,
        val unlockLat: Double?,
        val unlockLng: Double?,
        val unlockWeather: String?,
        val unlockMileage: Double?
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
        @PrimaryKey val id: Int = 1, // Only one row
        val totalMileage: Double = 0.0,
        val totalDays: Int = 0,
        val citiesVisitedCount: Int = 0,
        val totalFootprints: Int = 0
)
