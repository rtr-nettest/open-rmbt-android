package at.specure.measurement.signal

import android.content.Context
import android.os.Binder
import android.telephony.SubscriptionManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import at.rmbt.client.control.data.SignalMeasurementType
import at.rmbt.util.exception.HandledException
import at.specure.config.Config
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.MeasurementRepository
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.connectivity.ConnectivityWatcher
import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.measurement.coverage.RtrCoverageMeasurementProcessor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

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

    private var globalNetworkInfo: DetailedNetworkInfo? = null
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

    private var locationInfo: LocationInfo? = null
    private var signalStrengthInfo: SignalStrengthInfo? = null

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
        _signalMeasurementSessionIdLiveData.postValue(coverageMeasurementSession.localMeasurementId)
    }

    val measurementSessionInitializationErrorCallback: (exception: Exception) -> Unit = { exception ->
        _signalMeasurementSessionErrorLiveData.postValue(exception)
    }

    val measurementSessionStoppedCallback: () -> Unit = {
        stopMeasurement(false)
        Timber.d("Stopping service from coverage measurement")
        context.startService(SignalMeasurementService.stopIntent(context))
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
            rtrCoverageMeasurementProcessor.onNewLocation(locationInfo, globalNetworkInfo)
        }

        if (isSignalMeasurementRunning()) {
            handleNewNetwork(signalStrengthWatcher.lastDetailedNetworkInfo)
        }
    }

    override fun stopMeasurement(unstoppable: Boolean) {
        Timber.w("stopMeasurement")

        chunk?.state = SignalMeasurementState.SUCCESS
        isUnstoppable = unstoppable
        if (lastSignalMeasurementType == SignalMeasurementType.DEDICATED) {
            Timber.d("Stopping coverage session from SignalMeasurementProcessor")
            rtrCoverageMeasurementProcessor.stopCoverageSession()
        }
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
            handleNewNetwork(signalStrengthWatcher.lastDetailedNetworkInfo)
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
                        Timber.d("passing new info with network: ${globalNetworkInfo?.networkInfo?.type}")
                        rtrCoverageMeasurementProcessor.onNewLocation(location, globalNetworkInfo)
                    }
                }
                if (isSignalMeasurementRunning()) {
//                    saveLocationInfo()
                }
            }
        })

        signalStrengthInfo = signalStrengthWatcher.lastSignalStrength
        signalStrengthLiveData.observe(owner, Observer { info ->
            signalStrengthInfo = info?.signalStrengthInfo
            globalNetworkInfo = info
            if (isSignalMeasurementRunning()) {
                handleNewNetwork(info)
//                saveCellInfo(info)
            }
        })

        connectivityWatcher.connectivityStateLiveData.observe(owner, Observer { state ->
            state?.let {
                if (isActive) {
//                    saveConnectivityState(state)
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

    private fun handleNewNetwork(newInfo: DetailedNetworkInfo?) {
        val currentInfo = networkInfo
        globalNetworkInfo = newInfo
        var newNetworkInfo = newInfo?.networkInfo
        if (newInfo?.networkInfo?.type != TransportType.CELLULAR) {
            newNetworkInfo = null
        }
        when {
            newNetworkInfo == null && currentInfo != null -> {
                Timber.i("Network become unavailable")
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
                }
            }
            // it must be started like new chunk on different type of the network because network type is common for entire chunk
            newNetworkInfo != null && currentInfo != null && currentInfo.type != newNetworkInfo.type -> {
//                Timber.i("Network changed")
                networkInfo = newNetworkInfo
            }

            else -> {
//                Timber.i("New network other case -> new: ${newNetworkInfo?.cellUUID} old ${currentInfo?.cellUUID}")
            }
        }
    }

    @ExperimentalCoroutinesApi
    override fun newUUIDSent(respondedUuid: String, session: CoverageMeasurementSession) {
        val network = networkInfo
        network?.let {
        }
    }

    override fun onSignalMeasurementChunkReadyCheckResult(
        isReady: Boolean,
        chunk: SignalMeasurementChunk?,
        validChunkPostProcessing: ValidChunkPostProcessing
    ) {

    }
}