package at.specure.measurement.coverage

import android.content.Context
import androidx.lifecycle.asFlow
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.info.network.NetworkInfo
import at.specure.location.LocationInfo
import at.specure.measurement.coverage.data.FencesDataSource
import at.specure.measurement.coverage.domain.CoverageMeasurementProcessor
import at.specure.measurement.coverage.domain.CoverageSessionEvent
import at.specure.measurement.coverage.domain.CoverageSessionManager
import at.specure.measurement.coverage.domain.CoverageTimer
import at.specure.measurement.coverage.domain.PingProcessor
import at.specure.measurement.coverage.domain.models.state.CoverageMeasurementState
import at.specure.measurement.coverage.domain.monitors.ConnectivityMonitor
import at.specure.measurement.coverage.domain.validators.CoverageDataValidator
import at.specure.measurement.coverage.domain.validators.LocationValidator
import at.specure.measurement.coverage.presentation.CoverageMeasurementDataStateManager
import at.specure.measurement.coverage.presentation.monitors.CoverageDataSimMonitor
import at.specure.test.DeviceInfo
import at.specure.test.toDeviceInfoLocation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.seconds

// TODO: resolve problems with signal uuids and coverage uuids, send coverage results, new coverage request on network change and response
// TODO: make own signal listener because now we do not get null signals on signal loss from SignalMeasurementProcessor

