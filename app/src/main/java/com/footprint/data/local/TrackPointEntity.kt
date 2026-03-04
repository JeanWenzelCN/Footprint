package com.footprint.data.local

import androidx.room.*

@Entity(tableName = "track_points", indices = [Index(value = ["timestamp"])])
data class TrackPointEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long,
        val speed: Float,
        val accuracy: Float,
        val altitude: Double,
        val adcode: String? = null
)
