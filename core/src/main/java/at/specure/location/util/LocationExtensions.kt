package at.specure.location.util

import at.specure.data.entity.GeoLocationRecord
import at.specure.test.DeviceInfo

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