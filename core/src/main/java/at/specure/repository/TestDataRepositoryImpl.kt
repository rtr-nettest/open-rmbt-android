package at.specure.repository

import at.rmbt.util.io
import at.specure.database.CoreDatabase
import at.specure.database.entity.GeoLocation
import at.specure.database.entity.TestTrafficDownload
import at.specure.database.entity.TestTrafficUpload
import at.specure.location.LocationInfo

class TestDataRepositoryImpl(db: CoreDatabase) : TestDataRepository {

    private val geoLocationDao = db.geoLocationDao()
    private val testTrafficDao = db.testTrafficItemDao()

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

    override fun saveTrafficDownload(testUUID: String, threadId: Int, timeNanos: Long, bytes: Long) = io {
        val item = TestTrafficDownload(
            testUUID = testUUID,
            threadNumber = threadId,
            timeNanos = timeNanos,
            bytes = bytes
        )
        testTrafficDao.insertDownloadItem(item)
    }

    override fun saveTrafficUpload(testUUID: String, threadId: Int, timeNanos: Long, bytes: Long) {
        val item = TestTrafficUpload(
            testUUID = testUUID,
            threadNumber = threadId,
            timeNanos = timeNanos,
            bytes = bytes
        )
        testTrafficDao.insertUploadItem(item)
    }
}