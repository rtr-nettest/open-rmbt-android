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

    /**
     * Raw string of location provider from android.location.Location object
     */
    val providerRaw: String?

    /**
     * Return the time of this fix, in elapsed real-time since system boot.
     * This value can be reliably compared to SystemClock.elapsedRealtimeNanos(), to calculate the age of a fix and to compare Location fixes.
     * This is reliable because elapsed real-time is guaranteed monotonic for each system boot and continues to increment even when the system
     * is in deep sleep (unlike getTime().
     */
    val elapsedRealtimeNanos: Long

    /**
     * Direction of latitude ([NORTH], [SOUTH]) or null if it is not defined
     */
    val latitudeDirection: LocationCardinalDirections

    /**
     * Direction of latitude ([WEST], [EAST]) or null if it is not defined
     */
    val longitudeDirection: LocationCardinalDirections

    /**
     * Latitude in Human readable format XX°XX.XXX'
     */
    val latitude: Double

    /**
     * Longitude in Human readable format XX°XX.XXX'
     */
    val longitude: Double

    /**
     * true if location has speed valid
     */
    val hasSpeed: Boolean

    /**
     * Speed in km/h without unit, null if N/A
     */
    val speed: Float

    /**
     * Count of available satellites or 0 if N/A
     */
    val satellites: Int

    /**
     * true if location has accuracy valid
     */
    val hasAccuracy: Boolean

    /**
     * Accuracy in meters without unit or null if N/A
     */
    val accuracy: Float

    /**
     * Provider as [LocationProvider]
     */
    val provider: LocationProvider

    /**
     * true if location is from mocked provider, false otherwise
     */
    val locationIsMocked: Boolean

    /**
     * true if location has altitude valid
     */
    val hasAltitude: Boolean

    /**
     * Altitude in meters without unit or null if N/A
     */
    val altitude: Double

    /**
     * true if location has bearing valid
     */
    val hasBearing: Boolean

    /**
     * Get the bearing, in degrees.
     * Bearing is the horizontal direction of travel of this device, and is not related to the device orientation. It is guaranteed to be in the range (0.0, 360.0] if the device has a bearing.
     * If this location does not have a bearing then null is returned.
     */
    val bearing: Float

    /**
     * true if location has bearingAccuracy valid
     */
    val hasBearingAccuracy: Boolean

    /**
     * Get the estimated bearing accuracy of this location, in degrees.
     * We define bearing accuracy at 68% confidence. Specifically, as 1-side of the 2-sided range on each side of the estimated bearing reported by getBearing(), within which there is a 68% probability of finding the true bearing.
     * In the case where the underlying distribution is assumed Gaussian normal, this would be considered 1 standard deviation.
     * For example, if getBearing() returns 60, and getBearingAccuracy() returns 10, then there is a 68% probability of the true bearing being between 50 and 70 degrees.
     */
    val bearingAccuracy: Float

    /**
     * Time duration since current location info was received in nanoseconds
     */
    val ageNanos: Long
        get() = SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos

    /**
     * Return the UTC time of this fix, in milliseconds since January 1, 1970.
     */
    val time: Long

    constructor(location: Location) {
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
        } else {
            hasBearingAccuracy = false
            bearingAccuracy = 0f
        }
        elapsedRealtimeNanos = location.elapsedRealtimeNanos

        time = location.time
    }

    /**
     * [latitude] in degrees
     * [longitude] in degrees
     * [hasSpeed] true if speed value is valid
     * [speed] in m/s
     * [hasAccuracy] true if accuracy is valid
     * [accuracy] in meters, radius with 68% probability
     * [provider] string representing provider ("gps, network, fused")
     * [isFromMockProvider] true if values are mocked
     * [hasAltitude] true if altitude is valid
     * [altitude] in meters
     * [hasBearing] true if bearing value is valid
     * [bearing] in degrees (0.0 .. 360>, 0.0 is invalid value
     * [hasBearingAccuracy] true if bearing accuracy is valid value
     * [bearingAccuracyDegrees] one side of two-side degrees area which represents 68% probability that bearing is in that way
     * [elapsedRealtimeNanos] time of location fix, in elapsed real-time since system boot
     * [satellitesCount] satellites count
     * [time] UTC time for this fix
     */
    constructor(
        latitude: Double,
        longitude: Double,
        hasSpeed: Boolean,
        speed: Float,
        hasAccuracy: Boolean,
        accuracy: Float,
        provider: String,
        providerRaw: String?,
        isFromMockProvider: Boolean,
        hasAltitude: Boolean,
        altitude: Double,
        hasBearing: Boolean,
        bearing: Float,
        hasBearingAccuracy: Boolean,
        bearingAccuracyDegrees: Float,
        elapsedRealtimeNanos: Long,
        satellitesCount: Int,
        time: Long
    ) {
        this.latitudeDirection = assignLatitudeDirection(latitude)
        this.longitudeDirection = assignLongitudeDirection(longitude)

        this.latitude = latitude
        this.longitude = longitude

        this.hasSpeed = hasSpeed
        this.speed = speed

        this.hasAccuracy = hasAccuracy
        this.accuracy = accuracy

        this.satellites = satellitesCount

        this.provider = formatProvider(provider)
        this.providerRaw = providerRaw
        this.locationIsMocked = isFromMockProvider

        this.hasAltitude = hasAltitude
        this.altitude = altitude

        this.hasBearing = hasBearing
        this.bearing = bearing

        this.hasBearingAccuracy = hasBearingAccuracy
        this.bearingAccuracy = bearingAccuracyDegrees

        this.elapsedRealtimeNanos = elapsedRealtimeNanos

        this.time = time
    }

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
