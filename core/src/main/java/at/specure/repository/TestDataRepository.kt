package at.specure.repository

import at.specure.database.entity.GraphItem
import at.specure.info.cell.CellTechnology
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.location.LocationInfo

interface TestDataRepository {

    fun saveGeoLocation(testUUID: String, location: LocationInfo)

    fun saveTrafficDownload(testUUID: String, threadId: Int, timeNanos: Long, bytes: Long)

    fun saveTrafficUpload(testUUID: String, threadId: Int, timeNanos: Long, bytes: Long)

    fun saveDownloadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun saveUploadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun getDownloadGraphItems(testUUID: String): List<GraphItem>

    fun getUploadGraphItems(testUUID: String): List<GraphItem>

    fun saveSignalStrength(testUUID: String, cellUUID: String, mobileNetworkType: MobileNetworkType?, info: SignalStrengthInfo)

    fun saveActiveCellInfo(
        testUUID: String,
        cellUUID: String,
        mobileNetworkType: MobileNetworkType?,
        networkInfo: NetworkInfo?,
        cellTechnology: CellTechnology?
    )
}