package at.specure.repository

import androidx.lifecycle.LiveData
import at.rmbt.util.io
import at.specure.database.CoreDatabase
import at.specure.database.entity.Capabilities
import at.specure.database.entity.GeoLocation
import at.specure.database.entity.GraphItem
import at.specure.database.entity.PermissionStatus
import at.specure.database.entity.Signal
import at.specure.database.entity.TestTrafficDownload
import at.specure.database.entity.TestTrafficUpload
import at.specure.info.network.MobileNetworkType
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthInfoGsm
import at.specure.info.strength.SignalStrengthInfoLte
import at.specure.info.strength.SignalStrengthInfoWiFi
import at.specure.location.LocationInfo

class TestDataRepositoryImpl(db: CoreDatabase) : TestDataRepository {

    private val geoLocationDao = db.geoLocationDao()
    private val graphItemDao = db.graphItemsDao()
    private val testTrafficDao = db.testTrafficItemDao()
    private val signalDao = db.signalDao()
    private val capabilitiesDao = db.capabilitiesDao()
    private val permissionStatusDao = db.permissionStatusDao()

    override fun saveGeoLocation(testUUID: String, location: LocationInfo) = io {
        val geoLocation = GeoLocation(
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
        val graphItem = GraphItem(testUUID = testUUID, progress = progress, value = speedBps, type = GraphItem.GRAPH_ITEM_TYPE_DOWNLOAD)
        graphItemDao.insertItem(graphItem)
    }

    override fun saveUploadGraphItem(testUUID: String, progress: Int, speedBps: Long) = io {
        val graphItem = GraphItem(testUUID = testUUID, progress = progress, value = speedBps, type = GraphItem.GRAPH_ITEM_TYPE_UPLOAD)
        graphItemDao.insertItem(graphItem)
    }

    override fun getDownloadGraphItemsLiveData(testUUID: String): LiveData<List<GraphItem>> {
        return graphItemDao.getDownloadGraphLiveData(testUUID)
    }

    override fun getUploadGraphItemsLiveData(testUUID: String): LiveData<List<GraphItem>> {
        return graphItemDao.getUploadGraphLiveData(testUUID)
    }

    override fun saveTrafficDownload(testUUID: String, threadId: Int, timeNanos: Long, bytes: Long) = io {
        val item = TestTrafficDownload(
            testUUID = testUUID,
            threadNumber = threadId,
            timeNanos = timeNanos,
            bytes = bytes
        )
        testTrafficDao.insertDownloadItem(item)
    }

    override fun saveTrafficUpload(testUUID: String, threadId: Int, timeNanos: Long, bytes: Long) = io {
        val item = TestTrafficUpload(
            testUUID = testUUID,
            threadNumber = threadId,
            timeNanos = timeNanos,
            bytes = bytes
        )
        testTrafficDao.insertUploadItem(item)
    }

    override fun saveSignalStrength(testUUID: String, cellUUID: String, mobileNetworkType: MobileNetworkType?, info: SignalStrengthInfo) = io {
        var networkTypeId: Int = mobileNetworkType?.intValue ?: 0
        val signal = info.value
        var wifiLinkSpeed: Int? = null
        // 2G/3G
        var timeNanos: Long? = null
        var timeNanosLast: Long? = null
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
                networkTypeId = 99 // TODO constant from old app
            }
            is SignalStrengthInfoGsm -> {
                timeNanos = System.nanoTime()
                timeNanosLast = System.nanoTime()
                bitErrorRate = info.bitErrorRate
                timingAdvance = info.timingAdvance
            }
        }

        val item = Signal(
            testUUID = testUUID,
            cellUuid = cellUUID,
            networkTypeId = networkTypeId,
            signal = signal,
            wifiLinkSpeed = wifiLinkSpeed,
            timeNanos = timeNanos,
            timeNanosLast = timeNanosLast,
            bitErrorRate = bitErrorRate,
            lteRsrp = lteRsrp,
            lteRsrq = lteRsrq,
            lteRssnr = lteRssnr,
            lteCqi = lteCqi,
            timingAdvance = timingAdvance
        )
        signalDao.insert(item)
    }

    override fun savePermissionStatus(testUUID: String, permission: String, granted: Boolean) {
        val permissionStatus = PermissionStatus(testUUID = testUUID, permissionName = permission, status = granted)
        permissionStatusDao.insert(permissionStatus)
    }

    override fun getCapabilities(testUUID: String): Capabilities {
        return capabilitiesDao.getCapabilitiesForTest(testUUID)
    }

    override fun saveCapabilities(testUUID: String, rmbtHttp: Boolean, qosSupportsInfo: Boolean, classificationCount: Int) {
        val capabilities = Capabilities(
            testUUID = testUUID,
            rmbtHttpStatus = rmbtHttp,
            qosSupportInfo = qosSupportsInfo,
            classificationCount = classificationCount
        )
        capabilitiesDao.insert(capabilities)
    }
}