package at.specure.measurement.coverage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.NetworkInfo
import at.specure.location.LocationInfo
import at.specure.measurement.coverage.data.FencesDataSource
import at.specure.measurement.coverage.domain.CoverageMeasurementProcessor
import at.specure.measurement.coverage.domain.CoverageSessionManager
import at.specure.measurement.coverage.domain.CoverageTimer
import at.specure.measurement.coverage.domain.PingProcessor
import at.specure.measurement.coverage.domain.models.CoverageMeasurementData
import at.specure.measurement.coverage.domain.models.state.CoverageMeasurementState
import at.specure.measurement.coverage.domain.validators.CoverageDataValidator
import at.specure.measurement.coverage.domain.validators.LocationValidator
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
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val config: Config,
    private val coverageLocationValidator: LocationValidator,
    private val mainCoverageDataValidator: CoverageDataValidator,
    private val coveragePingProcessor: PingProcessor,
    private val fencesDataSource: FencesDataSource,
    private val coverageSessionManager: CoverageSessionManager,
) : CoverageMeasurementProcessor, CoroutineScope {

    val coverageMeasurementData: MutableLiveData<CoverageMeasurementData?> = MutableLiveData()
    val currentSessionId: String?
        get() = coverageMeasurementData.value?.coverageMeasurementSession?.sessionId

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
    override val coroutineContext = EmptyCoroutineContext + coroutineExceptionHandler

    override fun startCoverageSession(
        sessionCreated: ((session: CoverageMeasurementSession) -> Unit)?,
        sessionCreationError: ((e: Exception) -> Unit)?)
    {
        loadingFencesJob?.cancel()
        coverageSessionManager.createSession(
            onSessionCreated = { session ->
                sessionCreated?.invoke(session)
                loadingFencesJob = loadPoints(session.sessionId)
                coverageMeasurementData.postValue(
                    CoverageMeasurementData(
                        coverageMeasurementSession = session,
                        coverageMeasurementSettings = coverageMeasurementSettings,
                    )
                )
                Timber.d("Session created: ${session.sessionId} invoking callback")
            },
            onSessionRegistered = { registeredSession ->
                scope.launch(Dispatchers.IO) {
                    onStartAndRegistrationCompleted(registeredSession)
                }
            },
            onSessionCreationError = {
                onError(it)
                sessionCreationError
            },
            scope
        )
    }

    override fun stopCoverageSession() {
        this.launch(CoroutineName("OnDedicatedSignalMeasurementStop")) {
            try {
                val avgPingMillis = coveragePingProcessor.stopPing()?.average
                val lastPoint = coverageMeasurementData.value?.points?.lastOrNull()
                fencesDataSource.updateSignalFenceAndSaveOnLeaving(
                    lastPoint,
                    leaveTimestampMillis = System.currentTimeMillis(),
                    avgPingMillis = avgPingMillis
                )
            } finally {
                signalMeasurementRepository.sendFences(
                    coverageMeasurementData.value?.coverageMeasurementSession?.sessionId ?: "",
                    coverageMeasurementData.value?.points ?: emptyList()
                )
//                cleanData()
                updateCoverageDataState(CoverageMeasurementState.FINISHED_LOOP_CORRECTLY)
            }
        }
    }

    override fun pauseCoverageSession() {
        updateCoverageDataState(CoverageMeasurementState.PAUSED)
    }

    override fun resumeCoverageSession() {
        updateCoverageDataState(CoverageMeasurementState.RUNNING)
    }

    override fun getData(): CoverageMeasurementSession {
        TODO("Not yet implemented")
    }

    private suspend fun onStartAndRegistrationCompleted(registeredAndStartedSession: CoverageMeasurementSession) {
        coveragePingProcessor.startPing(registeredAndStartedSession).collect {
            coverageMeasurementData.postValue(
                coverageMeasurementData.value?.copy(
                    currentPingStatus = null,
                    currentPingMs = it.pingStatistics?.average,
                )
            )
        }
        startMaxCoverageMeasurementSecondsReachedJob(session = registeredAndStartedSession)
        startMaxCoverageSessionSecondsReachedJob(session = registeredAndStartedSession)
        updateCoverageDataState(CoverageMeasurementState.RUNNING)
    }

    private fun onError(e: Exception) {
        coverageMeasurementData.postValue(
            coverageMeasurementData.value?.copy(
                signalMeasurementException = e,
            )
        )
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

    private fun updateCoverageDataState(state: CoverageMeasurementState) {
        coverageMeasurementData.postValue(
            coverageMeasurementData.value?.copy(
                state = state
            )
        )
    }

    fun onNewLocation(location: LocationInfo, signalRecord: SignalRecord?, networkInfo: NetworkInfo?) {
        // TODO: check how old is signal information + also handle no signal record in SignalMeasurementProcessor
        // TODO: check if airplane mode is enabled or not, check if mobile data are enabled
        val newTimestamp = System.currentTimeMillis()
        val lastRecordedFence = coverageMeasurementData.value?.points?.lastOrNull()
        val isDataValidToSaveNewFence = mainCoverageDataValidator.areDataValidToSaveNewFence(
            newTimestamp = newTimestamp,
            newLocation = location.toDeviceInfoLocation(),
            newNetworkInfo = networkInfo,
            lastRecordedFenceRecord = lastRecordedFence
        )
        if (isDataValidToSaveNewFence) {
            val sessionId = coverageMeasurementData.value?.coverageMeasurementSession?.sessionId
            if (sessionId == null) {
                Timber.e("Signal measurement Session not initialized yet - sessionId missing")
            }
            sessionId?.let { sessionIdLocal ->
                Timber.d("Creating a new point with signal record: $signalRecord")
                scope.launch(Dispatchers.IO + CoroutineName("Saving new fence")) {
                    fencesDataSource.createSignalFenceAndUpdateLastOne(
                        sessionId = sessionIdLocal,
                        location = location,
                        signalRecord = signalRecord,
                        radiusMeters = config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement,
                        lastSavedFence = lastRecordedFence,
                        entryTimestampMillis = newTimestamp,
                        avgPingMillisForLastFence = coveragePingProcessor.onNewFenceStarted()?.average
                    )
                }
            }
        } else if (isTheSameLocation(location.toDeviceInfoLocation())) { // todo verify what to do on the same location and what values needs to be replaced - how to replace ping, ...
            val lastPoint = coverageMeasurementData.value?.points?.lastOrNull()
            lastPoint?.let { point ->
                replaceSignalFenceAndSave(point, signalRecord)
            }
        }

        coverageMeasurementData.postValue(
            coverageMeasurementData.value?.copy(
                currentNetworkInfo = networkInfo,
                currentLocation = location,
            )
        )

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
            signalMeasurementRepository.updateSignalMeasurementPoint(updatedPoint)
        }
    }

    private fun isTheSameLocation(location: DeviceInfo.Location?): Boolean {
        return coverageLocationValidator.isTheSameLocation(
            newLocation = location,
            lastSavedLocation = coverageMeasurementData.value?.points?.lastOrNull()?.location
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
                    coverageMeasurementData.postValue(
                        coverageMeasurementData.value?.copy(
                            points = loadedPoints
                        )
                    )
                    Timber.d("New points loaded ${loadedPoints.size}")
                }
    }

    private fun cleanData() {
        loadingFencesJob?.cancel()
        coverageMeasurementData.postValue(null)
        coverageMeasurementSettings.signalMeasurementLastSessionId = null
    }

    private fun cleanPingDataOnly() {
        coverageMeasurementData.postValue(
            coverageMeasurementData.value?.copy(
                currentPingStatus = null,
                currentPingMs = null,
            )
        )
    }
}