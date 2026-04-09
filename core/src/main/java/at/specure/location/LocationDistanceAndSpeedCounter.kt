package at.specure.location

import cz.mroczis.netmonster.core.Milliseconds
import kotlin.math.abs
import kotlin.math.pow

object LocationDistanceAndSpeedCounter {

    private const val EARTH_RADIUS_METERS = 6371000.0

    fun getSpeedMetersPerSecond(lat1: Double, lon1: Double, lat2: Double, lon2: Double, timestampMilliseconds1: Long, timestampMilliseconds2: Long ): Float {
        val distanceMeters = getDistanceMeters(lat1, lon1, lat2, lon2)
        val timeSeconds = abs(timestampMilliseconds2 - timestampMilliseconds1) / 1000.0
        if (timeSeconds == 0.0) {
            return 0f
        }
        val speedMetersPerSecond = (distanceMeters / timeSeconds).toFloat()
        return speedMetersPerSecond
    }

    fun getDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = Math.sin(dLat / 2).pow(2) +
                Math.cos(rLat1) * Math.cos(rLat2) *
                Math.sin(dLon / 2).pow(2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        val distanceMeters =  EARTH_RADIUS_METERS * c
        return distanceMeters
    }
}