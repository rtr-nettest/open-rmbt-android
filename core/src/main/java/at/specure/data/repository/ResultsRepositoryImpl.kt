package at.specure.data.repository

import android.content.Context
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.QosResultResponse
import at.rmbt.client.control.TestResultBody
import at.rmbt.util.Maybe
import at.rmbt.util.io
import at.rtr.rmbt.client.helper.TestStatus
import at.specure.config.Config
import at.specure.data.Classification
import at.specure.data.ClientUUID
import at.specure.data.CoreDatabase
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.PingRecord
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.QosCategoryRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.SpeedRecord
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord
import at.specure.data.entity.VoipTestResultRecord
import at.specure.data.entity.getJitter
import at.specure.data.entity.getPacketLoss
import at.specure.data.toRequest
import at.specure.info.TransportType
import at.specure.info.network.MobileNetworkType
import at.specure.result.QoECategory
import at.specure.result.QoSCategory
import at.specure.test.DeviceInfo
import at.specure.util.exception.DataMissingException
import timber.log.Timber
import javax.inject.Inject

class ResultsRepositoryImpl @Inject constructor(
    context: Context,
    private val db: CoreDatabase,
    private val clientUUID: ClientUUID,
    private val client: ControlServerClient,
    private val config: Config
) : ResultsRepository {

    private val deviceInfo = DeviceInfo(context)

    override fun sendTestResults(testUUID: String, callback: (Maybe<Boolean>) -> Unit) = io {
        callback.invoke(sendTestResults(testUUID))
    }

    override fun sendTestResults(testUUID: String): Maybe<Boolean> {
        val testDao = db.testDao()
        val testRecord = testDao.get(testUUID) ?: throw DataMissingException("TestRecord not found uuid: $testUUID")
        val clientUUID = clientUUID.value ?: throw DataMissingException("ClientUUID is null")
        val jplTestResultsDao = db.jplResultsDao()
        val jplTestResultsRecord = jplTestResultsDao.get(testUUID)

        var finalResult: Maybe<Boolean> = Maybe(true)
        val qosRecord = testDao.getQoSRecord(testUUID)

        if (!testRecord.isSubmitted) {

            val telephonyInfo: TestTelephonyRecord? =
                if (testRecord.transportType == TransportType.CELLULAR) {
                    db.testDao().getTelephonyRecord(testUUID)
                } else {
                    null
                }

            val wlanInfo: TestWlanRecord? = if (testRecord.transportType == TransportType.WIFI) {
                db.testDao().getWlanRecord(testUUID)
            } else {
                null
            }

            val pings: List<PingRecord> = db.pingDao().get(testUUID)
            val speeds: List<SpeedRecord> = db.speedDao().get(testUUID)
            val signals: List<SignalRecord> = db.signalDao().get(testUUID)

            val body = testRecord.toRequest(
                clientUUID = clientUUID,
                deviceInfo = deviceInfo,
                telephonyInfo = telephonyInfo,
                wlanInfo = wlanInfo,
                locations = db.geoLocationDao().get(testUUID),
                capabilities = db.capabilitiesDao().get(testUUID),
                pingList = pings,
                cellInfoList = db.cellInfoDao().get(testUUID),
                signalList = signals,
                speedInfoList = speeds,
                cellLocationList = db.cellLocationDao().get(testUUID),
                permissions = db.permissionStatusDao().get(testUUID),
                jplTestResultsRecord
            )

            // save results locally in every condition the test was successful, if result will be sent and obtained successfully, it overwrites the local results
            if ((testRecord.status == TestStatus.SPEEDTEST_END) || (testRecord.status == TestStatus.QOS_END) || (testRecord.status == TestStatus.END)) {
                saveLocalTestResults(body, testUUID, wlanInfo, speeds, pings, signals, jplTestResultsRecord)
            }

            body.radioInfo?.cells?.forEach {
                Timber.d("valid cells: ${it.uuid}   technology: ${it.technology}")
            }

            val result = client.sendTestResults(body)

            result.onSuccess {
                db.testDao().updateTestIsSubmitted(testUUID)
            }

            result.onFailure {

                if ((testRecord.status != TestStatus.SPEEDTEST_END) || (testRecord.status != TestStatus.QOS_END) || (testRecord.status != TestStatus.END)) {
                    return@onFailure
                }
            }

            finalResult = result.map { result.ok }
        }

        if (finalResult.ok) {
            if (qosRecord != null) {
                val body = qosRecord.toRequest(clientUUID, deviceInfo)

                val isONTApp = !config.headerValue.isNullOrEmpty()

                val result = if (isONTApp) {
                    client.sendQoSTestResultsONT(body)
                } else {
                    client.sendQoSTestResults(body)
                }

                result.onSuccess {
                    db.testDao().updateQoSTestIsSubmitted(testUUID)
                    db.historyDao().clear()
                }

                if (isONTApp) {
                    val qosResult = (result.success as QosResultResponse)
                    db.qoeInfoDao().insert(
                        QoeInfoRecord(
                            testUUID = qosRecord.uuid,
                            category = QoECategory.QOE_QOS,
                            percentage = qosResult.overallQosPercentage ?: 0f,
                            classification = Classification.NONE,
                            priority = -1,
                            info = "${qosResult.overallQosPercentage ?: 0}%"
                        )
                    )
                    qosResult.partialQosResults.forEach { qosResultItem ->
                        db.qosCategoryDao().insert(
                            QosCategoryRecord(
                                testUUID = qosRecord.uuid,
                                category = qosResultItem.testType?.let { QoSCategory.fromString(it) }
                                    ?: QoSCategory.QOS_UNKNOWN,
                                categoryName = "",
                                categoryDescription = "",
                                language = "",
                                failedCount = qosResultItem.totalCount?.let {
                                    it - (qosResultItem.successCount ?: 0)
                                } ?: 0,
                                successCount = qosResultItem.successCount ?: 0
                            )
                        )
                    }
                }

                finalResult = result.map { result.ok }
            } else {
                db.historyDao().clear()
            }
        }

        return finalResult
    }

    private fun saveLocalTestResults(
        body: TestResultBody,
        testUUID: String,
        wlanInfo: TestWlanRecord?,
        speeds: List<SpeedRecord>,
        pings: List<PingRecord>,
        signals: List<SignalRecord>,
        jitterAndPacketLoss: VoipTestResultRecord?
    ) {
        val networkType = NetworkTypeCompat.fromResultIntType(body.networkType.toInt())

        val jitterMean: Double? = jitterAndPacketLoss?.getJitter()
        val packetLoss: Double? = jitterAndPacketLoss?.getPacketLoss()

        db.testResultDao().insert(
            TestResultRecord(
                uuid = testUUID,
                clientOpenUUID = "",
                testOpenUUID = "",
                timezone = "",
                shareText = "",
                shareTitle = "",
                locationText = "",
                longitude = body?.geoLocations?.get(0)?.longitude,
                latitude = body?.geoLocations?.get(0)?.latitude,
                timestamp = body.timeMillis,
                timeText = "",
                networkTypeRaw = body.networkType.toInt(),
                networkTypeText = if (networkType != NetworkTypeCompat.TYPE_WLAN) MobileNetworkType.fromValue(body.networkType.toInt()).displayName else NetworkTypeCompat.TYPE_WLAN.stringValue,
                networkName = if (networkType == NetworkTypeCompat.TYPE_WLAN) wlanInfo?.ssid ?: wlanInfo?.bssid else null,
                networkProviderName = body.telephonyNetworkSimOperatorName,
                networkType = networkType,
                uploadClass = Classification.fromValue(0),
                downloadClass = Classification.fromValue(0),
                downloadSpeedKbs = body.downloadSpeedKbs,
                uploadSpeedKbs = body.uploadSpeedKbs,
                signalClass = Classification.fromValue(0),
                signalStrength = body.radioInfo?.signals?.get(0)?.signal ?: body.radioInfo?.signals?.get(0)?.lteRsrp,
                pingClass = Classification.fromValue(0),
                pingMillis = body.shortestPingNanos / 1000000.toDouble(),
                isLocalOnly = true,
                jitterMillis = jitterMean,
                jitterClass = Classification.fromValue(0),
                packetLossClass = Classification.fromValue(0),
                packetLossPercents = packetLoss
            )
        )

        val uploadSpeeds = HashMap<Int, MutableList<SpeedRecord>>()
        val downloadSpeeds = HashMap<Int, MutableList<SpeedRecord>>()

        speeds.forEach {
            if (it.isUpload) {
                var threadList = uploadSpeeds[it.threadId]
                if (threadList == null) {
                    threadList = mutableListOf<SpeedRecord>()
                }
                threadList.add(it)
                uploadSpeeds[it.threadId] = threadList
            } else {
                var threadList = downloadSpeeds[it.threadId]
                if (threadList == null) {
                    threadList = mutableListOf<SpeedRecord>()
                }
                threadList.add(it)
                downloadSpeeds[it.threadId] = threadList
            }
        }

        val downloadSpeedsRecords = convertToThreadSpeeds(downloadSpeeds)
        db.testResultGraphItemDao().clearInsertItems(downloadSpeedsRecords)
        val uploadSpeedsRecords = convertToThreadSpeeds(uploadSpeeds)
        db.testResultGraphItemDao().clearInsertItems(uploadSpeedsRecords)
        val resultPings = pings.map { transformPing(it, testUUID) }
        db.testResultGraphItemDao().clearInsertItems(resultPings)
        val resultSignals = signals.map { transformSignal(it, testUUID) }
        db.testResultGraphItemDao().clearInsertItems(resultSignals)
    }

    private fun transformPing(pingRecord: PingRecord, testUUID: String): TestResultGraphItemRecord {
        return TestResultGraphItemRecord(
            testUUID = testUUID,
            time = pingRecord.testTimeNanos / 1000000,
            type = TestResultGraphItemRecord.Type.PING,
            value = pingRecord.value / 1000000
        )
    }

    private fun transformSignal(signalRecord: SignalRecord, testUUID: String): TestResultGraphItemRecord {
        return TestResultGraphItemRecord(
            testUUID = testUUID,
            time = signalRecord.timeNanos / 1000000,
            type = TestResultGraphItemRecord.Type.SIGNAL,
            value = signalRecord.signal?.toLong() ?: signalRecord.lteRsrp?.toLong() ?: 0
        )
    }

    /**
     * convert data transferred at point of time to speed at point of time
     */
    private fun convertToThreadSpeeds(dataTransferred: HashMap<Int, MutableList<SpeedRecord>>): MutableList<TestResultGraphItemRecord> {
        val currentValues = HashMap<Int, SpeedRecord?>()
        val records = mutableListOf<TestResultGraphItemRecord>()
        var shouldContinue: Boolean

        dataTransferred.keys.forEach {
            currentValues[it] = dataTransferred[it]?.get(0) as SpeedRecord
        }

        do {
            dataTransferred.keys.forEach {
                currentValues[it] =
                    if (dataTransferred[it]?.size!! > 0) dataTransferred[it]?.get(0) as SpeedRecord else null
            }
            // select item with minimum time
            val minTime = currentValues.minWithOrNull(Comparator { o1, o2 ->
                if (o1?.value == null) {
                    1
                } else {
                    if (o2?.value == null) {
                        -1
                    } else {
                        o1.value?.timestampNanos?.compareTo(o2.value?.timestampNanos ?: 0) ?: 1
                    }
                }
            })
            if (minTime?.value != null) {
                records.add(calculateSpeed(dataTransferred, minTime.value as SpeedRecord))
                dataTransferred[minTime.key]?.remove(minTime.value as SpeedRecord)
            }
            shouldContinue = true
            dataTransferred.keys.forEach {
                if (dataTransferred[it]?.isEmpty() == true) {
                    shouldContinue = false
                }
            }
        } while (shouldContinue)

        return records
    }

    private fun calculateSpeed(dataTransferred: java.util.HashMap<Int, MutableList<SpeedRecord>>, minTime: SpeedRecord): TestResultGraphItemRecord {
        var speedBPS: Long = 0
        dataTransferred.keys.forEach {
            val speedRecord = if (dataTransferred[it]?.size!! > 0) dataTransferred[it]?.get(0) as SpeedRecord else null
            speedRecord?.let {
                speedBPS += ((speedRecord.bytes) / (speedRecord.timestampNanos / 1000000000f)).toLong()
            }
        }
        return TestResultGraphItemRecord(
            testUUID = minTime.testUUID,
            value = (speedBPS * (minTime.timestampNanos / 1000000000f)).toLong(),
            time = minTime.timestampNanos / 1000000,
            type = if (minTime.isUpload) TestResultGraphItemRecord.Type.UPLOAD else TestResultGraphItemRecord.Type.DOWNLOAD
        )
    }

    override fun updateSubmissionsCounter(testUUID: String) {
        db.testDao().updateSubmissionsRetryCounter(testUUID)
    }
}