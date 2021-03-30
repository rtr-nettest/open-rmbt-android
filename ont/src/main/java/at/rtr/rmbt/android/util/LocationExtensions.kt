package at.rtr.rmbt.android.util

import android.location.Location
import at.specure.location.LocationInfo
import java.util.concurrent.TimeUnit

fun LocationInfo.formatAccuracy(): String? {
    return if (!hasAccuracy || accuracy < 0)
        null
    else
        String.format("+/-%.0f ", accuracy)
}

fun LocationInfo.formatCoordinate(coordinate: Double): String {
    var min = 0f
    val rawStr = coordinate.let { Location.convert(it, Location.FORMAT_MINUTES) }
    val split = rawStr?.split(":".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()
    try {
        split?.set(1, split[1].replace(",", "."))
        min = split?.get(1)?.let { java.lang.Float.parseFloat(it) }!!
    } catch (e: NumberFormatException) {
    }
    return String.format("%s°%.3f'", split?.get(0)?.replace("-", ""), min)
}

/**
 * Convert speed from m/s to km/h
 */
fun LocationInfo.formatSpeed(): String? {
    var speedFormatted: String? = null
    if (hasSpeed) {
        speedFormatted = String.format("%.0f", speed.times(3.6))
    }
    return speedFormatted
}

/**
 * @return formatted altitude in meters if [LocationInfo.hasAltitude] is true in X.X format without unit
 */
fun LocationInfo.formatAltitude(): String? {
    return if (hasAltitude)
        String.format("%.0f", altitude)
    else
        null
}

/**
 * @return formatted bearing accuracy in degrees if [LocationInfo.hasBearingAccuracy] is true in +/-X.0° format
 */
fun LocationInfo.formatBearingAccuracy(): String? {
    return if (hasBearingAccuracy) {
        String.format("+/-%.0f°", bearingAccuracy)
    } else {
        null
    }
}

/**
 * @return formatted bearing in degrees if [LocationInfo.hasBearing] is true in X.0° format
 */
fun LocationInfo.formatBearing(): String? {
    return if (hasBearing && bearing != 0.0f) {
        String.format("%.0f°", bearing)
    } else {
        null
    }
}

/**
 * @return string containing age of event in s without unit, for values below 1s returns "< 1"
 */
fun LocationInfo.formatAgeString(): String {
    return if (ageNanos < TimeUnit.SECONDS.toNanos(1))
        "< 1"
    else
        String.format("%d", TimeUnit.NANOSECONDS.toSeconds(ageNanos))
}