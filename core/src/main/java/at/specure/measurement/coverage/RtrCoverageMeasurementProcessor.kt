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
import at.specure.test.DeviceInfo
import at.specure.test.toDeviceInfoLocation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOn
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
    override val coroutineContext = EmptyCoroutineContext + coroutineExceptionHandler
    val stateManager = CoverageMeasurementDataStateManager(coverageMeasurementSettings, scope)

    override fun startCoverageSession(
        sessionCreated: ((CoverageMeasurementSession) -> Unit)?,
        sessionCreationError: ((Exception) -> Unit)?,
        sessionStopped: (() -> Unit)?,
    ) {
//        airplaneModeMonitor.start(
//            onEnabled = {
//                Timber.d("✈️ Airplane mode ENABLED → stopping session")
//                stopCoverageSession()
//            },
//            onDisabled = {
//                Timber.d("📶 Airplane mode DISABLED → resuming session")
//                resumeCoverageSession()
//            }
//        )
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
                                loadingFencesJob = loadPoints(session.sessionId)
                                stateManager.onSessionCreated(session)
                            }

                            is CoverageSessionEvent.SessionRegistered -> {
                                // this replaces your onSessionRegistered callback
                                scope.launch(Dispatchers.IO) {
                                    onStartAndRegistrationCompleted(event.session)
                                }
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
        this.launch(CoroutineName("OnDedicatedSignalMeasurementStop")) {
            try {
                val avgPingMillis = coveragePingProcessor.stopPing()?.average
                val lastFence = stateManager.getLastFence()
                fencesDataSource.updateSignalFenceAndSaveOnLeaving(
                    lastFence,
                    leaveTimestampMillis = System.currentTimeMillis(),
                    avgPingMillis = avgPingMillis
                )
            } finally {
                stateManager.onUpdateCoverageDataState(CoverageMeasurementState.FINISHED_LOOP_CORRECTLY)
                val data = stateManager.state.value
                signalMeasurementRepository.sendFences(
                    data?.coverageMeasurementSession?.sessionId ?: "",
                    data?.fences ?: emptyList()
                )
//                cleanData()
                coverageMeasurementSettings.onStopMeasurementSession()
                sessionCollectorJob?.cancel()
                sessionCollectorJob = null
                connectivityMonitor.stop()
                coverageSessionManager.endSession()
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
        coveragePingProcessor.startPing(registeredAndStartedSession).collect {
            stateManager.updatePingData(it)
        }
        startMaxCoverageMeasurementSecondsReachedJob(session = registeredAndStartedSession)
        startMaxCoverageSessionSecondsReachedJob(session = registeredAndStartedSession)
    }

    private fun onError(e: Exception) {
        stateManager.onException(e)
    }

    private fun startMaxCoverageMeasurementSecondsReachedJob(session: CoverageMeasurementSession) {
        session.maxCoverageMeasurementSeconds?.let { maxCoverageMeasurementSeconds ->
            coverageMeasurementTimer.start(maxCoverageMeasurementSeconds.seconds, { onMeasurementStop() })
        }
    }

    private fun startMaxCoverageSessionSecondsReachedJob(session: CoverageMeasurementSession) {
        session.maxCoverageSessionSeconds?.let { maxCoverageSessionSeconds ->
            coverageSessionTimer.start(maxCoverageSessionSeconds.seconds, { stopCoverageSession() })
        }
    }

    fun onNewLocation(location: LocationInfo, networkInfo: NetworkInfo?) {
        // TODO: check how old is signal information + also handle no signal record in SignalMeasurementProcessor
        // TODO: check if airplane mode is enabled or not, check if mobile data are enabled

        val coverageMeasurementDataValue = stateManager.state.value ?: return

        if (!stateManager.isInStateToAddNewFences()) return

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
        val sessionId = coverageMeasurementDataValue.coverageMeasurementSession?.sessionId
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

        stateManager.updateLocation(location)
        stateManager.updateNetworkInfo(networkInfo)

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