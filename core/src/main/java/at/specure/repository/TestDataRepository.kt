package at.specure.repository

import androidx.lifecycle.LiveData
import at.rtr.rmbt.client.Ping
import at.specure.database.entity.CapabilitiesRecord
import at.specure.database.entity.GraphItemRecord
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.location.LocationInfo
import at.specure.location.cell.CellLocationInfo

interface TestDataRepository {

    fun saveGeoLocation(testUUID: String, location: LocationInfo)

    fun saveTrafficDownload(testUUID: String, threadId: Int, timeNanos: Long, bytes: Long)

    fun saveTrafficUpload(testUUID: String, threadId: Int, timeNanos: Long, bytes: Long)

    fun saveDownloadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun saveUploadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun getDownloadGraphItemsLiveData(testUUID: String): LiveData<List<GraphItemRecord>>

    fun getUploadGraphItemsLiveData(testUUID: String): LiveData<List<GraphItemRecord>>

    fun saveSignalStrength(
        testUUID: String,
        cellUUID: String,
        mobileNetworkType: MobileNetworkType?,
        info: SignalStrengthInfo,
        testStartTimeNanos: Long
    )

    fun saveCellInfo(testUUID: String, infoList: List<NetworkInfo>)

    fun getCapabilities(testUUID: String): CapabilitiesRecord

    fun saveCapabilities(testUUID: String, rmbtHttp: Boolean, qosSupportsInfo: Boolean, classificationCount: Int)

    fun savePermissionStatus(testUUID: String, permission: String, granted: Boolean)

    fun saveCellLocation(testUUID: String, info: CellLocationInfo)

    fun saveAllPingValues(testUUID: String, pings: List<Ping>)
}