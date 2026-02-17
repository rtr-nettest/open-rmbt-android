package at.specure.measurement.signal

import android.content.Context
import android.os.Binder
import android.telephony.SubscriptionManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.data.SignalMeasurementType
import at.rmbt.util.exception.HandledException
import at.specure.config.Config
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.repository.MeasurementRepository
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.connectivity.ConnectivityWatcher
import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.location.LocationInfo
import at.specure.location.LocationWatcher
import at.specure.measurement.coverage.RtrCoverageMeasurementProcessor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

@Singleton
class SignalMeasurementProcessor @Inject constructor(
    private val context: Context,
    private val config: Config,
    private val repository: TestDataRepository,
    private val locationWatcher: LocationWatcher,
    private val signalStrengthLiveData: SignalStrengthLiveData,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val subscriptionManager: SubscriptionManager,
    private val signalRepository: SignalMeasurementRepository,
    private val connectivityWatcher: ConnectivityWatcher,
    private val measurementRepository: MeasurementRepository,
    private val rtrCoverageMeasurementProcessor: RtrCoverageMeasurementProcessor,
    private val cellInfoWatcher: CellInfoWatcher
) : Binder(), SignalMeasurementProducer, CoroutineScope {

    private var globalNetworkInfo: DetailedNetworkInfo? = null
    private var isUnstoppable = false
    private var _isActive = false
    private var _isPaused = false
    private val _activeStateLiveData = MutableLiveData<Boolean>()
    private val _pausedStateLiveData = MutableLiveData<Boolean>()
    private val _signalMeasurementSessionIdLiveData = MutableLiveData<String?>()
    private val _signalMeasurementSessionErrorLiveData = MutableLiveData<Exception?>()

    private var globalLocationInfo: LocationInfo? = null

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

    val measurementSessionInitializedCallback: (sessionId: CoverageMeasurementSession) -> Unit = { coverageMeasurementSession ->
        _signalMeasurementSessionIdLiveData.postValue(coverageMeasurementSession.localMeasurementId)
        rtrCoverageMeasurementProcessor.onNewLocation(globalLocationInfo, globalNetworkInfo)
    }

    val measurementSessionInitializationErrorCallback: (exception: Exception) -> Unit = { exception ->
        _signalMeasurementSessionErrorLiveData.postValue(exception)
    }

    val measurementSessionStoppedCallback: () -> Unit = {
        stopMeasurement(false)
        Timber.d("Stopping service from coverage measurement")
        context.startService(SignalMeasurementService.stopIntent(context))
    }

    val locationListener = object : LocationWatcher.Listener {
        override fun onLocationInfoChanged(locationInfo: LocationInfo?) {
            globalLocationInfo = locationInfo
            rtrCoverageMeasurementProcessor.onNewLocation(globalLocationInfo, globalNetworkInfo)
        }
    }

    val signalStrengthListener = object: SignalStrengthWatcher.SignalStrengthListener {
        override fun onSignalStrengthChanged(signalInfo: DetailedNetworkInfo?) {
            globalNetworkInfo = signalInfo
        }
    }

    override fun startMeasurement(
        unstoppable: Boolean,
        signalMeasurementType: SignalMeasurementType
    ) {
        val shouldStartCoverage = !_isActive
        Timber.w("startMeasurement $shouldStartCoverage")
        _isActive = true
        isUnstoppable = unstoppable
        postStateData()

        locationWatcher.addListener(locationListener)
        signalStrengthWatcher.addListener(signalStrengthListener)

        if (shouldStartCoverage) {
            Timber.d("Starting coverage session")
            rtrCoverageMeasurementProcessor.startCoverageSession(
                sessionCreated = measurementSessionInitializedCallback,
                sessionCreationError = measurementSessionInitializationErrorCallback,
                sessionStopped = measurementSessionStoppedCallback,
            )
            rtrCoverageMeasurementProcessor.onNewLocation(globalLocationInfo, globalNetworkInfo)
        }
    }

    override fun stopMeasurement(unstoppable: Boolean) {
        Timber.d("Stopping coverage session from SignalMeasurementProcessor")
        rtrCoverageMeasurementProcessor.stopCoverageSession()
        resetStateData()
        postStateData()
        locationWatcher.removeListener(locationListener)
        signalStrengthWatcher.removeListener(signalStrengthListener)
    }

    private fun resetStateData() {
        _isActive = false
        _isPaused = false
        globalNetworkInfo = null
    }

    private fun postStateData() {
        _activeStateLiveData.postValue(_isActive)
        _pausedStateLiveData.postValue(_isPaused)
    }

    fun bind(owner: LifecycleOwner) {

    }

    private fun isSignalMeasurementRunning() = isActive

}