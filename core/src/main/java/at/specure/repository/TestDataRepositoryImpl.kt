package at.specure.repository

import androidx.lifecycle.LiveData
import at.rmbt.util.io
import at.specure.database.CoreDatabase
import at.specure.database.entity.CapabilitiesRecord
import at.specure.database.entity.CellInfoRecord
import at.specure.database.entity.CellLocationRecord
import at.specure.database.entity.GeoLocationRecord
import at.specure.database.entity.GraphItemRecord
import at.specure.database.entity.PermissionStatusRecord
import at.specure.database.entity.PingRecord
import at.specure.database.entity.SignalRecord
import at.specure.database.entity.SpeedRecord
import at.specure.database.entity.TestTelephonyRecord
import at.specure.database.entity.TestWlanRecord
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
            provider = location.provider.name,
            speed = location.speed,
            altitude = location.altitude,
            time = location.time,
            timeCorrectionNanos = location.elapsedRealtimeNanos,
            ageNanos = location.ageNanos,
            accuracy = location.accuracy,
            bearing = location.bearing,
            satellitesCount = location.satellites,
            isMocked = location.locationIsMocked
        )
        geoLocationDao.insert(geoLocation)
    }

    override fun saveDownloadGraphItem(testUUID: String, progress: Int, speedBps: Long) = io {
        val graphItem = GraphItemRecord(testUUID = testUUID, progress = progress, value = speedBps, type = GraphItemRecord.GRAPH_ITEM_TYPE_DOWNLOAD)
        graphItemDao.insertItem(graphItem)
    }

    override fun saveUploadGraphItem(testUUID: String, progress: Int, speedBps: Long) = io {
        val graphItem = GraphItemRecord(testUUID = testUUID, progress = progress, value = speedBps, type = GraphItemRecord.GRAPH_ITEM_TYPE_UPLOAD)
        graphItemDao.insertItem(graphItem)
    }

    override fun getDownloadGraphItemsLiveData(testUUID: String): List<GraphItemRecord> {
        return graphItemDao.getDownloadGraphLiveData(testUUID)
    }

    override fun getUploadGraphItemsLiveData(testUUID: String): LiveData<List<GraphItemRecord>> {
        return graphItemDao.getUploadGraphLiveData(testUUID)
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
        val signal = info.value
        var wifiLinkSpeed: Int? = null
        val timeNanos = info.timestampNanos
        val timeNanosLast = if (timeNanos < testStartTimeNanos) 0 else timeNanos - testStartTimeNanos
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

    override fun saveCellInfo(testUUID: String, infoList: List<NetworkInfo>) = io {
        val cellInfo = mutableListOf<CellInfoRecord>()
        infoList.forEach { info ->
            val mapped = when (info) {
                is WifiNetworkInfo -> info.toCellInfoRecord(testUUID)
                is CellNetworkInfo -> info.toCellInfoRecord(testUUID)
                else -> throw IllegalArgumentException("Don't know how to save ${info.javaClass.simpleName} info into db")
            }
            cellInfo.add(mapped)
        }
        cellInfoDao.clearInsert(testUUID, cellInfo)
    }

    private fun WifiNetworkInfo.toCellInfoRecord(testUUID: String) = CellInfoRecord(
        testUUID = testUUID,
        uuid = cellUUID,
        active = true,
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
        active = isActive,
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
        return capabilitiesDao.getCapabilitiesForTest(testUUID)
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
        val record = TestTelephonyRecord(
            testUUID = testUUID,
            networkOperatorName = operatorName,
            networkOperator = networkOperator,
            networkIsRoaming = networkInfo?.isRoaming,
            networkCountry = networkCountry,
            networkSimCountry = simCountry,
            networkSimOperator = "${networkInfo?.mnc}-${networkInfo?.mcc}",
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
            supplicantState = wifiInfo.supplicantState.toString(),
            supplicantDetailedState = wifiInfo.supplicantDetailedState.toString(),
            ssid = wifiInfo.ssid,
            bssid = wifiInfo.bssid,
            networkId = wifiInfo.networkId.toString()
        )
        testDao.insert(record)
    }
}