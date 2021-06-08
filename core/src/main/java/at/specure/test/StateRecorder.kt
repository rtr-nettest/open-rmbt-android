package at.specure.test

import android.content.Context
import android.location.Location
import android.telephony.SubscriptionManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import at.rmbt.client.control.data.TestFinishReason
import at.rmbt.client.control.getCurrentDataSubscriptionId
import at.rmbt.util.io
import at.rtr.rmbt.client.RMBTClientCallback
import at.rtr.rmbt.client.TotalTestResult
import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.client.v2.task.result.QoSResultCollector
import at.rtr.rmbt.client.v2.task.service.TestMeasurement.TrafficDirection
import at.specure.config.Config
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.LoopModeState
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.TestRecord
import at.specure.data.entity.getJitter
import at.specure.data.entity.getPacketLoss
import at.specure.data.entity.toRecord
import at.specure.data.repository.MeasurementRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.Network5GSimulator
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.location.cell.CellLocationInfo
import at.specure.location.cell.CellLocationWatcher
import at.specure.util.filterOnlyActiveDataCell
import at.specure.util.isCoarseLocationPermitted
import at.specure.util.isReadPhoneStatePermitted
import at.specure.util.mobileNetworkType
import at.specure.util.toCellInfoRecord
import at.specure.util.toCellLocation
import at.specure.util.toSignalRecord
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.feature.merge.CellSource
import cz.mroczis.netmonster.core.model.cell.ICell
import org.json.JSONArray
import timber.log.Timber
import java.util.Collections
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.floor

