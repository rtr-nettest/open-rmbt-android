package at.specure.measurement.signal

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.data.SignalMeasurementType
import at.rmbt.util.exception.HandledException
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.network.MobileNetworkType
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.location.LocationInfo
import at.specure.location.LocationWatcher
import at.specure.measurement.coverage.RtrCoverageMeasurementProcessor
import at.specure.measurement.coverage.domain.models.CoverageMeasurementTerminationCause
import at.specure.temperature.BatteryInfoReceiver
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

const val MAXIMUM_TIME_NETWORK_KEEP_MILLS = 3000
const val MAXIMUM_TIME_LOCATION_KEEP_MILLS = 3000

@Singleton
class SignalMeasurementProcessor @Inject constructor(
    private val context: Context,
    private val locationWatcher: LocationWatcher,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val rtrCoverageMeasurementProcessor: RtrCoverageMeasurementProcessor,
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

    val measurementSessionInitializedCallback: (sessionId: CoverageMeasurementSession) -> Unit =
        { coverageMeasurementSession ->
            _signalMeasurementSessionIdLiveData.postValue(coverageMeasurementSession.localMeasurementId)
            rtrCoverageMeasurementProcessor.onNewLocation(
                globalLocationInfo,
                globalNetworkInfo,
                batteryInfo.getTemp()
            )
        }

    val measurementSessionInitializationErrorCallback: (exception: Exception) -> Unit =
        { exception ->
            _signalMeasurementSessionErrorLiveData.postValue(exception)
        }

    val measurementSessionStoppedCallback: () -> Unit = {
        stopMeasurement(false)
        Timber.d("Stopping service from coverage measurement")
        locationWatcher.removeListener(locationListener)
        signalStrengthWatcher.removeListener(signalStrengthListener)
        context.startService(SignalMeasurementService.stopIntent(context))
    }

    val locationListener = object : LocationWatcher.Listener {
        override fun onLocationInfoChanged(locationInfo: LocationInfo?) {
            updateLocation(locationInfo)
        }
    }

    val signalStrengthListener = object : SignalStrengthWatcher.SignalStrengthListener {
        override fun onSignalStrengthChanged(signalInfo: DetailedNetworkInfo?) {
            updateNetworkInfo(signalInfo)
        }
    }
    private val processorJob = SupervisorJob()

    override val coroutineContext =
        Dispatchers.Default +
                processorJob +
                CoroutineName("SignalMeasurementProcessor") +
                coroutineExceptionHandler

    var locationResetJob: Job? = null
    var networkInfoResetJob: Job? = null
    private var batteryInfo = BatteryInfoReceiver()

    fun updateLocation(newValue: LocationInfo?) {
        globalLocationInfo = newValue
        rtrCoverageMeasurementProcessor.onNewLocation(
            globalLocationInfo,
            globalNetworkInfo,
            batteryInfo.getTemp()
        )

        // restart timer
        locationResetJob?.cancel()
        locationResetJob = launch {
            while (isActive) {
                delay(MAXIMUM_TIME_LOCATION_KEEP_MILLS.toLong())
                // Double-check: If we still have satellites used in fix,
                // we are likely just stationary, so we keep the last location.
                if (locationWatcher.satellitesCount == 0) {
                    globalLocationInfo = null
                    rtrCoverageMeasurementProcessor.onNewLocation(
                        null,
                        globalNetworkInfo,
                        batteryInfo.getTemp()
                    )
                    break
                }
            }
        }
    }

    fun updateNetworkInfo(newValue: DetailedNetworkInfo?) {

        val isKnownCellularNetwork =
            (newValue != null) && (newValue.networkInfo != null) && (newValue.networkInfo is CellNetworkInfo && newValue.networkInfo.networkType.intValue != MobileNetworkType.UNKNOWN.intValue)
        if (isKnownCellularNetwork) {
            globalNetworkInfo = newValue
            networkInfoResetJob?.cancel()
        }

        val isNonCellularNetwork =
            (newValue != null) && (newValue.networkInfo != null) && newValue.networkInfo !is CellNetworkInfo
        if (isNonCellularNetwork) {
            globalNetworkInfo = newValue
            networkInfoResetJob?.cancel()
        }

        val isNoSignal =
                    newValue == null
                    || newValue.networkInfo == null
                    || (
                        newValue.networkInfo is CellNetworkInfo
                        && newValue.networkInfo.networkType.intValue == MobileNetworkType.UNKNOWN.intValue
                        && newValue.networkInfo.signalStrength?.value == null
                        )
        val isUnknownNetwork =
                    (
                    newValue?.networkInfo is CellNetworkInfo
                    && newValue.networkInfo.networkType.intValue == MobileNetworkType.UNKNOWN.intValue
                    && newValue.networkInfo.signalStrength?.value != null
                    )
        if (isNoSignal || isUnknownNetwork) {
            networkInfoResetJob?.cancel()
            networkInfoResetJob = launch {
                delay(MAXIMUM_TIME_NETWORK_KEEP_MILLS.toLong())
                globalNetworkInfo = if (isNoSignal) null else newValue
            }
        }
    }

    override fun startMeasurement(
        unstoppable: Boolean,
        signalMeasurementType: SignalMeasurementType,
    ) {
        val shouldStartCoverage = !_isActive
        Timber.w("startMeasurement $shouldStartCoverage")
        _isActive = true
        isUnstoppable = unstoppable
        postStateData()

        if (shouldStartCoverage) {
            registerBatteryInfoReceiver(batteryInfo)
            locationWatcher.addListener(locationListener)
            signalStrengthWatcher.addListener(signalStrengthListener)
            Timber.d("Starting coverage session")
            rtrCoverageMeasurementProcessor.startCoverageSession(
                sessionCreated = measurementSessionInitializedCallback,
                sessionCreationError = measurementSessionInitializationErrorCallback,
                sessionStopped = measurementSessionStoppedCallback,
            )
            rtrCoverageMeasurementProcessor.onNewLocation(
                globalLocationInfo,
                globalNetworkInfo,
                batteryInfo.getTemp()
            )
        }
    }

    override fun stopMeasurement(unstoppable: Boolean) {
        Timber.d("Stopping coverage session from SignalMeasurementProcessor")
        rtrCoverageMeasurementProcessor.stopCoverageSession(CoverageMeasurementTerminationCause.EndedByUser())
        unregisterBatteryInfoReceiver(batteryInfo)
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

    private fun registerBatteryInfoReceiver(batteryInfoReceiver: BatteryInfoReceiver) {
        Timber.d("REGISTERING TEMPERATURE")
        context.registerReceiver(
            batteryInfoReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    private fun unregisterBatteryInfoReceiver(batteryInfoReceiver: BatteryInfoReceiver) {
        try {
            Timber.d("UNREGISTERING TEMPERATURE")
            context.unregisterReceiver(
                batteryInfoReceiver
            )
        } catch (e: java.lang.Exception) {
            Timber.e("Error during unregistering battery info receiver: ${e.localizedMessage}")
        }
    }
}