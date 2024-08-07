package at.specure.data.repository

import android.os.SystemClock
import androidx.lifecycle.LiveData
import at.rmbt.util.io
import at.rtr.rmbt.client.helper.TestStatus
import at.specure.data.CoreDatabase
import at.specure.data.entity.CapabilitiesRecord
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.ConnectivityStateRecord
import at.specure.data.entity.GeoLocationRecord
import at.specure.data.entity.GraphItemRecord
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.PermissionStatusRecord
import at.specure.data.entity.PingRecord
import at.specure.data.entity.QoSResultRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.SpeedRecord
import at.specure.data.entity.TestRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord
import at.specure.data.entity.VoipTestResultRecord
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.CellTechnology
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthInfoCommon
import at.specure.info.strength.SignalStrengthInfoGsm
import at.specure.info.strength.SignalStrengthInfoLte
import at.specure.info.strength.SignalStrengthInfoNr
import at.specure.info.strength.SignalStrengthInfoWiFi
import at.specure.location.LocationInfo
import at.specure.location.cell.CellLocationInfo
import org.json.JSONArray
import timber.log.Timber
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class TestDataRepositoryImpl(db: CoreDatabase) : TestDataRepository {

    private val geoLocationDao = db.geoLocationDao()
    private val graphItemDao = db.graphItemsDao()
    private val speedDao = db.speedDao()
    private val signalDao = db.signalDao()
    private val capabilitiesDao = db.capabilitiesDao()
    private val permissionStatusDao = db.permissionStatusDao()
    private val cellInfoDao = db.cellInfoDao()
    private val cellLocationDao = db.cellLocationDao()
    private val pingDao = db.pingDao()
    private val testDao = db.testDao()
    private val voipResultsDao = db.jplResultsDao()
    private val connectivityStateDao = db.connectivityStateDao()

    override fun saveGeoLocation(testUUID: String?, signalChunkId: String?, location: LocationInfo, testStartTimeNanos: Long, filterOldValues: Boolean) = io {
        if (filterOldValues) {
            val timeDiff = TimeUnit.MINUTES.toMillis(1)
            val locationAgeDiff = System.currentTimeMillis() - location.time

            val ageAcceptable = TimeUnit.MINUTES.toNanos(1)
            val locationAge = location.ageNanos

            val locationTimeIsOutOfBounds = timeDiff < locationAgeDiff
            val locationAgeIsOutOfBounds = locationAge > ageAcceptable

            if (locationTimeIsOutOfBounds && locationAgeIsOutOfBounds) {
                return@io
            }
        }

        val geoLocation = GeoLocationRecord(
            testUUID = testUUID,
            signalChunkId = signalChunkId,
            latitude = location.latitude,
            longitude = location.longitude,
            provider = location.provider,
            speed = location.speed,
            altitude = location.altitude,
            timestampMillis = location.time, // time of acquired information directly from android API
            timeRelativeNanos = location.systemNanoTime - testStartTimeNanos, // relative time to the start of the test
            ageNanos = location.ageNanos,
            accuracy = location.accuracy,
            bearing = location.bearing,
            satellitesCount = location.satellites ?: 0,
            isMocked = location.locationIsMocked
        )
        geoLocationDao.insert(geoLocation)
    }

    override fun saveDownloadGraphItem(testUUID: String, progress: Int, speedBps: Long) = io {
        val graphItem = GraphItemRecord(
            testUUID = testUUID,
            progress = progress,
            value = speedBps,
            type = GraphItemRecord.GRAPH_ITEM_TYPE_DOWNLOAD
        )
        graphItemDao.insertItem(graphItem)
    }

    override fun saveUploadGraphItem(testUUID: String, progress: Int, speedBps: Long) = io {
        val graphItem = GraphItemRecord(
            testUUID = testUUID,
            progress = progress,
            value = speedBps,
            type = GraphItemRecord.GRAPH_ITEM_TYPE_UPLOAD
        )
        graphItemDao.insertItem(graphItem)
    }

    override fun getDownloadGraphItemsLiveData(testUUID: String, loadDownloadGraphItems: (List<GraphItemRecord>) -> Unit) = io {
        loadDownloadGraphItems.invoke(graphItemDao.getDownloadGraphLiveData(testUUID))
    }

    override fun getUploadGraphItemsLiveData(testUUID: String, loadUploadGraphItems: (List<GraphItemRecord>) -> Unit) = io {
        loadUploadGraphItems.invoke(graphItemDao.getUploadGraphLiveData(testUUID))
    }

    override fun saveSpeedData(testUUID: String, threadId: Int, bytes: Long, timestampNanos: Long, isUpload: Boolean) = io {
        val record = SpeedRecord(
            testUUID = testUUID,
            threadId = threadId,
            bytes = bytes,
            timestampNanos = timestampNanos,
            isUpload = isUpload
        )
        speedDao.insert(record)
    }

    override fun validateSignalStrengthInfo(mobileNetworkType: MobileNetworkType?, info: SignalStrengthInfo, cellUUID: String): Boolean {
        if (cellUUID.isEmpty()) return false

        val technologyClass = CellTechnology.fromMobileNetworkType(mobileNetworkType ?: MobileNetworkType.UNKNOWN)

        val isSignalValid = when {
            (info is SignalStrengthInfoLte && (technologyClass == CellTechnology.CONNECTION_4G || technologyClass == CellTechnology.CONNECTION_4G_5G)) -> true
            (info is SignalStrengthInfoLte && (technologyClass == CellTechnology.CONNECTION_3G || technologyClass == CellTechnology.CONNECTION_2G || technologyClass == CellTechnology.CONNECTION_5G)) -> false
            (info is SignalStrengthInfoNr && technologyClass == CellTechnology.CONNECTION_5G) -> true
            (info is SignalStrengthInfoNr && (technologyClass == CellTechnology.CONNECTION_4G_5G || technologyClass == CellTechnology.CONNECTION_4G || technologyClass == CellTechnology.CONNECTION_3G || technologyClass == CellTechnology.CONNECTION_2G)) -> false
            (info is SignalStrengthInfoGsm && (technologyClass == CellTechnology.CONNECTION_2G || technologyClass == CellTechnology.CONNECTION_3G)) -> true
            (info is SignalStrengthInfoGsm && (technologyClass == CellTechnology.CONNECTION_5G || technologyClass == CellTechnology.CONNECTION_4G_5G || technologyClass == CellTechnology.CONNECTION_4G)) -> false
            (info is SignalStrengthInfoCommon && (technologyClass == CellTechnology.CONNECTION_2G || technologyClass == CellTechnology.CONNECTION_3G)) -> true // this might be TDSCDMA, WCDMA, CDMA or some unknown type
            (info is SignalStrengthInfoCommon && (technologyClass == CellTechnology.CONNECTION_5G || technologyClass == CellTechnology.CONNECTION_4G_5G || technologyClass == CellTechnology.CONNECTION_4G)) -> false
            (info is SignalStrengthInfoWiFi) -> true // accepting all wifi signals as valid
            (mobileNetworkType == null || mobileNetworkType == MobileNetworkType.UNKNOWN) -> true
            else -> false
        }
        Timber.d("Signal valid?  ${mobileNetworkType?.displayName}  ${info.transport}   ${info::class.java.name}    $cellUUID   $technologyClass    $isSignalValid")
        return isSignalValid
    }

    override fun saveSignalStrength(
        testUUID: String?,
        signalChunkId: String?,
        cellUUID: String,
        mobileNetworkType: MobileNetworkType?,
        info: SignalStrengthInfo,
        testStartTimeNanos: Long,
        nrConnectionState: NRConnectionState
    ) = io {
        saveSignalStrengthDirectly(testUUID, signalChunkId, cellUUID, mobileNetworkType, info, testStartTimeNanos, nrConnectionState)
    }

    private fun saveSignalStrengthDirectly(
        testUUID: String?,
        signalChunkId: String?,
        cellUUID: String,
        mobileNetworkType: MobileNetworkType?,
        info: SignalStrengthInfo,
        testStartTimeNanos: Long,
        nrConnectionState: NRConnectionState
    ) {
        var signal = info.value
        var wifiLinkSpeed: Int? = null
        // 2G/3G
        var bitErrorRate: Int? = null
        // 4G
        var lteRsrp: Int? = null
        var lteRsrq: Int? = null
        var lteRssnr: Int? = null
        var lteCqi: Int? = null
        var timingAdvance: Int? = null

        var nrCsiRsrp: Int? = null
        var nrCsiRsrq: Int? = null
        var nrCsiSinr: Int? = null
        var nrSsRsrp: Int? = null
        var nrSsRsrq: Int? = null
        var nrSsSinr: Int? = null

        when (info) {
            is SignalStrengthInfoNr -> {
                nrCsiRsrp = info.csiRsrp
                nrCsiRsrq = info.csiRsrq
                nrCsiSinr = info.csiSinr
                nrSsRsrp = info.ssRsrp
                nrSsRsrq = info.ssRsrq
                nrSsSinr = info.ssSinr
            }
            is SignalStrengthInfoLte -> {
                signal = null
                lteRsrp = info.rsrp
                lteRsrq = info.rsrq
                lteRssnr = info.rssnr
                lteCqi = info.cqi
                timingAdvance = info.timingAdvance
            }
            is SignalStrengthInfoWiFi -> {
                wifiLinkSpeed = info.linkSpeed
            }
            is SignalStrengthInfoGsm -> {
                bitErrorRate = info.bitErrorRate
                timingAdvance = info.timingAdvance
            }
        }

//        Timber.d("Signal saving time 1: ${info.timestampNanos}  starting time: $testStartTimeNanos   current time: ${System.nanoTime()}")
        val startTimestampNsSinceBoot = testStartTimeNanos + (SystemClock.elapsedRealtimeNanos() - System.nanoTime())
        val timeNanos = info.timestampNanos - testStartTimeNanos
        var timeNanosLast = if (info.timestampNanos < startTimestampNsSinceBoot) info.timestampNanos - startTimestampNsSinceBoot else null
        if (timeNanosLast == 0L) {
            timeNanosLast = null
        }

        val item = SignalRecord(
            testUUID = testUUID,
            cellUuid = cellUUID,
            signal = signal,
            wifiLinkSpeed = wifiLinkSpeed,
            timeNanos = timeNanos,
            timeNanosLast = timeNanosLast,
            bitErrorRate = bitErrorRate,
            lteRsrp = lteRsrp,
            lteRsrq = lteRsrq,
            lteRssnr = lteRssnr,
            lteCqi = lteCqi,
            timingAdvance = timingAdvance,
            mobileNetworkType = mobileNetworkType,
            transportType = info.transport,
            nrConnectionState = nrConnectionState,
            nrCsiRsrp = nrCsiRsrp,
            nrCsiRsrq = nrCsiRsrq,
            nrCsiSinr = nrCsiSinr,
            nrSsRsrp = nrSsRsrp,
            nrSsRsrq = nrSsRsrq,
            nrSsSinr = nrSsSinr,
            source = info.source,
            signalChunkId = signalChunkId
        )
        signalDao.insert(item)
    }

    override fun saveCellInfoRecord(cellInfoRecordList: List<CellInfoRecord>) = io {
        if (cellInfoRecordList.isNotEmpty()) {
            cellInfoDao.clearInsert(cellInfoRecordList[0].testUUID, cellInfoRecordList[0].signalChunkId, cellInfoRecordList)
        }
    }

    @Synchronized
    override fun saveSignalRecord(signalRecordList: List<SignalRecord>, filterOldValues: Boolean) = io {
        synchronized(signalRecordList) {
            signalRecordList.forEach {
                if (filterOldValues) {
                    val lastSignal: SignalRecord? = signalDao.getLatestForCell(it.testUUID, it.signalChunkId, it.cellUuid)
                    if (lastSignal != null) {
                        val distinct = isSignalSignificantlyDistinct(it, lastSignal)
                        Timber.d("Distinct Signal Values $distinct: $it and $lastSignal")
                        if (distinct) {
                            signalDao.insert(it)
                        }
                    } else {
                        Timber.d("Distinct Signal Values true because of non previous signal")
                        signalDao.insert(it)
                    }
                } else {
                    signalDao.insert(it)
                }
            }
        }
    }

    private fun isSignalSignificantlyDistinct(signalRecord: SignalRecord, lastSignal: SignalRecord): Boolean {
        return (isValueSignificantlyDifferent(signalRecord.lteRsrp, lastSignal.lteRsrp) &&
                isValueSignificantlyDifferent(signalRecord.bitErrorRate, lastSignal.bitErrorRate) &&
                isValueSignificantlyDifferent(signalRecord.lteCqi, lastSignal.lteCqi) &&
                isValueSignificantlyDifferent(signalRecord.lteRsrq, lastSignal.lteRsrq) &&
                isValueSignificantlyDifferent(signalRecord.lteRssnr, lastSignal.lteRssnr) &&
                isValueSignificantlyDifferent(signalRecord.nrCsiRsrp, lastSignal.nrCsiRsrp) &&
                isValueSignificantlyDifferent(signalRecord.nrCsiRsrq, lastSignal.nrCsiRsrq) &&
                isValueSignificantlyDifferent(signalRecord.nrCsiSinr, lastSignal.nrCsiSinr) &&
                isValueSignificantlyDifferent(signalRecord.nrSsRsrp, lastSignal.nrSsRsrp) &&
                isValueSignificantlyDifferent(signalRecord.nrSsRsrq, lastSignal.nrSsRsrq) &&
                isValueSignificantlyDifferent(signalRecord.nrSsSinr, lastSignal.nrSsSinr) &&
                isValueSignificantlyDifferent(signalRecord.signal, lastSignal.signal) &&
                isValueSignificantlyDifferent(signalRecord.timingAdvance, lastSignal.timingAdvance) &&
                isValueSignificantlyDifferent(signalRecord.wifiLinkSpeed, lastSignal.wifiLinkSpeed))
    }

    private fun isValueSignificantlyDifferent(first: Int?, second: Int?): Boolean {
        return if ((first != null && second == null) || (first == null && second != null)) {
            true
        } else if (first == null && second == null) {
            false
        } else {
            val range = (second?.minus(1))!!..(second.plus(1)!!)
            val inRange = first in range
            !inRange
        }
    }

    override fun saveCellLocationRecord(cellLocationRecordList: List<CellLocationRecord>) = io {
        if (cellLocationRecordList.isNotEmpty()) {
            cellLocationDao.insertNew(cellLocationRecordList[0].testUUID, cellLocationRecordList[0].signalChunkId, cellLocationRecordList)
        }
    }

    override fun saveCellInfo(testUUID: String?, signalChunkId: String?, infoList: List<NetworkInfo>, testStartTimeNanos: Long) = io {
        val cellInfo = mutableListOf<CellInfoRecord>()
        infoList.forEach { info ->
            val mapped = when (info) {
                is WifiNetworkInfo -> info.toCellInfoRecord(testUUID, signalChunkId)
                is CellNetworkInfo -> {
                    info.signalStrength?.let {
//                        Timber.d("Signal saving time SCI: starting time: $testStartTimeNanos   current time: ${System.nanoTime()}")
//                        Timber.d("valid signal directly")
                        if (info.cellUUID.isNotEmpty() && validateSignalStrengthInfo(info.networkType, it, info.cellUUID)) {
                            saveSignalStrengthDirectly(testUUID, signalChunkId, info.cellUUID, info.networkType, it, testStartTimeNanos, info.nrConnectionState)
                        }
                    }
                    info.toCellInfoRecord(testUUID, signalChunkId)
                }
                else -> throw IllegalArgumentException("Don't know how to save ${info.javaClass.simpleName} info into db")
            }
            if (mapped.uuid.isNotEmpty()) {
                cellInfo.add(mapped)
            }
        }
        cellInfoDao.clearInsert(testUUID, signalChunkId, cellInfo)
    }

    private fun WifiNetworkInfo.toCellInfoRecord(testUUID: String?, signalChunkId: String?) = CellInfoRecord(
        testUUID = testUUID,
        uuid = cellUUID,
        isActive = true,
        cellTechnology = null,
        transportType = type,
        registered = true,
        areaCode = null,
        channelNumber = band.channelNumber,
        frequency = band.frequency.toDouble(),
        locationId = null,
        mcc = null,
        mnc = null,
        primaryScramblingCode = null,
        dualSimDetectionMethod = null,
        isPrimaryDataSubscription = null,
        signalChunkId = signalChunkId,
        cellState = null
    )

    private fun CellNetworkInfo.toCellInfoRecord(testUUID: String?, signalChunkId: String?): CellInfoRecord {
        val cellInfoRecord = CellInfoRecord(
            testUUID = testUUID,
            signalChunkId = signalChunkId,
            uuid = cellUUID,
            isActive = isActive,
            cellTechnology = cellType,
            transportType = type,
            registered = isRegistered,
            areaCode = areaCode,
            channelNumber = band?.channel,
            frequency = band?.frequencyDL,
            locationId = locationId,
            mcc = mcc,
            mnc = mnc,
            primaryScramblingCode = scramblingCode,
            dualSimDetectionMethod = dualSimDetectionMethod,
            isPrimaryDataSubscription = isPrimaryDataSubscription?.value,
            cellState = cellState
        )
        Timber.d("Saving CellInfo Record TDR with uuid: ${cellInfoRecord.uuid} and cellTechnology: ${cellInfoRecord.cellTechnology?.name} and channel number: ${cellInfoRecord.channelNumber}")
        return cellInfoRecord
    }

    override fun savePermissionStatus(testUUID: String?, signalChunkId: String?, permission: String, granted: Boolean) = io {
        val permissionStatus = PermissionStatusRecord(testUUID = testUUID, signalChunkId = signalChunkId, permissionName = permission, status = granted)
        permissionStatusDao.insert(permissionStatus)
    }

    override fun getCapabilities(testUUID: String?, signalChunkId: String?): CapabilitiesRecord {
        return capabilitiesDao.get(testUUID, signalChunkId)
    }

    override fun saveCapabilities(testUUID: String?, signalChunkId: String?, rmbtHttp: Boolean, qosSupportsInfo: Boolean, classificationCount: Int) = io {
        val capabilities = CapabilitiesRecord(
            testUUID = testUUID,
            signalChunkId = signalChunkId,
            rmbtHttpStatus = rmbtHttp,
            qosSupportInfo = qosSupportsInfo,
            classificationCount = classificationCount
        )
        capabilitiesDao.insert(capabilities)
    }

    override fun saveCellLocation(testUUID: String?, signalChunkId: String?,  info: CellLocationInfo, startTimeNanos: Long) = io {
        val record = CellLocationRecord(
            testUUID = testUUID,
            signalChunkId = signalChunkId,
            scramblingCode = info.scramblingCode,
            areaCode = info.areaCode,
            locationId = info.locationId,
            timestampNanos = info.timestampNanos - startTimeNanos,
            timestampMillis = info.timestampMillis
        )
        cellLocationDao.insertNew(testUUID, signalChunkId, listOf(record))
    }

    override fun saveAllPingValues(testUUID: String, clientPing: Long, serverPing: Long, timeNs: Long) {
        val record = PingRecord(testUUID = testUUID, value = clientPing, valueServer = serverPing, testTimeNanos = timeNs)
        pingDao.insert(record)
    }

    override fun saveTelephonyInfo(
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
    ) = io {
        val networkOperator = when {
            networkInfo?.mcc == null -> null
            networkInfo.mnc == null -> null
            else -> "${networkInfo.mcc}-${DecimalFormat("00").format(networkInfo.mnc)}"
        }
        val record = TestTelephonyRecord(
            testUUID = testUUID,
            networkOperatorName = operatorName,
            networkOperator = networkOperator,
            networkIsRoaming = networkInfo?.isRoaming,
            networkCountry = networkCountry,
            networkSimCountry = simCountry,
            networkSimOperator = networkSimOperator,
            networkSimOperatorName = simOperatorName,
            phoneType = phoneType,
            dataState = dataState,
            apn = networkInfo?.apn,
            simCount = simCount,
            hasDualSim = simCount > 1
        )
        testDao.insert(record)
    }

    override fun saveWlanInfo(testUUID: String, wifiInfo: WifiNetworkInfo) = io {
        val record = TestWlanRecord(
            testUUID = testUUID,
            supplicantState = wifiInfo.supplicantState,
            supplicantDetailedState = wifiInfo.supplicantDetailedState,
            ssid = wifiInfo.ssid,
            bssid = wifiInfo.bssid,
            networkId = wifiInfo.networkId?.toString()
        )
        testDao.insert(record)
    }

    override fun saveTest(test: TestRecord) {
        return testDao.insert(test)
    }

    override fun update(testRecord: TestRecord, onUpdated: () -> Unit) = io {
        val count = testDao.update(testRecord)
        if (count == 0) {
            Timber.e("DB: Failed to update test record")
        }
        onUpdated.invoke()
    }

    override fun saveQoSResults(testUUID: String, testToken: String, qosData: JSONArray, onUpdated: () -> Unit) = io {
        val record = QoSResultRecord(
            uuid = testUUID,
            testToken = testToken,
            timeMillis = System.currentTimeMillis(),
            results = qosData
        )
        testDao.insert(record)
        onUpdated.invoke()
    }

    override fun updateQoSTestStatus(testUUID: String, status: TestStatus?) = io {
        val count = testDao.updateQoSTestStatus(testUUID, status?.ordinal)
        if (count == 0) {
            Timber.e("DB: failed to update QOS test status to: ${status?.ordinal}")
        }
    }

    override fun saveLoopMode(loopModeRecord: LoopModeRecord) = io {
        testDao.saveLoopModeRecord(loopModeRecord)
    }

    override fun updateLoopMode(loopModeRecord: LoopModeRecord) = io {
        val count = testDao.updateLoopModeRecord(loopModeRecord)
        if (count == 0) {
            Timber.e("DB: failed to update loopModeRecord")
        }
    }

    override fun saveConnectivityState(state: ConnectivityStateRecord) = io {
        connectivityStateDao.saveState(state)
    }

    override fun getLoopModeByLocal(loopUUID: String): LiveData<LoopModeRecord?> {
        return testDao.getLoopModeRecord(loopUUID)
    }

    override fun saveVoipResult(voipTestResultRecord: VoipTestResultRecord) {
        return voipResultsDao.insert(voipTestResultRecord)
    }
}