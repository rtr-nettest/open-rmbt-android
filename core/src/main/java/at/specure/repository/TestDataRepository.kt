package at.specure.repository

import androidx.lifecycle.LiveData
import at.specure.database.entity.Capabilities
import at.specure.database.entity.GraphItem
import at.specure.info.network.MobileNetworkType
import at.specure.info.strength.SignalStrengthInfo
import at.specure.location.LocationInfo

interface TestDataRepository {

    fun saveGeoLocation(testUUID: String, location: LocationInfo)

    fun saveTrafficDownload(testUUID: String, threadId: Int, timeNanos: Long, bytes: Long)

    fun saveTrafficUpload(testUUID: String, threadId: Int, timeNanos: Long, bytes: Long)

    fun saveDownloadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun saveUploadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun getDownloadGraphItemsLiveData(testUUID: String): LiveData<List<GraphItem>>

    fun getUploadGraphItemsLiveData(testUUID: String): LiveData<List<GraphItem>>

    fun saveSignalStrength(testUUID: String, cellUUID: String, mobileNetworkType: MobileNetworkType?, info: SignalStrengthInfo)

    fun getCapabilities(testUUID: String): Capabilities

    fun saveCapabilities(testUUID: String, rmbtHttp: Boolean, qosSupportsInfo: Boolean, classificationCount: Int)

    fun savePermissionStatus(testUUID: String, permission: String, granted: Boolean)
}