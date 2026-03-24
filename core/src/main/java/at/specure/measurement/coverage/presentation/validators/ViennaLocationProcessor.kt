package at.specure.measurement.coverage.presentation.validators

import at.specure.test.DeviceInfo
import kotlin.math.sqrt

/**
 * Calculates the distance between two points in meters with Vienna approximations provided by DZ suitable for quick distance computing
 * requested by #83
 */
object ViennaLocationProcessor {

    fun shouldStoreLocation(
        lastLocation: DeviceInfo.Location?,
        newLocation: DeviceInfo.Location
    ): Boolean {

        if (lastLocation == null) return true

        val distance = distanceMetersApprox(
            lastLocation.lat,
            lastLocation.long,
            newLocation.lat,
            newLocation.long
        )

        val threshold = maxOf(newLocation.accuracy.toDouble(), 10.0)
        return distance > threshold
    }

    fun distanceMetersApprox(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {

        val deltaX = (lon2 - lon1) / 0.000014   // ~1m v longitude (Vienna approx)
        val deltaY = (lat2 - lat1) / 0.000009   // ~1m v latitude

        val distanceMetersApprox = sqrt(deltaX * deltaX + deltaY * deltaY)

        return distanceMetersApprox
    }
}

