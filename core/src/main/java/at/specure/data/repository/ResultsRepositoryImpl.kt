package at.specure.data.repository

import android.content.Context
import at.rmbt.client.control.ControlServerClient
import at.rmbt.util.exception.HandledException
import at.specure.data.ClientUUID
import at.specure.data.CoreDatabase
import at.specure.data.toRequest
import at.specure.test.DeviceInfo
import at.specure.util.exception.DataMissingException
import timber.log.Timber
import javax.inject.Inject

class ResultsRepositoryImpl @Inject constructor(
    context: Context,
    private val db: CoreDatabase,
    private val clientUUID: ClientUUID,
    private val client: ControlServerClient
) :
    ResultsRepository {

    private val deviceInfo = DeviceInfo(context)

    @Throws(HandledException::class)
    override fun sendTestResults(testUUID: String) {
        val testRecord = db.testDao().get(testUUID) ?: throw DataMissingException("TestRecord not found uuid: $testUUID")
        val clientUUID = clientUUID.value ?: throw DataMissingException("ClientUUID is null")

        val body = testRecord.toRequest(
            clientUUID = clientUUID,
            deviceInfo = deviceInfo,
            telephonyInfo = db.testDao().getTelehonyRecord(testUUID),
            wlanInfo = db.testDao().getWlanRecord(testUUID),
            locations = db.geoLocationDao().get(testUUID),
            capabilities = db.capabilitiesDao().get(testUUID),
            pingList = db.pingDao().get(testUUID),
            cellInfoList = db.cellInfoDao().get(testUUID),
            signalList = db.signalDao().get(testUUID),
            speedInfoList = db.speedDao().get(testUUID),
            cellLocationList = db.cellLocationDao().get(testUUID),
            permissions = db.permissionStatusDao().get(testUUID)
        )

        val result = client.sendTestResults(body)
        Timber.d("Test Data sent")
    }
}