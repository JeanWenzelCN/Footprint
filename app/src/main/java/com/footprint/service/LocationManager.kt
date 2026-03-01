package com.footprint.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

/** 位置管理器 提供位置相关的工具方法 */
class LocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

    /** 检查是否有位置权限 */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** 获取当前位置（一次性） */
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 计算两个位置之间的距离（米） */
    fun calculateDistance(start: Location, end: Location): Float {
        return start.distanceTo(end)
    }

    /** 格式化距离显示 */
    fun formatDistance(meters: Float): String {
        return when {
            meters < 1000 -> "${meters.toInt()} 米"
            else -> String.format("%.3f 公里", meters / 1000)
        }
    }

    /** 计算路径总距离 */
    fun calculatePathDistance(locations: List<Location>): Float {
        if (locations.size < 2) return 0f

        var totalDistance = 0f
        for (i in 0 until locations.size - 1) {
            totalDistance += calculateDistance(locations[i], locations[i + 1])
        }
        return totalDistance
    }

    companion object {
        /** 需要的位置权限 */
        val REQUIRED_PERMISSIONS =
                arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )

        /** 后台位置权限（可选） */
        val BACKGROUND_PERMISSION =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                } else {
                    null
                }
    }
}
