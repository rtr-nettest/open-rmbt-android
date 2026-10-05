package at.specure.location.util

import at.specure.config.Config
import at.specure.data.entity.GeoLocationRecord
import at.specure.location.LocationInfo
import at.specure.test.DeviceInfo

/**
 * Single source of truth for the GPS quality a fix must have for a signal (coverage) measurement: it
 * must be accurate enough (accuracy within [Config.minLocationAccuracyMetersDuringSignalMeasurement])
 * and fresh enough (age within [Config.maxAgeOfLocationInformationForSignalMeasurementMillis]).
 *
 * Used both to decide when recording may START ([at.specure.measurement.signal.SignalMeasurementProcessor])
 * and to drive the "GPS ok" readiness indicator on the waiting screen, so the indicator can never be
 * green while the start is still waiting for a better fix. The comparison is on the raw accuracy value
 * - never a rounded one - so the two can't disagree by a fraction of a meter.
 */
fun LocationInfo.meetsSignalMeasurementGpsCriteria(config: Config): Boolean {
    if (!hasAccuracy) return false
    val ageMillis = ageNanos / 1_000_000L
    return accuracy <= config.minLocationAccuracyMetersDuringSignalMeasurement &&
        ageMillis <= config.maxAgeOfLocationInformationForSignalMeasurementMillis
}

fun GeoLocationRecord.toDeviceInfoLocation(): DeviceInfo.Location? {
    return DeviceInfo.Location(
        lat = latitude,
        long = longitude,
        speed = speed,
        altitude = altitude,
        time = timestampMillis,
        accuracy = accuracy,
        bearing = bearing,
        satellites = satellitesCount,
        mock_location = isMocked,
        provider = provider,
        age = ageNanos
    )
}