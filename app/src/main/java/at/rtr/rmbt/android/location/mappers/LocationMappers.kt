package at.rtr.rmbt.android.location.mappers

import android.location.Location
import at.specure.location.LocationInfo

fun LocationInfo.toLocation(): Location {
    val location = Location(provider)
    location.latitude = latitude
    location.longitude = longitude
    location.time = time
    location.accuracy = accuracy
    location.bearing = bearing
    location.bearingAccuracyDegrees = bearingAccuracy
    location.elapsedRealtimeNanos = elapsedRealtimeNanos
    location.speed = speed
    return location
}