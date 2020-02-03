package at.specure.data.repository

import android.os.SystemClock
import at.rmbt.util.io
import at.rtr.rmbt.client.helper.TestStatus
import at.specure.data.CoreDatabase
import at.specure.data.entity.CapabilitiesRecord
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.GeoLocationRecord
import at.specure.data.entity.GraphItemRecord
import at.specure.data.entity.PermissionStatusRecord
import at.specure.data.entity.PingRecord
import at.specure.data.entity.QoSResultRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.SpeedRecord
import at.specure.data.entity.TestRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.CellTechnology
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthInfoGsm
import at.specure.info.strength.SignalStrengthInfoLte
import at.specure.info.strength.SignalStrengthInfoWiFi
import at.specure.location.LocationInfo
import at.specure.location.cell.CellLocationInfo
import org.json.JSONArray
import java.text.DecimalFormat

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

    override fun saveGeoLocation(testUUID: String, location: LocationInfo) = io {
        val geoLocation = GeoLocationRecord(
            testUUID = testUUID,
            latitude = location.latitude,
            longitude = location.longitude,
            provider = location.provider,
            speed = location.speed,
            altitude = location.altitude,
            timestampMillis = location.time,
            timeRelativeNanos = location.elapsedRealtimeNanos,
            ageNanos = location.ageNanos,
            accuracy = location.accuracy,
            bearing = location.bearing,
            satellitesCount = location.satellites,
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

    override fun saveSignalStrength(
        testUUID: String,
        cellUUID: String,
        mobileNetworkType: MobileNetworkType?,
        info: SignalStrengthInfo,
        testStartTimeNanos: Long
    ) = io {
        saveSignalStrengthDirectly(testUUID, cellUUID, mobileNetworkType, info, testStartTimeNanos)
    }

    private fun saveSignalStrengthDirectly(
        testUUID: String,
        cellUUID: String,
        mobileNetworkType: MobileNetworkType?,
        info: SignalStrengthInfo,
        testStartTimeNanos: Long
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

        when (info) {
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

        val startTimestampNsSinceBoot = testStartTimeNanos + (SystemClock.elapsedRealtimeNanos() - System.nanoTime())
        val timeNanos = info.timestampNanos - startTimestampNsSinceBoot
        val timeNanosLast = if (info.timestampNanos < startTimestampNsSinceBoot) info.timestampNanos - startTimestampNsSinceBoot else null

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
            transportType = info.transport
        )
        signalDao.insert(item)
    }

    override fun saveCellInfo(testUUID: String, infoList: List<NetworkInfo>, testStartTimeNanos: Long) = io {
        val cellInfo = mutableListOf<CellInfoRecord>()
        infoList.forEach { info ->
            val mapped = when (info) {
                is WifiNetworkInfo -> info.toCellInfoRecord(testUUID)
                is CellNetworkInfo -> {
                    info.signalStrength?.let {
                        saveSignalStrengthDirectly(testUUID, info.cellUUID, info.networkType, it, testStartTimeNanos)
                    }
                    info.toCellInfoRecord(testUUID)
                }
                else -> throw IllegalArgumentException("Don't know how to save ${info.javaClass.simpleName} info into db")
            }
            cellInfo.add(mapped)
        }
        cellInfoDao.clearInsert(testUUID, cellInfo)
    }

    private fun WifiNetworkInfo.toCellInfoRecord(testUUID: String) = CellInfoRecord(
        testUUID = testUUID,
        uuid = cellUUID,
        isActive = true,
        cellTechnology = null,
        transportType = type,
        registered = true,
        areaCode = null,
        channelNumber = band.channelNumber,
        locationId = null,
        mcc = null,
        mnc = null,
        primaryScramblingCode = null
    )

    private fun CellNetworkInfo.toCellInfoRecord(testUUID: String) = CellInfoRecord(
        testUUID = testUUID,
        uuid = cellUUID,
        isActive = isActive,
        cellTechnology = CellTechnology.fromMobileNetworkType(networkType),
        transportType = type,
        registered = isRegistered,
        areaCode = areaCode,
        channelNumber = band?.channel,
        locationId = locationId,
        mcc = mcc,
        mnc = mnc,
        primaryScramblingCode = scramblingCode
    )

    override fun savePermissionStatus(testUUID: String, permission: String, granted: Boolean) = io {
        val permissionStatus = PermissionStatusRecord(testUUID = testUUID, permissionName = permission, status = granted)
        permissionStatusDao.insert(permissionStatus)
    }

    override fun getCapabilities(testUUID: String): CapabilitiesRecord {
        return capabilitiesDao.get(testUUID)
    }

    override fun saveCapabilities(testUUID: String, rmbtHttp: Boolean, qosSupportsInfo: Boolean, classificationCount: Int) = io {
        val capabilities = CapabilitiesRecord(
            testUUID = testUUID,
            rmbtHttpStatus = rmbtHttp,
            qosSupportInfo = qosSupportsInfo,
            classificationCount = classificationCount
        )
        capabilitiesDao.insert(capabilities)
    }

    override fun saveCellLocation(testUUID: String, info: CellLocationInfo) = io {
        val record = CellLocationRecord(
            testUUID = testUUID,
            scramblingCode = info.scramblingCode,
            areaCode = info.areaCode,
            locationId = info.locationId,
            timestampNanos = info.timestampNanos,
            timestampMillis = info.timestampMillis
        )
        cellLocationDao.insert(record)
    }

    override fun saveAllPingValues(testUUID: String, clientPing: Long, serverPing: Long, timeNs: Long) {
        val record = PingRecord(testUUID = testUUID, value = clientPing, valueServer = serverPing, testTimeNanos = timeNs)
        pingDao.insert(record)
    }

    override fun saveTelephonyInfo(
        testUUID: String,
        networkInfo: CellNetworkInfo?,
        operatorName: String?,
        networkOperator: String?,
        networkCountry: String?,
        simCountry: String?,
        simOperatorName: String?,
        phoneType: String?,
        dataState: String?,
        simCount: Int
    ) = io {
        val networkSimOperator = when {
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
            networkId = if (wifiInfo.networkId == -1) null else wifiInfo.networkId.toString()
        )
        testDao.insert(record)
    }

    override fun saveTest(test: TestRecord) = io {
        testDao.insert(test)
    }

    override fun update(testRecord: TestRecord, onUpdated: () -> Unit) = io {
        testDao.update(testRecord)
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
        testDao.updateQoSTestStatus(testUUID, status?.ordinal)
    }
}