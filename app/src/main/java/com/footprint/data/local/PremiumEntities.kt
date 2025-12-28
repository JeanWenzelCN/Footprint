package com.footprint.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String, // 用于匹配资源图标
    val isUnlocked: Boolean = false,
    val unlockDate: LocalDate? = null,
    val category: String, // 如 "FOOD", "EXPLORE", "ECO"
    val progress: Int = 0,
    val target: Int = 1
)

@Entity(tableName = "privacy_fences")
data class PrivacyFenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val isActive: Boolean = true
)
