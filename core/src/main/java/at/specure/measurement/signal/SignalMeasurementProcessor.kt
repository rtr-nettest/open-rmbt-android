package at.specure.measurement.signal

import android.os.Binder
import android.os.Handler
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import at.rmbt.util.exception.HandledException
import at.specure.data.entity.ConnectivityStateRecord
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementInfo
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.repository.MeasurementRepository
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.connectivity.ConnectivityStateBundle
import at.specure.info.connectivity.ConnectivityWatcher
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.ActiveNetworkWatcher
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
import at.specure.location.cell.CellLocationLiveData
import at.specure.location.cell.CellLocationWatcher
import at.specure.test.toDeviceInfoLocation
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
    private val signalStrengthLiveData: SignalStrengthLiveData,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val activeNetworkLiveData: ActiveNetworkLiveData,
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val cellInfoWatcher: CellInfoWatcher,
    private val cellLocationLiveData: CellLocationLiveData,
    private val cellLocationWatcher: CellLocationWatcher,
    private val signalRepository: SignalMeasurementRepository,
    private val connectivityWatcher: ConnectivityWatcher,
    private val measurementRepository: MeasurementRepository
) : Binder(), SignalMeasurementProducer, CoroutineScope, SignalMeasurementChunkResultCallback {

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

    private var chunkDataSize = 0
    private var chunkCountDownRunner = Runnable {
        Timber.i("Chunk countdown timer reached")
        commitChunkData()
        createNewChunk()
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

    override fun startMeasurement(unstoppable: Boolean) {
        Timber.w("startMeasurement")
        _isActive = true
        isUnstoppable = unstoppable
        _activeStateLiveData.postValue(_isActive)
        _pausedStateLiveData.postValue(_isPaused)

        if (!isPaused) {
            handleNewNetwork(activeNetworkWatcher.currentNetworkInfo)
        }
    }

    override fun stopMeasurement(unstoppable: Boolean) {
        Timber.w("stopMeasurement")

        chunk?.state = SignalMeasurementState.SUCCESS
        commitChunkData()
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
                saveSignalStrengthInfo()
            }
        })

        cellLocation = cellLocationWatcher.latestLocation
        cellLocationLiveData.observe(owner, Observer {
            cellLocation = it
            if (isActive && !isPaused) {
                saveCellLocation()
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
                commitChunkData()
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
                commitChunkData()
                createNewRecord(newNetworkInfo)
            }
            else -> {
                Timber.i("New network other case -> new: ${newNetworkInfo?.cellUUID} old ${currentInfo?.cellUUID}")
            }
        }
    }

    private fun createNewRecord(networkInfo: NetworkInfo) {
        record = SignalMeasurementRecord(
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
            networkUUID = networkInfo.cellUUID,
            transportType = networkInfo.type,
            location = locationInfo.toDeviceInfoLocation()
        ).also {
            signalRepository.saveAndUpdateRegisteredRecord(it, newUUID, info)
        }
        chunk = null
        createNewChunk()
    }

    private fun commitChunkData() {
        chunk?.let {
            Timber.i("Commit chunk data chunkID = ${it.id} sequence: ${it.sequenceNumber}")
            signalRepository.sendMeasurementChunk(it, this)
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
            saveCellInfo()
            saveTelephonyInfo()
            saveSignalStrengthInfo()
            saveCellLocation()
            saveLocationInfo()
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
        val info = networkInfo
        if (uuid != null && info != null) {
            val infoList: List<NetworkInfo> = when (info) {
                is WifiNetworkInfo -> listOf(info)
                is CellNetworkInfo -> cellInfoWatcher.allCellInfo
                else -> throw IllegalArgumentException("Unknown cell info ${info.javaClass.simpleName}")
            }

            repository.saveCellInfo(uuid, infoList.toList(), record?.startTimeNanos ?: 0)
        }
    }

    private fun saveCapabilities() {
        chunk?.id?.let { measurementRepository.saveCapabilities(it) }
    }

    private fun saveLocationInfo() {
        val uuid = chunk?.id
        val location = locationInfo
        if (uuid != null && location != null && locationWatcher.state == LocationState.ENABLED) {
            repository.saveGeoLocation(uuid, location, record?.startTimeNanos ?: 0, false)
        }
    }

    private fun saveCellLocation() {
        val uuid = chunk?.id
        val location = cellLocation
        if (uuid != null && location != null) {
            repository.saveCellLocation(uuid, location, record?.startTimeNanos ?: 0L)
        }
    }

    private fun saveSignalStrengthInfo() {
        val uuid = chunk?.id
        val info = signalStrengthInfo
        if (uuid != null && info != null) {
            val cellUUID = networkInfo?.cellUUID ?: ""
            var mobileNetworkType: MobileNetworkType? = null
            var nrConnectionState = NRConnectionState.NOT_AVAILABLE
            if (networkInfo != null && networkInfo is CellNetworkInfo) {
                mobileNetworkType = (networkInfo as CellNetworkInfo).networkType
                nrConnectionState = (networkInfo as CellNetworkInfo).nrConnectionState
                Timber.d("Signal saving time SMP: chunkID: $uuid    starting time: ${record?.startTimeNanos}   current time: ${System.nanoTime()}")
                repository.saveSignalStrength(uuid, cellUUID, mobileNetworkType, info, record?.startTimeNanos ?: 0, nrConnectionState)

                chunkDataSize++
                if (chunkDataSize >= MAX_SIGNAL_COUNT_PER_CHUNK) {
                    Timber.v("Chunk max size reached: $chunkDataSize")
                    commitChunkData()
                    createNewChunk()
                }
            }
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
}