package at.specure.measurement.signal

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.data.SignalMeasurementType
import at.rmbt.util.exception.HandledException
import at.specure.config.Config
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
import javax.inject.Named
import javax.inject.Singleton

const val MAXIMUM_TIME_NETWORK_KEEP_MILLS = 3000
const val MAXIMUM_TIME_LOCATION_KEEP_MILLS = 3000

@Singleton
class SignalMeasurementProcessor @Inject constructor(
    private val context: Context,
    // The signal measurement must use GNSS only (reported source "gps"), not the combined watcher.
    @Named("gps-location") private val locationWatcher: LocationWatcher,
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val rtrCoverageMeasurementProcessor: RtrCoverageMeasurementProcessor,
    private val config: Config,
) : Binder(), SignalMeasurementProducer, CoroutineScope {

    private var globalNetworkInfo: DetailedNetworkInfo? = null
    private var isUnstoppable = false
    private var _isActive = false
    private var _isPaused = false

    // "Preparing" phase: the service is running and holding GPS (screen-off safe) but has NOT yet
    // started the coverage session (registration + fence recording). It stays here until GPS accuracy
    // and a mobile network are both good, then flips to active and begins recording. This replaces the
    // old UI-side "waiting for GPS" screen, whose GPS died whenever the screen turned off.
    private var _isPreparing = false
    private var pendingSignalMeasurementType: SignalMeasurementType = SignalMeasurementType.DEDICATED

    // The GPS/foreground listeners are held for the whole run - both while preparing and while active.
    private val isRunning: Boolean
        get() = _isActive || _isPreparing
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
            // Only re-record if we already have a real fix; otherwise the next GPS update records the
            // first fence (avoids persisting a null/stale cached position when the session registers).
            globalLocationInfo?.let {
                rtrCoverageMeasurementProcessor.onNewLocation(it, globalNetworkInfo, batteryInfo.getTemp())
            }
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
        // A fresh fix may now satisfy the start conditions - begin recording if we were preparing.
        maybeBeginCoverageSession()

        // restart timer
        locationResetJob?.cancel()
        locationResetJob = launch {
            while (isRunning) {
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
        // A newly-available mobile network may now satisfy the start conditions - begin if preparing.
        maybeBeginCoverageSession()
    }

    override fun startMeasurement(
        unstoppable: Boolean,
        signalMeasurementType: SignalMeasurementType,
    ) {
        Timber.w("startMeasurement (already running: $isRunning)")

        // Idempotent: ignore a repeated start while preparing or already recording (e.g. the terms
        // screen starts it, then the measurement screen's onStart calls startMeasurement again).
        if (isRunning) return

        isUnstoppable = unstoppable
        pendingSignalMeasurementType = signalMeasurementType

        // Enter the preparing phase: hold GPS + the foreground service, but do NOT start the coverage
        // session yet. Note isActive stays false here, so no "recording" state is reported to the UI.
        _isPreparing = true
        postStateData()
        registerBatteryInfoReceiver(batteryInfo)
        locationWatcher.addListener(locationListener)
        signalStrengthWatcher.addListener(signalStrengthListener)
        Timber.d("Preparing coverage session - waiting for good GPS and mobile network")

        // A good fix + network may already be available: isReadyToBegin() falls back to the watchers'
        // cached values, so recording can start immediately instead of waiting up to tens of seconds
        // for the next GPS/network callback (which left the UI showing "ready" while nothing happened).
        // The cached value is used ONLY for the readiness decision - the first fence is still recorded
        // from a real location update (see maybeBeginCoverageSession), so a stale cached position is
        // never persisted as a fence.
        maybeBeginCoverageSession()
    }

    /**
     * While preparing, start the actual coverage session (registration + fence recording) as soon as
     * the GPS fix is fresh + accurate enough AND a mobile network is available. Called on every
     * location/network update, so it fires the moment the conditions are first met. No-op once
     * recording has begun.
     */
    private fun maybeBeginCoverageSession() {
        if (_isActive || !_isPreparing) return
        if (!isReadyToBegin()) return

        _isPreparing = false
        _isActive = true
        postStateData()
        Timber.d("Conditions met - starting coverage session")
        rtrCoverageMeasurementProcessor.startCoverageSession(
            sessionCreated = measurementSessionInitializedCallback,
            sessionCreationError = measurementSessionInitializationErrorCallback,
            sessionStopped = measurementSessionStoppedCallback,
        )
        // Record the first fence only from a real location update we already received. If readiness
        // was met via the watcher's cached value (globalLocationInfo still null), the first fence is
        // recorded when the next GPS update arrives, so the cached seed position is never persisted.
        globalLocationInfo?.let {
            rtrCoverageMeasurementProcessor.onNewLocation(it, globalNetworkInfo, batteryInfo.getTemp())
        }
    }

    /**
     * Readiness to begin recording: a fresh, accurate-enough GPS fix (same thresholds the fix must
     * meet during the measurement) plus an active, known mobile (cellular) network.
     *
     * Falls back to the watchers' last-known values (the same cached values the readiness UI checks)
     * so recording can begin the moment a good fix + network exist, without waiting for the service's
     * own listeners to deliver the next callback. The cached values are only used for this decision -
     * they are never recorded as a fence.
     */
    private fun isReadyToBegin(): Boolean {
        val location = globalLocationInfo ?: locationWatcher.latestLocation ?: return false
        if (!location.hasAccuracy) return false
        val ageMillis = location.ageNanos / 1_000_000L
        val gpsOk = location.accuracy <= config.minLocationAccuracyMetersDuringSignalMeasurement &&
            ageMillis <= config.maxAgeOfLocationInformationForSignalMeasurementMillis

        val network = (globalNetworkInfo ?: signalStrengthWatcher.lastDetailedNetworkInfo)?.networkInfo
        val networkOk = network is CellNetworkInfo &&
            network.networkType.intValue != MobileNetworkType.UNKNOWN.intValue

        return gpsOk && networkOk
    }

    override fun stopMeasurement(unstoppable: Boolean) {
        Timber.d("Stopping coverage session from SignalMeasurementProcessor")
        // Only tear down a coverage session if one was actually started (i.e. not aborted while still
        // in the preparing phase, where no session/registration exists yet).
        if (_isActive) {
            rtrCoverageMeasurementProcessor.stopCoverageSession(CoverageMeasurementTerminationCause.EndedByUser())
        }
        unregisterBatteryInfoReceiver(batteryInfo)
        resetStateData()
        postStateData()
        locationWatcher.removeListener(locationListener)
        signalStrengthWatcher.removeListener(signalStrengthListener)
    }

    private fun resetStateData() {
        _isActive = false
        _isPreparing = false
        _isPaused = false
        globalNetworkInfo = null
    }

    private fun postStateData() {
        _activeStateLiveData.postValue(_isActive)
        _pausedStateLiveData.postValue(_isPaused)
    }

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