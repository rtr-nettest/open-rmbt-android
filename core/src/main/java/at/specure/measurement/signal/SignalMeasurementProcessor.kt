package at.specure.measurement.signal

import android.content.Context
import android.os.Binder
import android.os.Handler
import android.telephony.SubscriptionManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.entity.*
import at.specure.data.repository.MeasurementRepository
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.PrimaryDataSubscription
import at.specure.info.connectivity.ConnectivityStateBundle
import at.specure.info.connectivity.ConnectivityWatcher
import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.location.cell.CellLocationInfo
import at.rmbt.client.control.data.SignalMeasurementType
import at.specure.measurement.coverage.RtrCoverageMeasurementProcessor
import at.specure.test.toDeviceInfoLocation
import at.specure.util.isFineLocationPermitted
import at.specure.util.isLocationServiceEnabled
import at.specure.util.isReadPhoneStatePermitted
import at.specure.util.toCellLocation
import at.specure.util.toRecords
import cz.mroczis.netmonster.core.model.cell.ICell
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.Runnable
import java.lang.SecurityException
import java.lang.System
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

private const val MAX_SIGNAL_COUNT_PER_CHUNK = 25
private const val MAX_SIGNAL_UPTIME_PER_CHUNK_MIN = 10L
private const val MAX_TIME_NETWORK_UNREACHABLE_SECONDS = 300L

