package com.footprint.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_capsules")
data class TimeCapsuleEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val latitude: Double,
        val longitude: Double,
        val message: String,
        val contentUri: String? = null,
        val creationTime: Long, // Epoch Milliseconds
        val unlockTime: Long, // Epoch Milliseconds
        val isUnlocked: Boolean = false,
        val radius: Double = 50.0 // Unlock radius in meters
)
