package at.specure.location

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import at.specure.location.LocationInfo.LocationCardinalDirections.EAST
import at.specure.location.LocationInfo.LocationCardinalDirections.NORTH
import at.specure.location.LocationInfo.LocationCardinalDirections.SOUTH
import at.specure.location.LocationInfo.LocationCardinalDirections.WEST

/**
 * Class suitable to display information for user, information are in human-readable form
 */
class LocationInfo {

    constructor(location: Location) {
        this.provider = LocationProvider.UNKNOWN

        latitudeDirection = assignLatitudeDirection(location.latitude)
        longitudeDirection = assignLongitudeDirection(location.longitude)

        latitude = location.latitude
        longitude = location.longitude

        hasSpeed = location.hasSpeed()
        speed = location.speed

        hasAccuracy = location.hasAccuracy()
        accuracy = location.accuracy

        satellites = formatSatellites(location.extras)

        provider = formatProvider(location.provider)
        providerRaw = location.provider
        locationIsMocked = location.isFromMockProvider

        hasAltitude = location.hasAltitude()
        altitude = location.altitude

        hasBearing = location.hasBearing()
        bearing = location.bearing

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            hasBearingAccuracy = location.hasBearingAccuracy()
            bearingAccuracy = location.bearingAccuracyDegrees
        }
        elapsedRealtimeNanos = location.elapsedRealtimeNanos
    }

    /**
     * @param latitude in degrees
     * @param longitude in degrees
     * @param hasSpeed true if speed value is valid
     * @param speed in m/s
     * @param hasAccuracy true if accuracy is valid
     * @param accuracy in meters, radius with 68% probability
     * @param provider string representing provider ("gps, network, fused")
     * @param isFromMockProvider true if values are mocked
     * @param hasAltitude true if altitude is valid
     * @param altitude in meters
     * @param hasBearing true if bearing value is valid
     * @param bearing in degrees (0.0 .. 360>, 0.0 is invalid value
     * @param hasBearingAccuracy true if bearing accuracy is valid value
     * @param bearingAccuracyDegrees one side of two-side degrees area which represents 68% probability that bearing is in that way
     * @param elapsedRealtimeNanos time of location fix, in elapsed real-time since system boot
     * @param extras expecting "satellites" in it or nothing
     */
    constructor(
        latitude: Double,
        longitude: Double,
        hasSpeed: Boolean,
        speed: Float,
        hasAccuracy: Boolean,
        accuracy: Float,
        provider: String,
        isFromMockProvider: Boolean,
        hasAltitude: Boolean,
        altitude: Double,
        hasBearing: Boolean,
        bearing: Float,
        hasBearingAccuracy: Boolean,
        bearingAccuracyDegrees: Float,
        elapsedRealtimeNanos: Long,
        extras: Bundle
    ) {
        this.latitudeDirection = assignLatitudeDirection(latitude)
        this.longitudeDirection = assignLongitudeDirection(longitude)

        this.latitude = latitude
        this.longitude = longitude

        this.hasSpeed = hasSpeed
        this.speed = speed

        this.hasAccuracy = hasAccuracy
        this.accuracy = accuracy

        this.satellites = formatSatellites(extras)

        this.provider = formatProvider(provider)
        this.locationIsMocked = isFromMockProvider

        this.hasAltitude = hasAltitude
        this.altitude = altitude

        this.hasBearing = hasBearing
        this.bearing = bearing

        this.hasBearingAccuracy = hasBearingAccuracy
        this.bearingAccuracy = bearingAccuracyDegrees

        this.elapsedRealtimeNanos = elapsedRealtimeNanos
    }

    /**
     * Raw string of location provider from android.location.Location object
     */
    var providerRaw: String? = ""

    /**
     * Return the time of this fix, in elapsed real-time since system boot.
     * This value can be reliably compared to SystemClock.elapsedRealtimeNanos(), to calculate the age of a fix and to compare Location fixes.
     * This is reliable because elapsed real-time is guaranteed monotonic for each system boot and continues to increment even when the system
     * is in deep sleep (unlike getTime().
     */
    var elapsedRealtimeNanos: Long = SystemClock.elapsedRealtimeNanos()

    /**
     * Direction of latitude ([NORTH], [SOUTH]) or null if it is not defined
     */
    var latitudeDirection: LocationCardinalDirections? = null

    /**
     * Direction of latitude ([WEST], [EAST]) or null if it is not defined
     */
    var longitudeDirection: LocationCardinalDirections? = null

    /**
     * Latitude in Human readable format XX°XX.XXX'
     */
    var latitude: Double

    /**
     * Longitude in Human readable format XX°XX.XXX'
     */
    var longitude: Double

    /**
     * true if location has speed valid
     */
    var hasSpeed: Boolean = false

    /**
     * Speed in km/h without unit, null if N/A
     */
    var speed: Float

    /**
     * Count of available satellites or 0 if N/A
     */
    var satellites: Int = 0

    /**
     * true if location has accuracy valid
     */
    var hasAccuracy: Boolean = false

    /**
     * Accuracy in meters without unit or null if N/A
     */
    var accuracy: Float

    /**
     * Provider as [LocationProvider]
     */
    var provider: LocationProvider

    /**
     * true if location is from mocked provider, false otherwise
     */
    var locationIsMocked: Boolean = false

    /**
     * true if location has altitude valid
     */
    var hasAltitude: Boolean = false

    /**
     * Altitude in meters without unit or null if N/A
     */
    var altitude: Double

    /**
     * true if location has bearing valid
     */
    var hasBearing: Boolean = false

    /**
     * Get the bearing, in degrees.
     * Bearing is the horizontal direction of travel of this device, and is not related to the device orientation. It is guaranteed to be in the range (0.0, 360.0] if the device has a bearing.
     * If this location does not have a bearing then null is returned.
     */
    var bearing: Float = 0.0f

    /**
     * true if location has bearingAccuracy valid
     */
    var hasBearingAccuracy: Boolean = false

    /**
     * Get the estimated bearing accuracy of this location, in degrees.
     * We define bearing accuracy at 68% confidence. Specifically, as 1-side of the 2-sided range on each side of the estimated bearing reported by getBearing(), within which there is a 68% probability of finding the true bearing.
     * In the case where the underlying distribution is assumed Gaussian normal, this would be considered 1 standard deviation.
     * For example, if getBearing() returns 60, and getBearingAccuracy() returns 10, then there is a 68% probability of the true bearing being between 50 and 70 degrees.
     */
    var bearingAccuracy: Float = 0.0f

    val age: Long
        get() = SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos

    private fun formatProvider(provider: String): LocationProvider {
        return when (provider) {
            "fused" -> LocationProvider.FUSED
            "gps" -> LocationProvider.GPS
            "network" -> LocationProvider.NETWORK
            else -> LocationProvider.UNKNOWN
        }
    }

    private fun formatSatellites(extras: Bundle?): Int {
        return extras?.getInt("satellites") ?: 0
    }

    private fun assignLatitudeDirection(latitude: Double): LocationCardinalDirections {
        return when {
            latitude >= 0 -> NORTH
            else -> SOUTH
        }
    }

    private fun assignLongitudeDirection(longitude: Double): LocationCardinalDirections {
        return when {
            longitude >= 0 -> EAST
            else -> WEST
        }
    }

    enum class LocationCardinalDirections {

        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    enum class LocationProvider {

        UNKNOWN,
        NETWORK,
        GPS,
        FUSED,
    }
}