@Singleton
class SignalMeasurementProcessor @Inject constructor(
    private val context: Context,
    private val config: Config,
    private val repository: TestDataRepository,
    @Named("GPSAndFusedLocationProvider") private val locationWatcher: LocationWatcher,
    private val signalStrengthLiveData: SignalStrengthLiveData,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val subscriptionManager: SubscriptionManager,
    private val signalRepository: SignalMeasurementRepository,
    private val connectivityWatcher: ConnectivityWatcher,
    private val measurementRepository: MeasurementRepository,
    private val rtrCoverageMeasurementProcessor: RtrCoverageMeasurementProcessor,
    private val cellInfoWatcher: CellInfoWatcher
) : Binder(), SignalMeasurementProducer, CoroutineScope, SignalMeasurementChunkResultCallback,
    SignalMeasurementChunkReadyCallback {

    private var globalNetworkInfo: NetworkInfo? = null
    private var lastSignalRecord: SignalRecord? = null
    private var isUnstoppable = false
    private var _isActive = false
    private var _isPaused = false
    private val _activeStateLiveData = MutableLiveData<Boolean>()
    private val _pausedStateLiveData = MutableLiveData<Boolean>()
    private val _signalMeasurementSessionIdLiveData = MutableLiveData<String?>()
    private val _signalMeasurementSessionErrorLiveData = MutableLiveData<Exception?>()

    private var networkInfo: NetworkInfo? = null
    private var record: SignalMeasurementRecord? = null
    private var chunk: SignalMeasurementChunk? = null

    private var lastSeenNetworkInfo: NetworkInfo? = null
    private var lastSeenNetworkRecord: SignalMeasurementRecord? = null
    private var lastSeenNetworkTimestampMillis: Long? = null
    private var unconnectedTimer = Timer()

    private var lastSignalMeasurementType: SignalMeasurementType = SignalMeasurementType.UNKNOWN
    private var chunkDataSize = 0
    private var chunkCountDownRunner = Runnable {
        Timber.i("Chunk countdown timer reached")
        commitChunkData(ValidChunkPostProcessing.CREATE_NEW_CHUNK)
    }
    private var chunkCountDownHandler = Handler()

    private var locationInfo: LocationInfo? = null
    private var signalStrengthInfo: SignalStrengthInfo? = null
    private var cellLocation: CellLocationInfo? = null
    private val saveWlanInfo = false

    private val coroutineExceptionHandler = CoroutineExceptionHandler { context, e ->
        if (e is HandledException) {
            // do nothing
        } else {
            Timber.e("My SignalMeasurementProcessor coroutine named: ${context[CoroutineName]} has crashed with: ${e.message}")
            throw e
        }
    }

    override val coroutineContext = EmptyCoroutineContext + coroutineExceptionHandler

    override val isActive: Boolean
        get() = _isActive

    override val isPaused: Boolean
        get() = _isPaused

    override val activeStateLiveData: LiveData<Boolean>
        get() = _activeStateLiveData

    override val pausedStateLiveData: LiveData<Boolean>
        get() = _pausedStateLiveData

    override val signalMeasurementSessionIdLiveData: LiveData<String?>
        get() = _signalMeasurementSessionIdLiveData

    override val signalMeasurementSessionErrorLiveData: LiveData<Exception?>
        get() = _signalMeasurementSessionErrorLiveData

    override fun setEndAlarm() {
        // not necessary to implement here
    }

    val measurementSessionInitializedCallback: (sessionId: CoverageMeasurementSession) -> Unit = { coverageMeasurementSession ->
        _signalMeasurementSessionIdLiveData.postValue(coverageMeasurementSession.sessionId)
    }

    val measurementSessionInitializationErrorCallback: (exception: Exception) -> Unit = { exception ->
        _signalMeasurementSessionErrorLiveData.postValue(exception)
    }

    val measurementSessionStoppedCallback: () -> Unit = {
        stopMeasurementFromCoverage()
        SignalMeasurementService.stopIntent(context)
    }

    override fun startMeasurement(
        unstoppable: Boolean,
        signalMeasurementType: SignalMeasurementType
    ) {
        Timber.w("startMeasurement")
        _isActive = true
        isUnstoppable = unstoppable
        postStateData()
        lastSignalMeasurementType = signalMeasurementType

        if (lastSignalMeasurementType == SignalMeasurementType.DEDICATED) {
            Timber.d("Starting coverage session")
            rtrCoverageMeasurementProcessor.startCoverageSession(
                sessionCreated = measurementSessionInitializedCallback,
                sessionCreationError = measurementSessionInitializationErrorCallback,
                sessionStopped = measurementSessionStoppedCallback,
            )
            rtrCoverageMeasurementProcessor.onNewLocation(locationInfo, networkInfo)
        }

        if (isSignalMeasurementRunning()) {
            handleNewNetwork(signalStrengthWatcher.lastNetworkInfo)
        }
    }

    override fun stopMeasurement(unstoppable: Boolean) {
        Timber.w("stopMeasurement")
        stopMeasurementFromCoverage()
        isUnstoppable = unstoppable
        if (lastSignalMeasurementType == SignalMeasurementType.DEDICATED) {
            rtrCoverageMeasurementProcessor.stopCoverageSession()
        }
    }

    fun stopMeasurementFromCoverage() {
        Timber.w("stopMeasurement")
        chunk?.state = SignalMeasurementState.SUCCESS
        commitChunkData(ValidChunkPostProcessing.NOTHING)
        resetStateData()
        postStateData()
    }


    private fun resetStateData() {
        _isActive = false
        _isPaused = false
        networkInfo = null
        record = null
        chunk = null
    }

    private fun postStateData() {
        _activeStateLiveData.postValue(_isActive)
        _pausedStateLiveData.postValue(_isPaused)
    }

    override fun pauseMeasurement(unstoppable: Boolean) {
        Timber.w("pauseMeasurement")
        isUnstoppable = unstoppable
        setMeasurementAsPaused()
    }

    override fun resumeMeasurement(unstoppable: Boolean) {
        Timber.w("resumeMeasurement")
        isUnstoppable = unstoppable
        setMeasurementAsResumed()
        if (isSignalMeasurementRunning()) {
            handleNewNetwork(signalStrengthWatcher.lastNetworkInfo)
        }
    }

    private fun setMeasurementAsPaused() {
        _isPaused = true
        _pausedStateLiveData.postValue(_isPaused)
    }

    private fun setMeasurementAsResumed() {
        _isPaused = false
        _pausedStateLiveData.postValue(_isPaused)
    }

    fun bind(owner: LifecycleOwner) {

        if (locationWatcher.state == LocationState.ENABLED) {
            locationInfo = locationWatcher.latestLocation
        }
        locationWatcher.liveData.observe(owner, Observer { info ->
            if (locationWatcher.state == LocationState.ENABLED) {
                locationInfo = info
                if (lastSignalMeasurementType == SignalMeasurementType.DEDICATED) {
                    locationInfo?.let { location ->
                        Timber.d("passing new info with network: ${globalNetworkInfo?.type}")
                        rtrCoverageMeasurementProcessor.onNewLocation(location, globalNetworkInfo)
                    }
                }
                if (isSignalMeasurementRunning()) {
                    saveLocationInfo()
                }
            }
        })

        signalStrengthInfo = signalStrengthWatcher.lastSignalStrength
        signalStrengthLiveData.observe(owner, Observer { info ->
            signalStrengthInfo = info?.signalStrengthInfo
            if (isSignalMeasurementRunning()) {
                handleNewNetwork(info?.networkInfo)
                saveCellInfo(info)
            }
        })

        connectivityWatcher.connectivityStateLiveData.observe(owner, Observer { state ->
            state?.let {
                if (isActive) {
                    saveConnectivityState(state)
                }
            }
        })
    }

    private fun isSignalMeasurementRunning() = isActive && !isPaused

    private fun planUnconnectedClean() {
        synchronized(this) {
            unconnectedTimer.cancel()
            unconnectedTimer.purge()
            unconnectedTimer = Timer()
            Timber.d("Signal measurement unconnected gap timeout started")
            unconnectedTimer.schedule(
                object : TimerTask() {
                    override fun run() {
                        Timber.d("Signal measurement unconnected gap timeout reached")
                        cleanLastNetwork()
                    }
                },
                TimeUnit.SECONDS.toMillis(MAX_TIME_NETWORK_UNREACHABLE_SECONDS)
            )
        }
    }

    private fun cancelPlannedUnconnectedCleaning() {
        synchronized(this) {
            unconnectedTimer.cancel()
            unconnectedTimer.purge()
            Timber.d("Signal measurement unconnected gap timeout removed")
        }
    }

    private fun cleanLastNetwork() {
        lastSeenNetworkInfo = null
        lastSeenNetworkRecord = null
        lastSeenNetworkTimestampMillis = null
        lastSignalRecord = null
    }

    private fun handleNewNetwork(newInfo: NetworkInfo?) {
        val currentInfo = networkInfo
        globalNetworkInfo = newInfo
        var newNetworkInfo = newInfo
        if (newInfo?.type != TransportType.CELLULAR) {
            newNetworkInfo = null
        }
        when {
            newNetworkInfo == null && currentInfo != null -> {
                Timber.i("Network become unavailable")
                commitChunkData(ValidChunkPostProcessing.NOTHING)
                lastSeenNetworkInfo = networkInfo
                lastSeenNetworkRecord = record
                lastSeenNetworkTimestampMillis = System.currentTimeMillis()
                planUnconnectedClean()
                networkInfo = null
                record = null
            }

            newNetworkInfo != null && currentInfo == null -> {
                Timber.i("Network appeared")
                networkInfo = newNetworkInfo
                if ((lastSeenNetworkInfo != null) && (lastSeenNetworkInfo?.type == newNetworkInfo.type) && ((lastSeenNetworkTimestampMillis?.plus(
                        TimeUnit.SECONDS.toMillis(
                            MAX_TIME_NETWORK_UNREACHABLE_SECONDS
                        )
                    ) ?: -1) >= System.currentTimeMillis())
                ) {
                    networkInfo = lastSeenNetworkInfo
                    record = lastSeenNetworkRecord
                    cleanLastNetwork()
                    cancelPlannedUnconnectedCleaning()
                    Timber.i("Network appeared ${record?.mobileNetworkType?.name}")
                } else {
                    cleanLastNetwork()
                    cancelPlannedUnconnectedCleaning()
                    createNewRecord(newNetworkInfo)
                }
            }
            // it must be started like new chunk on different type of the network because network type is common for entire chunk
            newNetworkInfo != null && currentInfo != null && currentInfo.type != newNetworkInfo.type -> {
//                Timber.i("Network changed")
                networkInfo = newNetworkInfo
                commitChunkData(ValidChunkPostProcessing.NOTHING)
                createNewRecord(newNetworkInfo)
            }

            else -> {
//                Timber.i("New network other case -> new: ${newNetworkInfo?.cellUUID} old ${currentInfo?.cellUUID}")
            }
        }
    }

    private fun createNewRecord(networkInfo: NetworkInfo) {
        record = SignalMeasurementRecord(
            signalMeasurementType = lastSignalMeasurementType,
            networkUUID = networkInfo.cellUUID,
            transportType = networkInfo.type,
            location = locationInfo.toDeviceInfoLocation(),
            rawCapabilitiesRecord = networkInfo.capabilitiesRaw
        ).also {
            // todo: create new local dedicated measurement session
            signalRepository.saveAndRegisterRecord(it)
        }
        chunk = null
        createNewChunk()
    }

    private fun createNewRecordBecauseOfChangedUUID(
        networkInfo: NetworkInfo,
        newUUID: String,
        session: CoverageMeasurementSession
    ) {
        record = SignalMeasurementRecord(
            signalMeasurementType = lastSignalMeasurementType,
            networkUUID = networkInfo.cellUUID,
            transportType = networkInfo.type,
            location = locationInfo.toDeviceInfoLocation(),
            rawCapabilitiesRecord = networkInfo.capabilitiesRaw
        ).also {
            signalRepository.saveAndUpdateRegisteredRecord(it, newUUID, session)
        }
        chunk = null
        createNewChunk()
    }

    private fun commitChunkData(postProcessing: ValidChunkPostProcessing) {
        chunk?.let {
            Timber.i("Checking chunk data chunkID = ${it.id} sequence: ${it.sequenceNumber}")
            signalRepository.shouldSendMeasurementChunk(it, postProcessing, this)
        }
    }

    @ExperimentalCoroutinesApi
    private fun updateChunkInfo(chunkId: String) = launch(CoroutineName("updateChunkInfo")) {
        signalRepository.getSignalMeasurementChunk(chunkId)
            .flowOn(Dispatchers.IO)
            .collect { smr ->
                smr?.let {
                    Timber.i("Update chunk data chunkID started = ${chunk?.id} sequence: ${chunk?.sequenceNumber}")
                    chunk = smr
                    Timber.i("Update chunk data chunkID = ${chunk?.id} sequence: ${chunk?.sequenceNumber}")
                }
            }
    }

    @ExperimentalCoroutinesApi
    private fun createNewChunk() {
        record?.let {
            chunk = SignalMeasurementChunk(
                measurementId = it.id,
                sequenceNumber = chunk?.sequenceNumber?.inc() ?: 0,
                state = SignalMeasurementState.RUNNING,
                startTimeNanos = System.nanoTime(),
                submissionRetryCount = 0
            ).also { chunk ->
                signalRepository.saveMeasurementChunk(chunk)
                chunkDataSize = 0
                scheduleCountDownTimer()
                Timber.i("New chunk created chunkID = ${chunk.id} sequence: ${chunk.sequenceNumber} size: $chunkDataSize")
            }

            if (saveWlanInfo) {
                saveWlanInfo()
            }
            if (chunk?.sequenceNumber == 0) {
                saveCellInfo(
                    DetailedNetworkInfo(
                        cellInfoWatcher.activeNetwork,
                        cellInfoWatcher.signalStrengthInfo,
                        cellInfoWatcher.networkTypes,
                        cellInfoWatcher.allCellInfos.toList(),
                        cellInfoWatcher.secondaryActiveCellNetworks.toList(),
                        cellInfoWatcher.secondaryActiveCellSignalStrengthInfos.toList(),
                        cellInfoWatcher.secondary5GActiveCellNetworks.toList(),
                        cellInfoWatcher.secondary5GActiveCellSignalStrengthInfos.toList(),
                        cellInfoWatcher.dataSubscriptionId
                    )
                )
                Timber.i("Saving signal New chunk created chunkID = ${chunk?.id} sequence: ${chunk?.sequenceNumber}")
                saveLocationInfo()
            }
            saveTelephonyInfo()
            saveCapabilities()
            savePermissionsStatus()
            updateChunkInfo(it.id)
        }
    }

    private fun scheduleCountDownTimer() {
        chunkCountDownHandler.removeCallbacks(chunkCountDownRunner)
        chunkCountDownHandler.postDelayed(
            chunkCountDownRunner,
            TimeUnit.MINUTES.toMillis(MAX_SIGNAL_UPTIME_PER_CHUNK_MIN)
        )
    }

    private fun saveConnectivityState(bundle: ConnectivityStateBundle) {
        chunk?.let {
            repository.saveConnectivityState(
                ConnectivityStateRecord(
                    uuid = it.id,
                    state = bundle.state,
                    message = bundle.message,
                    timeNanos = bundle.timeNanos
                )
            )
        }
    }

    private fun saveCellInfo(detailedNetworkInfo: DetailedNetworkInfo?) = io {
        val uuid = chunk?.id
        if (context.isLocationServiceEnabled() && context.isFineLocationPermitted() && context.isReadPhoneStatePermitted()) {
            try {
                if (uuid != null && detailedNetworkInfo != null) {
                    val testStartTimeNanos = record?.startTimeNanos ?: 0
                    saveCellAndSignalInfo(
                        uuid,
                        detailedNetworkInfo,
                        testStartTimeNanos
                    )
                }
            } catch (e: SecurityException) {
                Timber.e("SecurityException: Not able to read telephonyManager.allCellInfo")
            } catch (e: IllegalStateException) {
                Timber.e("IllegalStateException: Not able to read telephonyManager.allCellInfo")
            } catch (e: NullPointerException) {
                Timber.e("NullPointerException: Not able to read telephonyManager.allCellInfo from other reason")
            }
        }
    }

    @Synchronized
    private fun saveCellAndSignalInfo(
        uuid: String?,
        detailedNetworkInfo: DetailedNetworkInfo?,
        testStartTimeNanos: Long
    ) {
        synchronized(this) {
            var signalsSavedCount = 0
            detailedNetworkInfo?.let {

                Timber.v("Process chunk data: ${detailedNetworkInfo.allCellInfos?.size} and add to: $chunkDataSize")

                val cellNetworkInfo = detailedNetworkInfo.networkInfo
                val active5GNetworkInfos = detailedNetworkInfo.secondary5GActiveCellNetworks
                val otherCells = if (detailedNetworkInfo.allCellInfos.isNullOrEmpty()) {
                    mutableListOf<ICell>()
                } else {
                    detailedNetworkInfo.allCellInfos.toMutableList()
                }
                val testStartTimeNanos = testStartTimeNanos ?: 0

                if (detailedNetworkInfo.networkInfo != null && detailedNetworkInfo.networkInfo is CellNetworkInfo) {
                    otherCells.remove(detailedNetworkInfo.networkInfo.rawCellInfo)
                }

                signalsSavedCount += saveNetworkInformation(
                    cellNetworkInfo,
                    detailedNetworkInfo.signalStrengthInfo,
                    uuid,
                    it.dataSubscriptionId,
                    testStartTimeNanos
                )
                Timber.v("Process chunk primary cell data end with: $signalsSavedCount")
                active5GNetworkInfos?.forEachIndexed { index, cellNetworkInfo ->
                    otherCells.remove(cellNetworkInfo?.rawCellInfo)
                    signalsSavedCount += saveNetworkInformation(
                        cellNetworkInfo,
                        detailedNetworkInfo.secondary5GActiveSignalStrengthInfos?.get(index),
                        uuid,
                        it.dataSubscriptionId,
                        testStartTimeNanos
                    )
                    Timber.v("Process chunk 5G cell data end with: $signalsSavedCount")
                }

                if (config.headerValue.isNullOrEmpty()) {
                    signalsSavedCount += saveOtherCellInfo(
                        otherCells.toMutableList(),
                        uuid,
                        testStartTimeNanos,
                        detailedNetworkInfo.networkTypes,
                        it.dataSubscriptionId
                    )
                    Timber.v("Process chunk other cell data end with: $signalsSavedCount")
                }
                Timber.v("Process chunk data end with: $signalsSavedCount")
            }
            Timber.v("Process chunk data ended, will add: $signalsSavedCount to $chunkDataSize")
            chunkDataSize += signalsSavedCount
            if (chunkDataSize >= MAX_SIGNAL_COUNT_PER_CHUNK) {
                Timber.v("Chunk max size reached: $chunkDataSize")
                commitChunkData(ValidChunkPostProcessing.CREATE_NEW_CHUNK)
            }
        }
    }

    private fun saveOtherCellInfo(
        cells: List<ICell>?,
        signalChunkId: String?,
        testStartTimeNanos: Long,
        mobileNetworkTypes: HashMap<Int, MobileNetworkType>,
        dataSubscriptionId: Int
    ): Int {
        var saveMobileSignalsCount = 0

        val cellInfosToSave = mutableListOf<CellInfoRecord>()
        val signalsToSave = mutableListOf<SignalRecord>()
        val cellLocationsToSave = mutableListOf<CellLocationRecord>()

        if (signalChunkId != null) {
            cells?.forEach {
                val iCell = it
                val map = iCell.toRecords(
                    null,
                    signalChunkId,
                    mobileNetworkTypes[iCell.subscriptionId] ?: MobileNetworkType.UNKNOWN,
                    testStartTimeNanos,
                    dataSubscriptionId,
                    NRConnectionState.NOT_AVAILABLE
                )
                if (map.keys.isNotEmpty()) {
                    val cell = map.keys.iterator().next()
                    cell?.let {
                        val signal = map.get(it)

                        if (signal?.hasNonNullSignal() == true) {
                            signalsToSave.add(signal)
                        }
                        val cellLocationRecord =
                            iCell.toCellLocation(
                                null,
                                signalChunkId,
                                System.currentTimeMillis(),
                                System.nanoTime(),
                                testStartTimeNanos
                            )
                        cellLocationRecord?.let {
                            cellLocationsToSave.add(cellLocationRecord)
                        }
                        cellInfosToSave.add(it)
                    }
                }
            }
            val signalsToSaveTmp = signalsToSave.toMutableList()

            repository.saveCellLocationRecord(cellLocationsToSave.toMutableList())
            repository.saveCellInfoRecord(cellInfosToSave.toMutableList())
            repository.saveSignalRecord(signalsToSaveTmp, false)
            saveMobileSignalsCount += signalsToSaveTmp.size
        }
        return saveMobileSignalsCount
    }

    private val onSignalInfoSaved: (signalRecord: SignalRecord) -> Unit = { signalRecord ->
        Timber.d("Last saved signal record: $signalRecord")
        lastSignalRecord = signalRecord
    }

    private fun saveNetworkInformation(
        cellNetworkInfo: NetworkInfo?,
        signalStrengthInfo: SignalStrengthInfo?,
        signalChunkId: String?,
        dataSubscriptionId: Int,
        testStartTimeNanos: Long
    ): Int {
        var saveMobileSignalsCount = 0
        if (cellNetworkInfo is CellNetworkInfo) {
            if (signalChunkId != null) {

                val cellInfoRecord = CellInfoRecord(
                    testUUID = null,
                    uuid = cellNetworkInfo.cellUUID,
                    isActive = cellNetworkInfo.isActive,
                    cellTechnology = cellNetworkInfo.cellType,
                    transportType = TransportType.CELLULAR,
                    registered = cellNetworkInfo.isRegistered,
                    isPrimaryDataSubscription = PrimaryDataSubscription.resolvePrimaryDataSubscriptionID(
                        dataSubscriptionId,
                        cellNetworkInfo.rawCellInfo?.subscriptionId
                    ).value,
                    areaCode = cellNetworkInfo.areaCode,
                    channelNumber = cellNetworkInfo.band?.channel,
                    frequency = cellNetworkInfo.band?.frequencyDL,
                    locationId = cellNetworkInfo.locationId,
                    mcc = cellNetworkInfo.mcc,
                    mnc = cellNetworkInfo.mnc,
                    primaryScramblingCode = cellNetworkInfo.scramblingCode,
                    dualSimDetectionMethod = cellNetworkInfo.dualSimDetectionMethod,
                    signalChunkId = signalChunkId,
                    cellState = cellNetworkInfo.cellState
                )
                repository.saveCellInfoRecord(listOf(cellInfoRecord))

                signalStrengthInfo?.let {
                    if (cellNetworkInfo.networkType != MobileNetworkType.UNKNOWN) {
                        repository.saveSignalStrength(
                            null,
                            signalChunkId,
                            cellNetworkInfo.cellUUID,
                            cellNetworkInfo.networkType,
                            it,
                            testStartTimeNanos,
                            NRConnectionState.NOT_AVAILABLE,
                            if (cellNetworkInfo.isActive) onSignalInfoSaved else null
                        )
                        saveMobileSignalsCount++
                    }
                }

                val cellLocationInfo = CellLocationInfo(
                    timestampMillis = System.currentTimeMillis(),
                    timestampNanos = System.nanoTime(),
                    locationId = cellNetworkInfo.locationId,
                    areaCode = cellNetworkInfo.areaCode,
                    scramblingCode = cellNetworkInfo.scramblingCode ?: 0
                )

                repository.saveCellLocation(
                    null,
                    signalChunkId,
                    cellLocationInfo,
                    testStartTimeNanos
                )
            }
        }
        return saveMobileSignalsCount
    }

    private fun saveCapabilities() {
        chunk?.id?.let { measurementRepository.saveCapabilities(null, it) }
    }

    private fun saveLocationInfo() {
        val signalChunkId = chunk?.id
        val location = locationInfo
//        Timber.d("Saving location:  UUID:$signalChunkId  ${location.toDeviceInfoLocation()} ")
        if (signalChunkId != null && location != null && locationWatcher.state == LocationState.ENABLED) {
            repository.saveGeoLocation(
                null,
                signalChunkId,
                location,
                record?.startTimeNanos ?: 0,
                true
            )
            locationInfo?.let { location ->
//                if (lastSignalMeasurementType == SignalMeasurementType.DEDICATED){
//                    dedicatedSignalMeasurementProcessor.onNewLocation(location, lastSignalRecord, networkInfo)
//                }
            }
        }
    }

    private fun savePermissionsStatus() {
        chunk?.id?.let { measurementRepository.savePermissionsStatus(null, it) }
    }

    private fun saveWlanInfo() {
        record?.let { measurementRepository.saveWlanInfo(it.id) }
    }

    private fun saveTelephonyInfo() {
        val info = networkInfo
        if (info != null && info is CellNetworkInfo) {
            record?.let {
                it.mobileNetworkType = info.networkType
                signalRepository.updateSignalMeasurementRecord(it)
            }
        }

        chunk?.id?.let { measurementRepository.saveTelephonyInfo(it) }
    }

    @ExperimentalCoroutinesApi
    override fun newUUIDSent(respondedUuid: String, session: CoverageMeasurementSession) {
        val network = networkInfo
        network?.let {
            createNewRecordBecauseOfChangedUUID(network, respondedUuid, session)
        }
    }

    override fun onSignalMeasurementChunkReadyCheckResult(
        isReady: Boolean,
        chunk: SignalMeasurementChunk?,
        validChunkPostProcessing: ValidChunkPostProcessing
    ) {
        Timber.v("Chunk is not ready: chunkID = ${chunk?.id}")
        if (isReady) {
            chunk?.let {
                Timber.i("Commit chunk data chunkID = ${it.id} sequence: ${it.sequenceNumber}")
                signalRepository.sendMeasurementChunk(it, this)
            }
            when (validChunkPostProcessing) {
                ValidChunkPostProcessing.NOTHING -> {
                }

                ValidChunkPostProcessing.CREATE_NEW_CHUNK -> {
                    createNewChunk()
                }
            }
        }
    }
}