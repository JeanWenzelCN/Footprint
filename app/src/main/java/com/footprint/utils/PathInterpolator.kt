package com.footprint.utils

import com.amap.api.maps.model.LatLng
import kotlin.math.pow

object PathInterpolator {
    /**
     * Interpolates a list of LatLng points using Catmull-Rom spline for smoothness.
     * [points] original points
     * [multiplier] number of points to generate between each pair of original points
     */
    fun interpolate(points: List<LatLng>, multiplier: Int = 5): List<LatLng> {
        if (points.size < 3) return points

        val result = mutableListOf<LatLng>()
        
        // Add virtual points at start and end to handle boundary conditions
        val p0 = points[0]
        val pLast = points.last()
        
        val extendedPoints = mutableListOf<LatLng>()
        // Virtual p-1
        extendedPoints.add(LatLng(p0.latitude * 2 - points[1].latitude, p0.longitude * 2 - points[1].longitude))
        extendedPoints.addAll(points)
        // Virtual p+1
        val secondToLast = points[points.size - 2]
        extendedPoints.add(LatLng(pLast.latitude * 2 - secondToLast.latitude, pLast.longitude * 2 - secondToLast.longitude))

        for (i in 1 until extendedPoints.size - 2) {
            val a = extendedPoints[i - 1]
            val b = extendedPoints[i]
            val c = extendedPoints[i + 1]
            val d = extendedPoints[i + 2]

            for (j in 0 until multiplier) {
                val t = j.toDouble() / multiplier
                result.add(catmullRom(a, b, c, d, t))
            }
        }
        result.add(points.last())
        return result
    }

    private fun catmullRom(p0: LatLng, p1: LatLng, p2: LatLng, p3: LatLng, t: Double): LatLng {
        val t2 = t * t
        val t3 = t2 * t
        
        val lat = 0.5 * (
            (2 * p1.latitude) +
            (-p0.latitude + p2.latitude) * t +
            (2 * p0.latitude - 5 * p1.latitude + 4 * p2.latitude - p3.latitude) * t2 +
            (-p0.latitude + 3 * p1.latitude - 3 * p2.latitude + p3.latitude) * t3
        )
        
        val lng = 0.5 * (
            (2 * p1.longitude) +
            (-p0.longitude + p2.longitude) * t +
            (2 * p0.longitude - 5 * p1.longitude + 4 * p2.longitude - p3.longitude) * t2 +
            (-p0.longitude + 3 * p1.longitude - 3 * p2.longitude + p3.longitude) * t3
        )
        
        return LatLng(lat, lng)
    }
}
