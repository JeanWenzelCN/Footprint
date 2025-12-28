package com.footprint.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.footprint.data.model.Mood
import java.time.LocalDate

@Entity(tableName = "footprints")
data class FootprintEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val location: String,
    val detail: String,
    val mood: Mood,
    val tags: List<String>,
    @ColumnInfo(name = "distance_km") val distanceKm: Double,
    val photos: List<String>,
    @ColumnInfo(name = "energy_level") val energyLevel: Int,
    @ColumnInfo(name = "happened_on") val happenedOn: LocalDate,
    val altitude: Double? = null,
    val weather: String? = null,
    val temperature: Double? = null,
    @ColumnInfo(name = "transport_type") val transportType: String = "UNKNOWN",
    @ColumnInfo(name = "carbon_saved") val carbonSaved: Double = 0.0
)