@Singleton
class RtrCoverageMeasurementProcessor @Inject constructor(
    private val appContext: Context,
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val config: Config,
    private val coverageLocationValidator: LocationValidator,
    private val mainCoverageDataValidator: CoverageDataValidator,
    private val coveragePingProcessor: PingProcessor,
    private val fencesDataSource: FencesDataSource,
    private val coverageSessionManager: CoverageSessionManager,
    private val connectivityMonitor: ConnectivityMonitor,
) : CoverageMeasurementProcessor, CoroutineScope {

    private val coverageSessionTimer = CoverageTimer(
        scope = CoroutineScope(Dispatchers.Default + CoroutineName("MaxCoverageSessionTimer")),
    )
    private val coverageMeasurementTimer = CoverageTimer(
        scope = CoroutineScope(Dispatchers.Default + CoroutineName("MaxCoverageMeasurementTimer")),
    )
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        if (e is HandledException) {
            // do nothing
        } else {
            throw e
        }
    }
    private var loadingFencesJob: Job? = null
    private var sessionCollectorJob: Job? = null
    private var pingJob: Job? = null
    override val coroutineContext = EmptyCoroutineContext + coroutineExceptionHandler
    val stateManager = CoverageMeasurementDataStateManager(coverageMeasurementSettings, scope)
    val dataSimMonitor = CoverageDataSimMonitor(scope = scope)
    var dataSimMonitorJob: Job? = null

    override fun startCoverageSession(
        sessionCreated: ((CoverageMeasurementSession) -> Unit)?,
        sessionCreationError: ((Exception) -> Unit)?,
        sessionStopped: (() -> Unit)?,
    ) {
        connectivityMonitor.start(
            onAirplaneEnabled = {
                Timber.d("✈️ Airplane mode changed to ENABLED → stopping coverage session")
                stopCoverageSession()
            },
            onAirplaneDisabled = {
                Timber.d("📶 Airplane mode changed to DISABLED → resuming session")
                resumeCoverageSession()
            },
            onMobileDataEnabled =  {
                Timber.d("📶 Mobile data changed to ENABLED → resuming session")
                resumeCoverageSession()
            },
            onMobileDataDisabled = {
                Timber.d("📶 Mobile data changed to DISABLED → stopping coverage session")
                stopCoverageSession()
            },
            onIpAddressChanged = {
                Timber.d("🌐 IP address changed to $it → stopping measurement")
                // todo: this is not working properly
//                onMeasurementStop()
            },
        )
        dataSimMonitorJob = scope.launch() {
            dataSimMonitor.activeDataSim
                .onEach { Timber.d("data sim subsId changed to: $it") }
                .drop(1)
                .debounce { 1_000 }
                .collect { subscriptionId ->
                    Timber.d("📶 Active data sim changed to subId: $subscriptionId -> stopping measurement")
                    onMeasurementStop()
            }
        }

        dataSimMonitor.start()

        stateManager.initData()
        coverageSessionManager.createSession(scope)

        if (sessionCollectorJob == null) {
            sessionCollectorJob = scope.launch {
                try {
                    coverageSessionManager.sessionFlow().collect { event ->
                        when (event) {

                            is CoverageSessionEvent.SessionInitializing -> {
                                // optional: show loading UI
                            }

                            is CoverageSessionEvent.SessionCreated -> {
                                val session = event.session
                                sessionCreated?.invoke(session)   // 🔥 same callback you originally had
                                loadingFencesJob?.cancel()
                                loadingFencesJob = loadPoints(session.localMeasurementId)
                                stateManager.onSessionCreated(session)
                            }

                            is CoverageSessionEvent.SessionRegistered -> {
                                // this replaces your onSessionRegistered callback
                                onStartAndRegistrationCompleted(event.session)
                            }

                            is CoverageSessionEvent.SessionRegistrationRetrying -> {
                                // optional: show retry attempt info in UI
                            }

                            is CoverageSessionEvent.SessionRegistrationFailed -> {
                                onError(event.error ?: Exception("Unknown registration failure"))
                                sessionCreationError?.invoke(event.error ?: Exception("Unknown registration failure"))
                            }

                            is CoverageSessionEvent.SessionCreationError -> {
                                onError(event.error)
                                sessionCreationError?.invoke(event.error)
                            }

                            CoverageSessionEvent.SessionEnded -> {
                                sessionStopped?.invoke()
                                sessionCollectorJob?.cancel()
                                sessionCollectorJob = null
                                cancelPingJob()
                            }
                        }
                    }
                } finally {
                    sessionCollectorJob = null
                }
            }
        }
    }

    override fun stopCoverageSession() {
        if (stateManager.state.value.state != CoverageMeasurementState.FINISHED_LOOP_CORRECTLY) {
            this.launch(CoroutineName("OnDedicatedSignalMeasurementStop")) {
                try {
                    val avgPingMillis = coveragePingProcessor.stopPing()?.average
                    val lastFence = stateManager.getLastFence()
                    fencesDataSource.updateSignalFenceAndSaveOnLeaving(
                        lastFence,
                        leaveTimestampMillis = System.currentTimeMillis(),
                        avgPingMillis = avgPingMillis
                    )
                    stateManager.onUpdateCoverageDataState(CoverageMeasurementState.FINISHED_LOOP_CORRECTLY)
                    val data = stateManager.state.value
                    Timber.d("Sending coverageResult")
                    signalMeasurementRepository.sendFences(
                        data?.coverageMeasurementSession?.localMeasurementId ?: ""
                    )
                } finally {
//                cleanData()
                    coverageMeasurementSettings.onStopMeasurementSession()
                    coverageSessionManager.endSession()
                    dataSimMonitorJob?.cancel()
                    dataSimMonitorJob = null
                    connectivityMonitor.stop()
                }
            }
        }
    }

    override fun pauseCoverageSession() {
        stateManager.onUpdateCoverageDataState(CoverageMeasurementState.PAUSED)
    }

    override fun resumeCoverageSession() {
        stateManager.onUpdateCoverageDataState(CoverageMeasurementState.RUNNING)
    }

    override fun getData(): CoverageMeasurementSession {
        TODO("Not yet implemented")
    }

    private suspend fun onStartAndRegistrationCompleted(registeredAndStartedSession: CoverageMeasurementSession) {
        stateManager.onUpdateCoverageDataState(CoverageMeasurementState.RUNNING)
        Timber.d("Starting ping")
        cancelPingJob()
        pingJob = scope.launch(CoroutineName("PingJobCoroutine")) {
            coveragePingProcessor.startPing(registeredAndStartedSession).collect { pingData ->
                stateManager.updatePingData(pingData)
            }
        }
        startMaxCoverageMeasurementSecondsReachedJob(session = registeredAndStartedSession)
        startMaxCoverageSessionSecondsReachedJob(session = registeredAndStartedSession)
        Timber.d("Starting cancellation jobs")
    }

    private fun cancelPingJob() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun onError(e: Exception) {
        stateManager.onException(e)
    }

    private fun startMaxCoverageMeasurementSecondsReachedJob(session: CoverageMeasurementSession) {
        session.maxCoverageMeasurementSeconds?.let { maxCoverageMeasurementSeconds ->
            Timber.d("Starting maxCoverageMeasurementSeconds timer with ${maxCoverageMeasurementSeconds.seconds.inWholeSeconds} seconds")
            coverageMeasurementTimer.start(maxCoverageMeasurementSeconds.seconds, {
                Timber.d("Stopping coverage measurement because of max time reached")
                onMeasurementStop()
            })
        }
    }

    private fun startMaxCoverageSessionSecondsReachedJob(session: CoverageMeasurementSession) {
        session.maxCoverageSessionSeconds?.let { maxCoverageSessionSeconds ->
            Timber.d("Starting maxCoverageSessionSeconds timer with ${maxCoverageSessionSeconds.seconds.inWholeSeconds} seconds")
            coverageSessionTimer.start(maxCoverageSessionSeconds.seconds, {
                Timber.d("Stopping coverage session because of max time reached")
                stopCoverageSession()
            })
        }
    }

    fun onNewLocation(location: LocationInfo?, networkInfo: NetworkInfo?) {
        // TODO: check how old is signal information + also handle no signal record in SignalMeasurementProcessor
        // TODO: check if airplane mode is enabled or not, check if mobile data are enabled

        val coverageMeasurementDataValue = stateManager.state.value ?: return

        stateManager.updateLocation(location)
        stateManager.updateNetworkInfo(networkInfo)

        val isConnectionStateValid = checkForTheConnectionState()
        if (!isConnectionStateValid) return

        if (!stateManager.isInStateToAddNewFences()) return
        if (location == null) return

        val newTimestamp = System.currentTimeMillis()
        val newLocation = location.toDeviceInfoLocation()
        Timber.d("DeviceInfoLocation: $newLocation \nLocationInfo: $location")
        val lastRecordedFence = coverageMeasurementDataValue.fences.lastOrNull()
        Timber.d("lastPoint = $lastRecordedFence")
        val isDataValidToSaveNewFence = mainCoverageDataValidator.areDataValidToSaveNewFence(
            newTimestamp = newTimestamp,
            newLocation = newLocation,
            newNetworkInfo = networkInfo,
            lastRecordedFenceRecord = lastRecordedFence
        )
        val sessionId = coverageMeasurementDataValue.coverageMeasurementSession?.localMeasurementId
        if (sessionId == null) {
            Timber.e("Signal measurement Session not initialized yet - sessionId missing")
            return
        }

        if (isDataValidToSaveNewFence) {
            saveNewFence(
                sessionId = sessionId,
                newLocation = newLocation!!,
                newTimestamp = newTimestamp,
                networkInfo = networkInfo,
                lastRecordedFence = lastRecordedFence
            )
        }

        /*
        TODO: Same location to replace points?
        else if (isTheSameLocation(location.toDeviceInfoLocation())) { // todo verify what to do on the same location and what values needs to be replaced - how to replace ping, ...
            val lastPoint = coverageMeasurementData.value?.points?.lastOrNull()
            lastPoint?.let { point ->
                replaceSignalFenceAndSave(point, signalRecord)
            }
        }*/



    }

    private fun checkForTheConnectionState(): Boolean {
        val airplaneModeEnabled = connectivityMonitor.isAirplaneModeCurrentlyEnabled()
        val mobileDataEnabled = connectivityMonitor.isMobileDataEnabled()
        if (airplaneModeEnabled || !mobileDataEnabled) {
            stateManager.onUpdateCoverageDataState(CoverageMeasurementState.PAUSED)
            return false
        }
        return true
    }

    private fun saveNewFence(
        sessionId: String,
        newLocation: DeviceInfo.Location,
        newTimestamp: Long,
        networkInfo: NetworkInfo?,
        lastRecordedFence: CoverageMeasurementFenceRecord?,
    ) {
        scope.launch(Dispatchers.IO + CoroutineName("Saving new fence")) {
            fencesDataSource.createSignalFenceAndUpdateLastOne(
                sessionId = sessionId,
                location = newLocation,
                signalRecord = null,
                radiusMeters = config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement.toDouble(),
                lastSavedFence = lastRecordedFence,
                entryTimestampMillis = newTimestamp,
                networkInfo = networkInfo,
                avgPingMillisForLastFence = coveragePingProcessor.onNewFenceStarted()?.average
            )
        }
    }



    /**
     * Location is kept from original fence as we could end in the cascade of updates with drifting
     * original location to even few tenth of meters
     */
    private fun replaceSignalFenceAndSave(
        lastPoint: CoverageMeasurementFenceRecord?,
        signalRecord: SignalRecord?
    ) = io {
        val updatedPoint = lastPoint?.copy(
            signalRecordId = signalRecord?.signalMeasurementPointId,
            entryTimestampMillis = System.currentTimeMillis(),
            technologyId = signalRecord?.mobileNetworkType?.intValue,
        )
        updatedPoint?.let {
            signalMeasurementRepository.updateSignalMeasurementFence(updatedPoint)
        }
    }

    private fun isTheSameLocation(location: DeviceInfo.Location?): Boolean {
        return coverageLocationValidator.isTheSameLocation(
            newLocation = location,
            lastSavedLocation = stateManager.getLastFence()?.location
        )
    }

    /**
     * Stop of single measurement in a loop
     */
    fun onMeasurementStop() {
        // todo
    }

    fun onNetworkChanged() {
        // todo: what to do on a new network
    }

    private fun loadPoints(sessionId: String) = scope.launch(Dispatchers.IO) {
        fencesDataSource.loadCoverageFences(sessionId)
            .asFlow().
            flowOn(Dispatchers.IO)
                .collect { loadedPoints ->
                    stateManager.updatePoints(loadedPoints)
                }
    }

    private fun cleanData() {
        loadingFencesJob?.cancel()
        stateManager.initData()
        coverageMeasurementSettings.signalMeasurementLastSessionId = null
    }

    private fun cleanPingDataOnly() {
        stateManager.updatePingData(null)
    }

    fun onCoverageConfigurationChanged() {
        fencesDataSource.updateLastFenceRadius(
            stateManager.getLastFence(),
            config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement.toDouble()
        )
    }

}