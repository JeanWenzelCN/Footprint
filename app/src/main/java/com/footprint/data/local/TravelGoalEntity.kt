package com.footprint.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "travel_goals")
data class TravelGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "target_location") val targetLocation: String,
    @ColumnInfo(name = "target_date") val targetDate: LocalDate,
    val notes: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean,
    val progress: Int,
    val icon: String = "Flag"
)
