package com.opiumfive.ycupwars

import com.google.android.gms.maps.model.LatLng
import com.google.android.material.math.MathUtils.floorMod
import com.google.maps.android.SphericalUtil.computeHeading
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Maths {
    companion object {

        fun angleFromCoordinate2(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
            val srcLat = Math.toRadians(lat1)
            val dstLat = Math.toRadians(lat2)
            val dLng = Math.toRadians(long2 - long1)
            val radians =  Math.atan2(
                Math.sin(dLng) * Math.cos(dstLat),
                Math.cos(srcLat) * Math.sin(dstLat) - Math.sin(srcLat) * Math.cos(dstLat) * Math.cos(dLng)
            )
            return Math.toDegrees(radians)
        }

        fun angleFromCoordinate3(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
            val point1 = LatLng(lat1, long1)
            val point2 = LatLng(lat2, long2)
            val heading = computeHeading(point1, point2)
            return heading
        }

        fun angleFromCoordinate(
            lat1: Double, long1: Double, lat2: Double,
            long2: Double
        ): Double {
            val dLon = long2 - long1
            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - (sin(lat1) * cos(lat2) * cos(dLon))
            var brng = atan2(y, x)
            brng = Math.toDegrees(brng)
            brng = (brng + 360) % 360
            brng = 360 - brng // count degrees counter-clockwise - remove to make clockwise
            return brng
        }

        fun angleDiff(a1: Float, a2: Float): Float {
            val tmp = a1 - a2
            return floorMod((tmp + 180), 360) - 180
        }
    }
}