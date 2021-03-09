package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rtr.rmbt.client.helper.TestStatus
import at.specure.data.entity.CapabilitiesRecord
import at.specure.data.entity.ConnectivityStateRecord
import at.specure.data.entity.GraphItemRecord
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.TestRecord
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.location.LocationInfo
import at.specure.location.cell.CellLocationInfo
import org.json.JSONArray

interface TestDataRepository {

    fun saveGeoLocation(testUUID: String, location: LocationInfo, testStartTimeNanos: Long, filterOldValues: Boolean)

    fun saveSpeedData(testUUID: String, threadId: Int, bytes: Long, timestampNanos: Long, isUpload: Boolean)

    fun saveDownloadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun saveUploadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun getDownloadGraphItemsLiveData(testUUID: String, loadDownloadGraphItems: (List<GraphItemRecord>) -> Unit)

    fun getUploadGraphItemsLiveData(testUUID: String, loadUploadGraphItems: (List<GraphItemRecord>) -> Unit)

    fun saveSignalStrength(
        testUUID: String,
        cellUUID: String,
        mobileNetworkType: MobileNetworkType?,
        info: SignalStrengthInfo,
        testStartTimeNanos: Long,
        nrConnectionState: NRConnectionState
    )

    fun saveCellInfo(testUUID: String, infoList: List<NetworkInfo>, testStartTimeNanos: Long)

    fun getCapabilities(testUUID: String): CapabilitiesRecord

    fun saveCapabilities(testUUID: String, rmbtHttp: Boolean, qosSupportsInfo: Boolean, classificationCount: Int)

    fun savePermissionStatus(testUUID: String, permission: String, granted: Boolean)

    fun saveCellLocation(testUUID: String, info: CellLocationInfo, startTimeNanos: Long)

    fun saveAllPingValues(testUUID: String, clientPing: Long, serverPing: Long, timeNs: Long)

    fun saveTelephonyInfo(
        testUUID: String,
        networkInfo: CellNetworkInfo?,
        operatorName: String?,
        networkSimOperator: String?,
        networkCountry: String?,
        simCountry: String?,
        simOperatorName: String?,
        phoneType: String?,
        dataState: String?,
        simCount: Int
    )

    fun saveWlanInfo(testUUID: String, wifiInfo: WifiNetworkInfo)

    fun saveTest(test: TestRecord)

    fun update(testRecord: TestRecord, onUpdated: () -> Unit)

    fun saveQoSResults(testUUID: String, testToken: String, qosData: JSONArray, onUpdated: () -> Unit)

    fun updateQoSTestStatus(testUUID: String, status: TestStatus?)

    fun saveLoopMode(loopModeRecord: LoopModeRecord)

    fun updateLoopMode(loopModeRecord: LoopModeRecord)

    fun saveConnectivityState(state: ConnectivityStateRecord)

    fun getLoopModeByLocal(loopUUID: String): LiveData<LoopModeRecord?>
}