package at.specure.measurement.signal

import android.os.Binder
import android.os.Handler
import android.telephony.SubscriptionManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import at.rmbt.client.control.getCurrentDataSubscriptionId
import at.rmbt.util.exception.HandledException
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.ConnectivityStateRecord
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementInfo
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.MeasurementRepository
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.connectivity.ConnectivityStateBundle
import at.specure.info.connectivity.ConnectivityWatcher
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.network.NRConnectionState
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.location.cell.CellLocationInfo
import at.specure.test.SignalMeasurementType
import at.specure.test.toDeviceInfoLocation
import at.specure.util.filterOnlyActiveDataCell
import at.specure.util.mobileNetworkType
import at.specure.util.toCellInfoRecord
import at.specure.util.toCellLocation
import at.specure.util.toSignalRecord
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.model.cell.ICell
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

private const val MAX_SIGNAL_COUNT_PER_CHUNK = 25
private const val MAX_SIGNAL_UPTIME_PER_CHUNK_MIN = 10L
private const val MAX_TIME_NETWORK_UNREACHABLE_SECONDS = 300L

@Singleton
class SignalMeasurementProcessor @Inject constructor(
    private val repository: TestDataRepository,
    private val locationWatcher: LocationWatcher,
    private val netmonster: INetMonster,
    private val signalStrengthLiveData: SignalStrengthLiveData,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val activeNetworkLiveData: ActiveNetworkLiveData,
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val subscriptionManager: SubscriptionManager,
    private val signalRepository: SignalMeasurementRepository,
    private val connectivityWatcher: ConnectivityWatcher,
    private val measurementRepository: MeasurementRepository
) : Binder(), SignalMeasurementProducer, CoroutineScope, SignalMeasurementChunkResultCallback, SignalMeasurementChunkReadyCallback {

    private var isUnstoppable = false
    private var _isActive = false
    private var _isPaused = false
    private val _activeStateLiveData = MutableLiveData<Boolean>()
    private val _pausedStateLiveData = MutableLiveData<Boolean>()

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

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        if (e is HandledException) {
            // do nothing
        } else {
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

    override fun setEndAlarm() {
        // not necessary to implement here
    }

    override fun startMeasurement(unstoppable: Boolean, signalMeasurementType: SignalMeasurementType) {
        Timber.w("startMeasurement")
        _isActive = true
        isUnstoppable = unstoppable
        _activeStateLiveData.postValue(_isActive)
        _pausedStateLiveData.postValue(_isPaused)
        lastSignalMeasurementType = signalMeasurementType

        if (!isPaused) {
            handleNewNetwork(activeNetworkWatcher.currentNetworkInfo)
        }
    }

    override fun stopMeasurement(unstoppable: Boolean) {
        Timber.w("stopMeasurement")

        chunk?.state = SignalMeasurementState.SUCCESS
        commitChunkData(ValidChunkPostProcessing.NOTHING)
        isUnstoppable = unstoppable
        _isActive = false
        _isPaused = false
        networkInfo = null
        record = null
        chunk = null
        _activeStateLiveData.postValue(_isActive)
        _pausedStateLiveData.postValue(_isPaused)
    }

    override fun pauseMeasurement(unstoppable: Boolean) {
        Timber.w("pauseMeasurement")
        isUnstoppable = unstoppable
        _isPaused = true
        _pausedStateLiveData.postValue(_isPaused)
    }

    override fun resumeMeasurement(unstoppable: Boolean) {
        Timber.w("resumeMeasurement")
        isUnstoppable = unstoppable
        _isPaused = false
        _pausedStateLiveData.postValue(_isPaused)
        if (isActive) {
            handleNewNetwork(activeNetworkWatcher.currentNetworkInfo)
        }
    }

    fun bind(owner: LifecycleOwner) {
        activeNetworkLiveData.observe(owner, Observer {
            if (isActive && !isPaused) {
                handleNewNetwork(it?.networkInfo)
                saveCellInfo()
            }
        })

        if (locationWatcher.state == LocationState.ENABLED) {
            locationInfo = locationWatcher.latestLocation
        }
        locationWatcher.liveData.observe(owner, Observer { info ->
            if (locationWatcher.state == LocationState.ENABLED) {
                locationInfo = info
                if (isActive && !isPaused) {
                    saveLocationInfo()
                }
            }
        })

        signalStrengthInfo = signalStrengthWatcher.lastSignalStrength
        signalStrengthLiveData.observe(owner, Observer { info ->
            signalStrengthInfo = info
            if (isActive && !isPaused) {
                saveCellInfo()
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
    }

    private fun handleNewNetwork(newInfo: NetworkInfo?) {
        val currentInfo = networkInfo
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
                if ((lastSeenNetworkInfo != null) && (lastSeenNetworkInfo?.type == newNetworkInfo.type) && (lastSeenNetworkTimestampMillis?.plus(
                        TimeUnit.SECONDS.toMillis(
                            MAX_TIME_NETWORK_UNREACHABLE_SECONDS
                        )
                    ) ?: -1 >= System.currentTimeMillis())
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
                Timber.i("Network changed")
                networkInfo = newNetworkInfo
                commitChunkData(ValidChunkPostProcessing.NOTHING)
                createNewRecord(newNetworkInfo)
            }
            else -> {
                Timber.i("New network other case -> new: ${newNetworkInfo?.cellUUID} old ${currentInfo?.cellUUID}")
            }
        }
    }

    private fun createNewRecord(networkInfo: NetworkInfo) {
        record = SignalMeasurementRecord(
            signalMeasurementType = lastSignalMeasurementType,
            networkUUID = networkInfo.cellUUID,
            transportType = networkInfo.type,
            location = locationInfo.toDeviceInfoLocation()
        ).also {
            signalRepository.saveAndRegisterRecord(it)
        }
        chunk = null
        createNewChunk()
    }

    private fun createNewRecordBecauseOfChangedUUID(networkInfo: NetworkInfo, newUUID: String, info: SignalMeasurementInfo) {
        record = SignalMeasurementRecord(
            signalMeasurementType = lastSignalMeasurementType,
            networkUUID = networkInfo.cellUUID,
            transportType = networkInfo.type,
            location = locationInfo.toDeviceInfoLocation()
        ).also {
            signalRepository.saveAndUpdateRegisteredRecord(it, newUUID, info)
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
    private fun updateChunkInfo(chunkId: String) = launch {
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
                Timber.i("New chunk created chunkID = ${chunk.id} sequence: ${chunk.sequenceNumber}")
            }

            if (saveWlanInfo) {
                saveWlanInfo()
            }
            if (chunk?.sequenceNumber == 0) {
                saveCellInfo()
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
        chunkCountDownHandler.postDelayed(chunkCountDownRunner, TimeUnit.MINUTES.toMillis(MAX_SIGNAL_UPTIME_PER_CHUNK_MIN))
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

    private fun saveCellInfo() {
        val uuid = chunk?.id
        var cells: List<ICell>? = null
        try {
            cells = netmonster.getCells()
        } catch (e: SecurityException) {
            Timber.e("SecurityException: Not able to read telephonyManager.allCellInfo")
        } catch (e: IllegalStateException) {
            Timber.e("IllegalStateException: Not able to read telephonyManager.allCellInfo")
        } catch (e: NullPointerException) {
            Timber.e("NullPointerException: Not able to read telephonyManager.allCellInfo from other reason")
        }

        val dataSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()

        val primaryCells = cells?.filterOnlyActiveDataCell(dataSubscriptionId)

        val cellInfosToSave = mutableListOf<CellInfoRecord>()
        val signalsToSave = mutableListOf<SignalRecord>()
        val cellLocationsToSave = mutableListOf<CellLocationRecord>()

        if (uuid != null) {
            val testStartTimeNanos = record?.startTimeNanos ?: 0
            primaryCells?.toList()?.let {
                it.forEach { iCell ->
                    val cellInfoRecord = iCell.toCellInfoRecord(uuid, netmonster)

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
                            if (signalRecord.hasNonNullSignal()) {
                                signalsToSave.add(signalRecord)
                            }
                        }
                    }
                    val cellLocationRecord = iCell.toCellLocation(uuid, System.currentTimeMillis(), System.nanoTime(), testStartTimeNanos)
                    cellLocationRecord?.let {
                        cellLocationsToSave.add(cellLocationRecord)
                    }
                    cellInfosToSave.add(cellInfoRecord)
                }
                repository.saveCellLocationRecord(cellLocationsToSave)
                repository.saveCellInfoRecord(cellInfosToSave)
                repository.saveSignalRecord(signalsToSave)
                chunkDataSize += signalsToSave.size
                if (chunkDataSize >= MAX_SIGNAL_COUNT_PER_CHUNK) {
                    Timber.v("Chunk max size reached: $chunkDataSize")
                    commitChunkData(ValidChunkPostProcessing.CREATE_NEW_CHUNK)
                }
            }
        }
    }

    private fun saveCapabilities() {
        chunk?.id?.let { measurementRepository.saveCapabilities(it) }
    }

    private fun saveLocationInfo() {
        val uuid = chunk?.id
        val location = locationInfo
        Timber.d("Saving location:  UUID:$uuid  ${location.toDeviceInfoLocation()} ")
        if (uuid != null && location != null && locationWatcher.state == LocationState.ENABLED) {
            repository.saveGeoLocation(uuid, location, record?.startTimeNanos ?: 0, false)
        }
    }

    private fun savePermissionsStatus() {
        chunk?.id?.let { measurementRepository.savePermissionsStatus(it) }
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
    override fun newUUIDSent(respondedUuid: String, info: SignalMeasurementInfo) {
        val network = networkInfo
        network?.let {
            createNewRecordBecauseOfChangedUUID(network, respondedUuid, info)
        }
    }

    override fun onSignalMeasurementChunkReadyCheckResult(
        isReady: Boolean,
        chunk: SignalMeasurementChunk?,
        validChunkPostProcessing: ValidChunkPostProcessing
    ) {
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