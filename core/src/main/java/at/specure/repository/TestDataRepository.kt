package at.specure.repository

import at.specure.database.entity.GraphItem
import at.specure.location.LocationInfo

interface TestDataRepository {

    fun saveGeoLocation(testUUID: String, location: LocationInfo)

    fun saveDownloadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun saveUploadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun getDownloadGraphItems(testUUID: String): List<GraphItem>

    fun getUploadGraphItems(testUUID: String): List<GraphItem>

}