class StateRecorder @Inject constructor(
    private val context: Context,
    private val netmonster: INetMonster,
    private val repository: TestDataRepository,
    private val locationWatcher: LocationWatcher,
    private val signalStrengthLiveData: SignalStrengthLiveData,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val config: Config,
    private val subscriptionManager: SubscriptionManager,
    private val cellLocationWatcher: CellLocationWatcher,
    private val measurementRepository: MeasurementRepository
) : RMBTClientCallback {
    private var testUUID: String? = null
    private var testToken: String? = null
    private var testStartTimeNanos: Long = 0L
    private var testRecord: TestRecord? = null

    private var _locationInfo: LocationInfo? = null
    private var signalStrengthInfo: SignalStrengthInfo? = null
    var lastMeasurementSignalStrength: SignalStrengthInfo? = null
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
        if ((loopModeRecord != null) && (loopModeRecord?.status == LoopModeState.RUNNING)) {
            lastMeasurementSignalStrength = signalStrengthInfo
        }
        signalStrengthLiveData.observe(lifecycle, Observer { info ->
            Timber.d("Signal change detection: $info")
            if ((loopModeRecord != null) && (loopModeRecord?.status == LoopModeState.RUNNING)) {
                lastMeasurementSignalStrength = signalStrengthInfo
            }
            signalStrengthInfo = info?.signalStrengthInfo
            Timber.e("Signal saving time OBSERVER: starting time: $testStartTimeNanos   current time: ${System.nanoTime()}")
            if (networkInfo?.type != TransportType.CELLULAR) {
                saveSignalStrength(testUUID, signalStrengthInfo)
            }
            networkInfo = info?.networkInfo
            saveCellInfo()
            saveTelephonyInfo()
            saveWlanInfo()
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
        Timber.e("Signal saving time OCR: starting time: $testStartTimeNanos   current time: ${System.nanoTime()}")
        saveTestInitialTestData(testUUID, loopUUID, testToken, testStartTimeNanos, threadNumber)
        cellLocation = cellLocationWatcher.getCellLocationFromTelephony()
        saveCellLocation()
        saveLocationInfo()
        if (networkInfo?.type != TransportType.CELLULAR) {
            saveSignalStrength(testUUID, signalStrengthInfo)
        }
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
            loopModeEnabled = config.loopModeEnabled,
            clientVersion = "",
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
            _loopModeRecord = LoopModeRecord(localLoopUUID, loopUUID, lastTestUuid = testRecord?.uuid)
            Timber.d("LOOP STATE SAVED 1: ${_loopModeRecord!!.status}")
            repository.saveLoopMode(_loopModeRecord!!)
        } else {
            if (_loopModeRecord?.uuid == null) {
                _loopModeRecord?.uuid = loopUUID
                _loopModeRecord?.lastTestUuid = testRecord?.uuid
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
            if (it.testsPerformed >= config.loopModeNumberOfTests) {
                it.status = LoopModeState.FINISHED
            } else {
                it.status = LoopModeState.IDLE
            }
            it.lastTestUuid = testRecord?.uuid
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
            repository.saveGeoLocation(uuid, location, testStartTimeNanos, false)
        }

        _loopModeRecord?.let {

            Timber.d("Location obtained: provider:${location?.provider} accuracy:${location?.accuracy}")
            // allow to use only high precision location data to start next test during the loop mode (default: ignore network provider, accept only accuracy better or equal to 20m)
            if (location?.provider == "network" || location?.accuracy?.compareTo(20) ?: 1 > 0) return@let
            Timber.d("Location accepted: provider:${location?.provider} accuracy:${location?.accuracy}")
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

                if (config.loopModeEnabled && loopModeRecord != null && loopModeRecord?.status != LoopModeState.FINISHED && loopModeRecord?.status != LoopModeState.CANCELLED) {
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

    @Deprecated("Use only saveSignalStrength")
    private fun saveSignalStrengthInfo() {
        val uuid = testUUID
        val info = signalStrengthInfo
        saveSignalStrength(uuid, info)
    }

    private fun saveSignalStrength(uuid: String?, info: SignalStrengthInfo?) {
        if (uuid != null && info != null) {
            val cellUUID = networkInfo?.cellUUID ?: ""
            var mobileNetworkType: MobileNetworkType? = null
            var nrConnectionState = NRConnectionState.NOT_AVAILABLE
            // adjusting mobile network type because of NSA mode where we are reporting NR cell
            if (networkInfo != null && networkInfo is CellNetworkInfo) {
                mobileNetworkType = (networkInfo as CellNetworkInfo).networkType
                nrConnectionState = (networkInfo as CellNetworkInfo).nrConnectionState
            }
            Timber.d("valid signal SSSI")
            val isSignalValid = repository.validateSignalStrengthInfo(mobileNetworkType, info, cellUUID)

            // saving only valid signal with associated cell (wifi and mobile connections)
            if (cellUUID.isNotEmpty() && isSignalValid) {
                Timber.e("Signal saving time SR: starting time: $testStartTimeNanos   current time: ${System.nanoTime()}")
                repository.saveSignalStrength(uuid, cellUUID, mobileNetworkType, info, testStartTimeNanos, nrConnectionState)
            }
        }
    }

    @Synchronized
    private fun saveCellInfo() = io {
        val uuid = testUUID
        val info = networkInfo
        if (networkInfo?.type == TransportType.CELLULAR) {
            if (context.isCoarseLocationPermitted() && context.isReadPhoneStatePermitted()) {
                try {
                    var cells: List<ICell>? = null

                    cells = netmonster.getCells(CellSource.NEIGHBOURING_CELLS, CellSource.ALL_CELL_INFO, CellSource.CELL_LOCATION)

                    val dataSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()

                    val primaryCells = cells?.filterOnlyActiveDataCell(dataSubscriptionId)

                    val cellInfosToSave = mutableListOf<CellInfoRecord>()
                    val signalsToSave = mutableListOf<SignalRecord>()
                    val cellLocationsToSave = mutableListOf<CellLocationRecord>()

                    if (uuid != null) {
                        val testStartTimeNanos = testStartTimeNanos ?: 0
                        primaryCells?.toList()?.let {
                            if (it.size == 1) {
                                val iCell = it[0]
                                val cellInfoRecord = iCell.toCellInfoRecord(uuid, netmonster)
                                cellInfoRecord?.let {
                                    if (cellInfoRecord.uuid.isNotEmpty()) {
                                        iCell.signal?.let { iSignal ->
                                            Timber.e("Signal saving time SCI: starting time: $testStartTimeNanos   current time: ${System.nanoTime()}")
                                            Timber.d("valid signal directly")
                                            val signalRecord = iSignal.toSignalRecord(
                                                uuid,
                                                cellInfoRecord.uuid,
                                                iCell.mobileNetworkType(netmonster),
                                                testStartTimeNanos,
                                                NRConnectionState.NOT_AVAILABLE
                                            )
                                            signalsToSave.add(signalRecord)
                                        }
                                    }
                                    val cellLocationRecord =
                                        iCell.toCellLocation(uuid, System.currentTimeMillis(), System.nanoTime(), testStartTimeNanos)
                                    cellLocationRecord?.let {
                                        cellLocationsToSave.add(cellLocationRecord)
                                    }
                                    cellInfosToSave.add(cellInfoRecord)
                                }
                            } else {
                                // ignoring more than one primary cell for one subscription ID - not consistent state
                            }

                            repository.saveCellLocationRecord(cellLocationsToSave)
                            repository.saveCellInfoRecord(cellInfosToSave)
                            repository.saveSignalRecord(signalsToSave)
                        }
                    }
                } catch (e: SecurityException) {
                    Timber.e("SecurityException: Not able to read telephonyManager.allCellInfo")
                } catch (e: IllegalStateException) {
                    Timber.e("IllegalStateException: Not able to read telephonyManager.allCellInfo")
                } catch (e: NullPointerException) {
                    Timber.e("NullPointerException: Not able to read telephonyManager.allCellInfo from other reason")
                }
            }
        } else if (networkInfo?.type == TransportType.WIFI) {
            if (uuid != null && info != null) {
                val infoList: List<NetworkInfo> = when (info) {
                    is WifiNetworkInfo -> listOf(info)
                    is CellNetworkInfo -> listOf<NetworkInfo>()
                    else -> throw IllegalArgumentException("Unknown cell info ${info.javaClass.simpleName}")
                }

                val copyInfoList = Collections.synchronizedList(infoList.toMutableList())

                val onlyActiveCellInfoList = Collections.synchronizedList(copyInfoList.filter {
                    if (it is CellNetworkInfo) {
                        it.isActive
                    } else {
                        true
                    }
                })

                repository.saveCellInfo(uuid, onlyActiveCellInfoList.toList(), testStartTimeNanos)
                onlyActiveCellInfoList.toList().forEach {
                    if (it is CellNetworkInfo) {
                        saveSignalStrength(uuid, it.signalStrength)
                    }
                }
            }
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
            threadCount = result.num_threads
            portRemote = result.port_remote
            clientVersion = result.client_version
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

            this.uuid.let {
                if (config.performJitterAndPacketLossTest) {
                    val voipTestResultRecord = result.voipTestResult.toRecord(it)
                    jitterNanos = (voipTestResultRecord.getJitter()?.times(1000000))?.toLong()
                    packetLossPercents = voipTestResultRecord.getPacketLoss()
                    repository.saveVoipResult(voipTestResultRecord)
                }
            }

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
        lastMeasurementSignalStrength = signalStrengthInfo
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

    fun onLoopTestStatusChanged(loopModeState: LoopModeState) {
        _loopModeRecord?.let {
            it.status = loopModeState
            repository.updateLoopMode(it)
        }
    }
}