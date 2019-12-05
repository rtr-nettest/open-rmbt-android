package at.specure.test

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import at.rtr.rmbt.client.RMBTClientCallback
import at.rtr.rmbt.client.TotalTestResult
import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.client.v2.task.service.TestMeasurement.TrafficDirection
import at.specure.config.Config
import at.specure.database.entity.TestRecord
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.location.LocationInfo
import at.specure.location.LocationInfoLiveData
import at.specure.location.LocationWatcher
import at.specure.location.cell.CellLocationInfo
import at.specure.location.cell.CellLocationLiveData
import at.specure.location.cell.CellLocationWatcher
import at.specure.repository.TestDataRepository
import at.specure.repository.TestRepository
import at.specure.util.hasPermission
import at.specure.util.permission.PermissionsWatcher
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.floor

class StateRecorder @Inject constructor(
    private val context: Context,
    private val repository: TestDataRepository,
    private val testRepository: TestRepository,
    private val locationInfoLiveData: LocationInfoLiveData,
    private val locationWatcher: LocationWatcher,
    private val signalStrengthLiveData: SignalStrengthLiveData,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val activeNetworkLiveData: ActiveNetworkLiveData,
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val cellInfoWatcher: CellInfoWatcher,
    private val permissionsWatcher: PermissionsWatcher,
    private val config: Config,
    private val cellLocationLiveData: CellLocationLiveData,
    private val cellLocationWatcher: CellLocationWatcher
) : RMBTClientCallback {

    private var testUUID: String? = null
    private var testStartTimeNanos: Long = 0L
    private var testRecord: TestRecord? = null

    private var _locationInfo: LocationInfo? = null
    private var signalStrengthInfo: SignalStrengthInfo? = null
    private var networkInfo: NetworkInfo? = null
    private var cellLocation: CellLocationInfo? = null

    val locationInfo: LocationInfo?
        get() = _locationInfo

    /**
     * Associate current state collector object with service lifecycle
     */
    fun bind(lifecycle: LifecycleOwner) {

        _locationInfo = locationWatcher.getLatestLocationInfo()
        locationInfoLiveData.observe(lifecycle, Observer { info ->
            _locationInfo = info
            saveLocationInfo()
        })

        signalStrengthInfo = signalStrengthWatcher.lastSignalStrength
        signalStrengthLiveData.observe(lifecycle, Observer { info ->
            signalStrengthInfo = info
            saveSignalStrengthInfo()
        })

        networkInfo = activeNetworkWatcher.currentNetworkInfo
        activeNetworkLiveData.observe(lifecycle, Observer {
            networkInfo = it
            saveCellInfo()
        })

        cellLocation = cellLocationWatcher.latestLocation
        cellLocationLiveData.observe(lifecycle, Observer {
            cellLocation = it
            saveCellLocation()
        })
    }

    override fun onClientReady(testUUID: String, loopUUID: String?, testToken: String, testStartTimeNanos: Long, threadNumber: Int) {
        this.testUUID = testUUID
        this.testStartTimeNanos = testStartTimeNanos
        saveTestInitialTestData(testUUID, loopUUID, testToken, testStartTimeNanos, threadNumber)
        saveLocationInfo()
        saveSignalStrengthInfo()
        saveCellInfo()
        saveCapabilities()
        savePermissionsStatus()
        saveCellLocation()
    }

    fun finish() {
        // TODO finish
        testUUID = null
    }

    private fun saveTestInitialTestData(testUUID: String, loopUUID: String?, testToken: String, testStartTimeNanos: Long, threadNumber: Int) {
        Timber.d("testUUID $testUUID, loopUUId $loopUUID, testToken: $testToken, start: $testStartTimeNanos, threadNumber $threadNumber")
        testRecord = TestRecord(
            uuid = testUUID,
            loopUUID = loopUUID,
            token = testToken,
            testStartTimeMillis = TimeUnit.NANOSECONDS.toMillis(testStartTimeNanos),
            threadNumber = threadNumber
        )
        testRepository.saveTest(testRecord!!)
    }

    private fun saveLocationInfo() {
        val uuid = testUUID
        val location = locationInfo
        if (uuid != null && location != null) {
            repository.saveGeoLocation(uuid, location)
        }
    }

    private fun saveSignalStrengthInfo() {
        val uuid = testUUID
        val info = signalStrengthInfo
        if (uuid != null && info != null) {
            val cellUUID = networkInfo?.cellUUID ?: ""
            var mobileNetworkType: MobileNetworkType? = null
            if (networkInfo != null && networkInfo is CellNetworkInfo) {
                mobileNetworkType = (networkInfo as CellNetworkInfo).networkType
            }
            repository.saveSignalStrength(uuid, cellUUID, mobileNetworkType, info, testStartTimeNanos)
        }
    }

    private fun saveCellInfo() {
        val uuid = testUUID
        val info = networkInfo
        if (uuid != null && info != null) {
            val infoList: List<NetworkInfo> = when (info) {
                is WifiNetworkInfo -> listOf(info)
                is CellNetworkInfo -> cellInfoWatcher.allCellInfo
                else -> throw IllegalArgumentException("Unknown cell info ${info.javaClass.simpleName}")
            }

            repository.saveCellInfo(uuid, infoList)
        }
    }

    private fun saveCapabilities() {
        val uuid = testUUID
        uuid?.let {
            repository.saveCapabilities(
                it,
                config.capabilitiesRmbtHttp,
                config.capabilitiesQosSupportsInfo,
                config.capabilitiesClassificationCount
            )
        }
    }

    private fun savePermissionsStatus() {
        val permissions = permissionsWatcher.allPermissions
        val uuid = testUUID
        uuid?.let {
            permissions.forEach { permission ->
                val permissionGranted = context.hasPermission(permission)
                repository.savePermissionStatus(it, permission, permissionGranted)
            }
        }
    }

    private fun saveCellLocation() {
        val uuid = testUUID
        val location = cellLocation
        if (uuid != null && location != null) {
            repository.saveCellLocation(uuid, location)
        }
    }

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long) {
        testUUID?.let {
            repository.saveDownloadGraphItem(it, progress, speedBps)
        }
    }

    fun onUploadSpeedChanged(progress: Int, speedBps: Long) {
        testUUID?.let {
            repository.saveUploadGraphItem(it, progress, speedBps)
        }
    }

    override fun onSpeedDataChanged(threadId: Int, bytes: Long, timestampNanos: Long, isUpload: Boolean) {
        testUUID?.let {
            repository.saveSpeedData(it, threadId, bytes, timestampNanos, isUpload)
        }
    }

    override fun onPingDataChanged(clientPing: Long, serverPing: Long, timeNs: Long) {
        testUUID?.let {
            repository.saveAllPingValues(it, clientPing, serverPing, timeNs)
        }
    }

    override fun onResultUpdated(result: TotalTestResult, status: TestStatus?) {
        testRecord?.apply {
            portRemote = result.port_remote
            bytesDownload = result.bytes_download
            bytesUpload = result.bytes_upload
            totalBytesDownload = result.totalDownBytes
            totalBytesUpload = result.totalUpBytes
            encryption = result.encryption
            ipLocal = result.ip_local?.hostAddress
            ipServer = result.ip_server?.hostAddress
            downloadDurationNanos = result.nsec_download
            uploadDurationNanos = result.nsec_upload
            downloadSpeedBps = floor(result.speed_download + 0.5).toLong()
            uploadSpeedBps = floor(result.speed_upload + 0.5).toLong()
            shortestPingNanos = result.ping_shortest
            downloadedBytesOnInterface = result.getTotalTrafficMeasurement(TrafficDirection.RX)
            uploadedBytesOnInterface = result.getTotalTrafficMeasurement(TrafficDirection.TX)
            downloadedBytesOnDownloadInterface = result.getTrafficByTestPart(TestStatus.DOWN, TrafficDirection.RX)
            uploadedBytesOnDownloadInterface = result.getTrafficByTestPart(TestStatus.DOWN, TrafficDirection.TX)
            downloadedBytesOnUploadInterface = result.getTrafficByTestPart(TestStatus.UP, TrafficDirection.RX)
            uploadedBytesOnUploadInterfaceKb = result.getTrafficByTestPart(TestStatus.UP, TrafficDirection.TX)

            val dlMeasurement = result.getTestMeasurementByTestPart(TestStatus.DOWN)
            dlMeasurement?.let {
                timeDownloadOffsetNanos = it.timeStampStart - testStartTimeNanos
            }

            val ulMeasurement = result.getTestMeasurementByTestPart(TestStatus.UP)
            ulMeasurement?.let {
                timeUploadOffsetNanos = it.timeStampStart - testStartTimeNanos
            }

            this.status = status
            transportType = networkInfo?.type
        }

        testRecord?.let {
            testRepository.update(it)
        }
    }
}