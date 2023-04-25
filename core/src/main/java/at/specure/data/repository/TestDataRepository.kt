package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rtr.rmbt.client.helper.TestStatus
import at.specure.data.entity.CapabilitiesRecord
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.ConnectivityStateRecord
import at.specure.data.entity.GraphItemRecord
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.TestRecord
import at.specure.data.entity.VoipTestResultRecord
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

    fun saveGeoLocation(testUUID: String?, signalChunkId: String?, location: LocationInfo, testStartTimeNanos: Long, filterOldValues: Boolean)

    fun saveSpeedData(testUUID: String, threadId: Int, bytes: Long, timestampNanos: Long, isUpload: Boolean)

    fun saveDownloadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun saveUploadGraphItem(testUUID: String, progress: Int, speedBps: Long)

    fun getDownloadGraphItemsLiveData(testUUID: String, loadDownloadGraphItems: (List<GraphItemRecord>) -> Unit)

    fun getUploadGraphItemsLiveData(testUUID: String, loadUploadGraphItems: (List<GraphItemRecord>) -> Unit)

    /**
     * Mentioned only for mobile network types and signals but you can validate wifi signals too, just pass null to mobileNetworkType parameter
     * return true if signal strength was valid and saved, false otherwise
     */
    fun validateSignalStrengthInfo(mobileNetworkType: MobileNetworkType?, info: SignalStrengthInfo, cellUUID: String): Boolean

    fun saveSignalStrength(
        testUUID: String?,
        signalChunkId: String?,
        cellUUID: String,
        mobileNetworkType: MobileNetworkType?,
        info: SignalStrengthInfo,
        testStartTimeNanos: Long,
        nrConnectionState: NRConnectionState
    )

    fun saveCellInfo(testUUID: String?, signalChunkId: String?, infoList: List<NetworkInfo>, testStartTimeNanos: Long)

    fun saveCellInfoRecord(cellInfoRecordList: List<CellInfoRecord>)

    fun saveSignalRecord(signalRecordList: List<SignalRecord>, filterSimilarValues: Boolean)

    fun saveCellLocationRecord(cellLocationRecordList: List<CellLocationRecord>)

    fun getCapabilities(testUUID: String?, signalChunkId: String?): CapabilitiesRecord

    fun saveCapabilities(testUUID: String?, signalChunkId: String?, rmbtHttp: Boolean, qosSupportsInfo: Boolean, classificationCount: Int)

    fun savePermissionStatus(testUUID: String?, signalChunkId: String?, permission: String, granted: Boolean)

    fun saveCellLocation(testUUID: String?, signalChunkId: String?, info: CellLocationInfo, startTimeNanos: Long)

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

    fun saveVoipResult(voipTestResultRecord: VoipTestResultRecord)
}