package at.specure.repository

import at.rmbt.util.io
import at.specure.database.CoreDatabase
import at.specure.database.entity.GRAPH_ITEM_TYPE_DOWNLOAD
import at.specure.database.entity.GRAPH_ITEM_TYPE_UPLOAD
import at.specure.database.entity.GeoLocation
import at.specure.database.entity.GraphItem
import at.specure.location.LocationInfo

class TestDataRepositoryImpl(db: CoreDatabase) : TestDataRepository {

    private val geoLocationDao = db.geoLocationDao()
    private val graphItemDao = db.graphItemsDao()

    override fun saveGeoLocation(testUUID: String, location: LocationInfo) = io {
        val geoLocation = GeoLocation(
            testUUID = testUUID,
            latitude = location.latitude,
            longitude = location.longitude,
            provider = location.provider.name,
            speed = location.speed,
            altitude = location.altitude,
            time = location.time,
            timeCorrectionNanos = location.elapsedRealtimeNanos,
            ageNanos = location.ageNanos,
            accuracy = location.accuracy,
            bearing = location.bearing,
            satellitesCount = location.satellites,
            isMocked = location.locationIsMocked
        )
        geoLocationDao.insert(geoLocation)
    }

    override fun saveDownloadGraphItem(testUUID: String, progress: Int, speedBps: Long) {
        val graphItem = GraphItem(testUUID = testUUID, progress = progress, value = speedBps, type = GRAPH_ITEM_TYPE_DOWNLOAD)
        graphItemDao.insertItem(graphItem)
    }

    override fun saveUploadGraphItem(testUUID: String, progress: Int, speedBps: Long) {
        val graphItem = GraphItem(testUUID = testUUID, progress = progress, value = speedBps, type = GRAPH_ITEM_TYPE_UPLOAD)
        graphItemDao.insertItem(graphItem)
    }

    override fun getDownloadGraphItems(testUUID: String): List<GraphItem> {
        return graphItemDao.getDownloadGraph(testUUID)
    }

    override fun getUploadGraphItems(testUUID: String): List<GraphItem> {
        return graphItemDao.getUploadGraph(testUUID)
    }
}