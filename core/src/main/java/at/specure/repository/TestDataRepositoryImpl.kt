package at.specure.repository

import at.rmbt.util.io
import at.specure.database.CoreDatabase
import at.specure.database.entity.GeoLocation
import at.specure.location.LocationInfo

class TestDataRepositoryImpl(db: CoreDatabase) : TestDataRepository {

    private val geoLocationDao = db.geoLocationDao()

    override fun saveGeoLocation(testUUID: String, location: LocationInfo) = io {
        val geoLocation = GeoLocation(
            testUUID = testUUID,
            latitude = location.latitude,
            longitude = location.longitude,
            provider = location.provider.name,
            speed = location.speed,
            altitude = location.altitude,
            time = location.time,
            timeCorrectionNs = location.elapsedRealtimeNanos,
            ageNanos = location.ageNanos,
            accuracy = location.accuracy,
            bearing = location.bearing,
            satellitesCount = location.satellites,
            isMocked = location.locationIsMocked
        )
        geoLocationDao.insert(geoLocation)
    }
}