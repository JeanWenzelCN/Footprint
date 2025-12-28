package com.footprint.data.model

import com.amap.api.maps.model.LatLng
import kotlin.math.floor

/**
 * 探索网格坐标（以 0.005 经纬度为单位，约 500m）
 */
data class ExplorerGrid(
    val latIndex: Int,
    val lngIndex: Int
) {
    companion object {
        private const val GRID_SIZE = 0.005
        
        fun fromLatLng(latLng: LatLng): ExplorerGrid {
            return ExplorerGrid(
                latIndex = floor(latLng.latitude / GRID_SIZE).toInt(),
                lngIndex = floor(latLng.longitude / GRID_SIZE).toInt()
            )
        }
    }
    
    fun toCenterLatLng(): LatLng {
        return LatLng(
            (latIndex * GRID_SIZE) + (GRID_SIZE / 2),
            (lngIndex * GRID_SIZE) + (GRID_SIZE / 2)
        )
    }
}
