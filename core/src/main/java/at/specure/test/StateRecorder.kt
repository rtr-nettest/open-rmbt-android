package at.specure.test

import android.annotation.SuppressLint
import android.location.Location
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import at.rmbt.client.control.data.TestFinishReason
import at.rtr.rmbt.client.RMBTClientCallback
import at.rtr.rmbt.client.TotalTestResult
import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.client.v2.task.result.QoSResultCollector
import at.rtr.rmbt.client.v2.task.service.TestMeasurement.TrafficDirection
import at.specure.config.Config
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.LoopModeState
import at.specure.data.entity.TestRecord
import at.specure.data.repository.MeasurementRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.Network5GSimulator
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
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.location.cell.CellLocationInfo
import at.specure.location.cell.CellLocationLiveData
import at.specure.location.cell.CellLocationWatcher
import org.json.JSONArray
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.floor

class StateRecorder @Inject constructor(
    private val repository: TestDataRepository,
    private val locationWatcher: LocationWatcher,
    private val signalStrengthLiveData: SignalStrengthLiveData,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val activeNetworkLiveData: ActiveNetworkLiveData,
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val cellInfoWatcher: CellInfoWatcher,
    private val config: Config,
    private val cellLocationLiveData: CellLocationLiveData,
    private val cellLocationWatcher: CellLocationWatcher,
    private val measurementRepository: MeasurementRepository
) : RMBTClientCallback {
    private var testUUID: String? = null
    private var testToken: String? = null
    private var testStartTimeNanos: Long = 0L
    private var testRecord: TestRecord? = null

    private var _locationInfo: LocationInfo? = null
    private var signalStrengthInfo: SignalStrengthInfo? = null
    private var networkInfo: NetworkInfo? = null
    private var cellLocation: CellLocationInfo? = null
    private var qosRunning = false

    var onReadyToSubmit: ((Boolean) -> Unit)? = null

    var onLoopDistanceReached: (() -> Unit)? = null

    val locationInfo: LocationInfo?
        get() = _locationInfo

    private var _loopModeRecord: LoopModeRecord? = null

    val loopModeRecord: LoopModeRecord?
        get() = _loopModeRecord

    val loopLocalUuid: String?
        get() = _loopModeRecord?.localUuid

    val loopTestCount: Int
        get() = _loopModeRecord?.testsPerformed ?: 1

    fun updateLocationInfo() {
        _locationInfo = if (locationWatcher.state == LocationState.ENABLED) {
            locationWatcher.latestLocation
        } else {
            null
        }
    }

    /**
     * Associate current state collector object with service lifecycle
     */
    fun bind(lifecycle: LifecycleOwner) {

        updateLocationInfo()

        locationWatcher.liveData.observe(lifecycle, Observer { info ->
            if (locationWatcher.state == LocationState.ENABLED) {
                _locationInfo = info
                saveLocationInfo()
            } else {
                _locationInfo = null
            }
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
            saveTelephonyInfo()
            saveWlanInfo()
        })

        cellLocation = cellLocationWatcher.latestLocation
        cellLocationLiveData.observe(lifecycle, Observer {
            cellLocation = it
            saveCellLocation()
        })
    }

    fun resetLoopMode() {
        _loopModeRecord = null
    }

    override fun onClientReady(testUUID: String, loopUUID: String?, testToken: String, testStartTimeNanos: Long, threadNumber: Int) {
        this.testUUID = testUUID
        this.testToken = testToken
        this.testStartTimeNanos = testStartTimeNanos
        qosRunning = false
        saveTestInitialTestData(testUUID, loopUUID, testToken, testStartTimeNanos, threadNumber)
        cellLocation = cellLocationWatcher.getCellLocationFromTelephony()
        saveCellLocation()
        saveLocationInfo()
        saveSignalStrengthInfo()
        saveCellInfo()
        saveCapabilities()
        savePermissionsStatus()
        saveTelephonyInfo()
        saveWlanInfo()
    }

    fun finish() {
        testUUID = null
        testToken = null
        qosRunning = false
    }

    private fun saveTestInitialTestData(testUUID: String, loopUUID: String?, testToken: String, testStartTimeNanos: Long, threadNumber: Int) {
        Timber.d("testUUID $testUUID, loopUUId $loopUUID, testToken: $testToken, start: $testStartTimeNanos, threadNumber $threadNumber")
        testRecord = TestRecord(
            uuid = testUUID,
            loopUUID = loopUUID,
            token = testToken,
            testStartTimeMillis = TimeUnit.NANOSECONDS.toMillis(testStartTimeNanos),
            threadCount = threadNumber,
            testTag = config.measurementTag,
            developerModeEnabled = config.developerModeIsEnabled,
            serverSelectionEnabled = config.expertModeEnabled,
            loopModeEnabled = config.loopModeEnabled
        )
        if (config.shouldRunQosTest) {
            testRecord?.lastQoSStatus = TestStatus.WAIT
        }

        if (config.loopModeEnabled) {
            initializeLoopModeData(loopUUID)
        }

        testRecord?.loopModeTestOrder = loopTestCount
        repository.saveTest(testRecord!!)
    }

    fun initializeLoopModeData(loopUUID: String?) {
        if (_loopModeRecord == null) {
            val localLoopUUID = UUID.randomUUID().toString()
            Timber.d("new generated local loop uuid $localLoopUUID")
            _loopModeRecord = LoopModeRecord(localLoopUUID, loopUUID)
            Timber.d("LOOP STATE SAVED 1: ${_loopModeRecord!!.status}")
            repository.saveLoopMode(_loopModeRecord!!)
        } else {
            if (_loopModeRecord?.uuid == null) {
                _loopModeRecord?.uuid = loopUUID
                Timber.d("new added remote loop uuid $loopUUID")
            }
            updateLoopModeRecord()
        }
    }

    private fun updateLoopModeRecord() {
        _loopModeRecord?.run {
            Timber.d("LOOP STATE UPDATED 1: ${this.status}")
            repository.updateLoopMode(this)
        }
    }

    fun onLoopTestFinished() {
        _loopModeRecord?.let {
            if (it.testsPerformed == config.loopModeNumberOfTests) {
                it.status = LoopModeState.FINISHED
            } else {
                it.status = LoopModeState.IDLE
            }
            Timber.d("LOOP STATE UPDATED FINISHED 2: ${it.status}")
            repository.updateLoopMode(it)
        }
    }

    fun onLoopTestScheduled() {
        _loopModeRecord?.let {
            val location = locationInfo
            it.lastTestFinishedTimeMillis = System.currentTimeMillis()
            it.lastTestLatitude = _locationInfo?.latitude
            it.lastTestLongitude = _locationInfo?.longitude
            it.movementDistanceMeters = 0
            Timber.d("LOOP STATE UPDATED SCHEDULED 3: ${it.status}")
            repository.updateLoopMode(it)
        }
    }

    private fun saveLocationInfo() {
        val uuid = testUUID
        val location = locationInfo
        if (uuid != null && location != null && locationWatcher.state == LocationState.ENABLED) {
            repository.saveGeoLocation(uuid, location, testStartTimeNanos)
        }

        _loopModeRecord?.let {
            val newLocation = Location("")
            newLocation.latitude = location?.latitude ?: return@let
            newLocation.longitude = location.longitude

            if (it.lastTestLatitude == null || it.lastTestLongitude == null) {
                it.lastTestLatitude = location.latitude
                it.lastTestLongitude = location.longitude
            } else {
                val loopLocation = Location("")
                loopLocation.latitude = it.lastTestLatitude ?: return@let
                loopLocation.longitude = it.lastTestLongitude ?: return@let

                it.movementDistanceMeters = loopLocation.distanceTo(newLocation).toInt()
                Timber.d("LOOP DISTANCE: ${it.movementDistanceMeters}")

                if (config.loopModeEnabled && loopModeRecord != null && loopModeRecord?.status != LoopModeState.FINISHED) {
                    var notifyDistanceReached = false
                    if (it.movementDistanceMeters >= config.loopModeDistanceMeters && newLocation.accuracy < config.loopModeDistanceMeters) {
                        Timber.d("LOOP STARTING DISTANCE: ${it.movementDistanceMeters}")
                        notifyDistanceReached = true
                    }

                    if (notifyDistanceReached) {
                        onLoopDistanceReached?.invoke()
                    }
                }
            }
            Timber.d("LOOP STATE UPDATED LOCATION SAVE 5: ${it.status}")
            repository.updateLoopMode(it)
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

            repository.saveCellInfo(uuid, infoList, testStartTimeNanos)
        }
    }

    private fun saveCapabilities() {
        testUUID?.let { measurementRepository.saveCapabilities(it) }
    }

    private fun savePermissionsStatus() {
        testUUID?.let { measurementRepository.savePermissionsStatus(it) }
    }

    private fun saveCellLocation() {
        val uuid = testUUID
        val location = cellLocation
        if (uuid != null && location != null) {
            repository.saveCellLocation(uuid, location, testStartTimeNanos)
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveTelephonyInfo() {
        val info = networkInfo
        if (info != null && info is CellNetworkInfo) {
            testRecord?.mobileNetworkType = info.networkType
        }

        testUUID?.let { measurementRepository.saveTelephonyInfo(it) }
    }

    private fun saveWlanInfo() {
        testUUID?.let {
            measurementRepository.saveWlanInfo(it)
        }
    }

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long) {
        testUUID?.let {
            if (progress > -1) {
                repository.saveDownloadGraphItem(it, progress, speedBps)
            }
        }
    }

    fun onUploadSpeedChanged(progress: Int, speedBps: Long) {
        testUUID?.let {
            if (progress > -1) {
                repository.saveUploadGraphItem(it, progress, speedBps)
            }
        }
    }

    override fun onSpeedDataChanged(threadId: Int, bytes: Long, timestampNanos: Long, isUpload: Boolean) {
        testUUID?.let {
            val value = if (isUpload) {
                Network5GSimulator.upBitPerSec(bytes)
            } else {
                Network5GSimulator.downBitPerSec(bytes)
            }
            repository.saveSpeedData(it, threadId, value, timestampNanos, isUpload)
        }
    }

    override fun onPingDataChanged(clientPing: Long, serverPing: Long, timeNs: Long) {
        testUUID?.let {
            repository.saveAllPingValues(it, clientPing, serverPing, timeNs)
        }
    }

    override fun onTestCompleted(result: TotalTestResult, waitQosResults: Boolean) {
        testRecord?.apply {
            portRemote = result.port_remote
            bytesDownloaded = result.bytes_download
            bytesUploaded = result.bytes_upload
            totalBytesDownloaded = result.totalDownBytes
            totalBytesUploaded = result.totalUpBytes
            encryption = result.encryption
            clientPublicIp = result.ip_local?.hostAddress
            serverPublicIp = result.ip_server?.hostAddress
            downloadDurationNanos = result.nsec_download
            uploadDurationNanos = result.nsec_upload
            downloadSpeedKps = Network5GSimulator.downBitPerSec(floor(result.speed_download + 0.5).toLong())
            uploadSpeedKps = Network5GSimulator.upBitPerSec(floor(result.speed_upload + 0.5).toLong())
            shortestPingNanos = result.ping_shortest
            downloadedBytesOnInterface = result.getTotalTrafficMeasurement(TrafficDirection.RX)
            uploadedBytesOnInterface = result.getTotalTrafficMeasurement(TrafficDirection.TX)
            downloadedBytesOnDownloadInterface = result.getTrafficByTestPart(TestStatus.DOWN, TrafficDirection.RX)
            uploadedBytesOnDownloadInterface = result.getTrafficByTestPart(TestStatus.DOWN, TrafficDirection.TX)
            downloadedBytesOnUploadInterface = result.getTrafficByTestPart(TestStatus.UP, TrafficDirection.RX)
            uploadedBytesOnUploadInterface = result.getTrafficByTestPart(TestStatus.UP, TrafficDirection.TX)

            val dlMeasurement = result.getTestMeasurementByTestPart(TestStatus.DOWN)
            dlMeasurement?.let {
                timeDownloadOffsetNanos = it.timeStampStart - testStartTimeNanos
            }

            val ulMeasurement = result.getTestMeasurementByTestPart(TestStatus.UP)
            ulMeasurement?.let {
                timeUploadOffsetNanos = it.timeStampStart - testStartTimeNanos
            }

            transportType = networkInfo?.type
            testTimeMillis = System.currentTimeMillis()

            testFinishReason = TestFinishReason.SUCCESS
        }

        testRecord?.let {
            repository.update(it) {
                if (!waitQosResults) {
                    onReadyToSubmit?.invoke(true)
                }
            }
        }

        if (!waitQosResults) {
            testUUID = null
            testToken = null
        } else {
            qosRunning = true
        }
    }

    override fun onQoSTestCompleted(qosResult: QoSResultCollector?) {
        val uuid = testUUID
        val token = testToken
        val data: JSONArray? = qosResult?.toJson()
        if (uuid != null && token != null && qosResult != null && data != null) {
            testRecord?.lastQoSStatus = TestStatus.QOS_END
            repository.updateQoSTestStatus(uuid, TestStatus.QOS_END)
            Timber.d("QOSLOG: ${TestStatus.QOS_END}")
            repository.saveQoSResults(uuid, token, data) {
                onReadyToSubmit?.invoke(true)
            }
        }
        testUUID = null
        qosRunning = false
    }

    override fun onTestStatusUpdate(status: TestStatus?) {
        status?.let {
            if (qosRunning) {
                testUUID?.let {
                    testRecord?.lastQoSStatus = status
                    repository.updateQoSTestStatus(it, status)
                    Timber.d("QOSLOG: $status")
                }
            } else {
                testRecord?.also {
                    it.status = status

                    if (status != TestStatus.ERROR && status != TestStatus.ABORTED) {
                        it.lastClientStatus = status
                    }
                }
            }
        }
    }

    fun onUnsuccessTest(reason: TestFinishReason) {
        testRecord?.also {
            it.testFinishReason = reason

            repository.update(it) {
                onReadyToSubmit?.invoke(false)
            }
        }
    }

    fun setErrorCause(message: String) {
        testRecord?.testErrorCause = message
    }

    fun onTestInLoopStarted() {
        _loopModeRecord?.let {
            it.movementDistanceMeters = 0
            it.lastTestLongitude = locationInfo?.longitude
            it.lastTestLatitude = locationInfo?.latitude
            it.status = LoopModeState.RUNNING
            it.testsPerformed++
            Timber.d("LOOP STATE UPDATED STARTED 4: ${it.status}")
            repository.updateLoopMode(it)
        }
    }
}