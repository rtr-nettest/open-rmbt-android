package at.specure.measurement.coverage

import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.isRegistered
import at.specure.measurement.coverage.domain.CoverageMeasurementEvent
import at.specure.measurement.coverage.domain.CoverageLoopManager
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class RtrCoverageLoopManager @Inject constructor(
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
) : CoverageLoopManager {

    private val _sessionEvents = MutableSharedFlow<CoverageMeasurementEvent>()
    override fun loopFlow(): SharedFlow<CoverageMeasurementEvent> = _sessionEvents

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var registrationJob: Job? = null

    private val MAX_RETRY = 100
    private val RETRY_DELAY_MS = 2_000L

    /**
     * Starts the first measurement in loop or continue in the last one
     */
    override fun startOrContinueInLoop() {
        scope.launch {
            _sessionEvents.emit(CoverageMeasurementEvent.MeasurementInitializing)
        }

        val lastLocalMeasurementId = coverageMeasurementSettings.signalMeasurementLastMeasurementId
        val shouldMeasurementContinue = coverageMeasurementSettings.signalMeasurementShouldContinueInLastSession
        val continueInPreviousSession = (shouldMeasurementContinue && lastLocalMeasurementId != null)

        if (continueInPreviousSession) {
            loadExistingMeasurement(lastLocalMeasurementId!!)
        } else {
            createNewMeasurement()
        }
    }

    /**
     * Creates a new measurement in the loop
     */
    override fun createNewMeasurementInLoop(lastCoverageMeasurementSession: CoverageMeasurementSession) {
        scope.launch {
            val newMeasurement = prepareNextMeasurementOrKeepCurrent(lastCoverageMeasurementSession)
            handleMeasurementReady(newMeasurement)
        }
    }

    override fun endMeasurementInLoop(lastCoverageMeasurementSession: CoverageMeasurementSession) {
        scope.launch {
            val fencesCount =
                signalMeasurementRepository.loadSignalMeasurementPointRecordsForMeasurementList(
                    lastCoverageMeasurementSession.localMeasurementId
                ).size
            val hasRecordedFences = fencesCount != 0

            if (lastCoverageMeasurementSession.isRegistered() && hasRecordedFences) {
                signalMeasurementRepository.sendFences(lastCoverageMeasurementSession.localMeasurementId)
            }
        }
    }

    override suspend fun endMeasurementLoop() {
        registrationJob?.cancel()
        registrationJob = null

        _sessionEvents.emit(CoverageMeasurementEvent.MeasurementLoopEnded)
    }

    private fun prepareNextMeasurementOrKeepCurrent(lastCoverageMeasurementSession: CoverageMeasurementSession): CoverageMeasurementSession {
        val fencesCount = signalMeasurementRepository
            .loadSignalMeasurementPointRecordsForMeasurementList(lastCoverageMeasurementSession.localMeasurementId)
            .size
        val previousWasNotRegistered = lastCoverageMeasurementSession.isRegistered().not()
        val noRecordedFences = fencesCount == 0

        val newMeasurement = if (previousWasNotRegistered || noRecordedFences) {
            CoverageMeasurementSession(
                sequenceNumber = lastCoverageMeasurementSession.sequenceNumber,
                serverSessionLoopId = lastCoverageMeasurementSession.serverSessionLoopId,
                localLoopId = lastCoverageMeasurementSession.localLoopId,
                startTimeLoopMillis = lastCoverageMeasurementSession.startTimeLoopMillis,
                startLoopResponseReceivedMillis = lastCoverageMeasurementSession.startLoopResponseReceivedMillis,
            )
        } else {
            CoverageMeasurementSession(
                sequenceNumber = lastCoverageMeasurementSession.sequenceNumber + 1,
                serverSessionLoopId = lastCoverageMeasurementSession.serverSessionLoopId,
                localLoopId = lastCoverageMeasurementSession.localLoopId,
                startTimeLoopMillis = lastCoverageMeasurementSession.startTimeLoopMillis,
                startLoopResponseReceivedMillis = lastCoverageMeasurementSession.startLoopResponseReceivedMillis
            )
        }

        saveNewMeasurement(newMeasurement)
        return newMeasurement
    }

    private fun loadExistingMeasurement(measurementId: String) {
        scope.launch(CoroutineName("loadSession")) {
            Timber.d("Continue in coverage measurement: $measurementId")

            val loaded = signalMeasurementRepository.getCoverageMeasurementSession(measurementId)

            if (loaded != null) {
                handleMeasurementReady(loaded)
            } else {
                createNewMeasurement()
            }
        }
    }

    private fun createNewMeasurement() {
        scope.launch(CoroutineName("createNewSession")) {
            val measurement = CoverageMeasurementSession()
            Timber.d("Creating new coverage measurement: ${measurement.localMeasurementId}")
            saveNewMeasurement(measurement)
            handleMeasurementReady(measurement)
        }
    }

    private fun saveNewMeasurement(measurement: CoverageMeasurementSession) {
        signalMeasurementRepository.saveCoverageMeasurementSession(measurement)
        coverageMeasurementSettings.signalMeasurementLastMeasurementId = measurement.localMeasurementId
    }

    private fun handleMeasurementReady(session: CoverageMeasurementSession) {
        scope.launch {
            _sessionEvents.emit(CoverageMeasurementEvent.MeasurementCreated(session))
        }

        if (session.serverMeasurementId != null) {
            scope.launch {
                _sessionEvents.emit(CoverageMeasurementEvent.MeasurementRegistered(session))
            }
            return
        }

        registrationJob = scope.launch(Dispatchers.IO + CoroutineName("registerSession")) {
            registerMeasurementWithRetry(session)
        }
    }

    private suspend fun registerMeasurementWithRetry(session: CoverageMeasurementSession) {
        var attempt = 1

        while (attempt <= MAX_RETRY) {
            coroutineContext.ensureActive()

            try {
                val ok = signalMeasurementRepository
                    .registerCoverageMeasurement(session.localMeasurementId)
                    .first()

                if (ok) {
                    val registered =
                        signalMeasurementRepository.getCoverageMeasurementSession(session.localMeasurementId)

                    _sessionEvents.emit(
                        CoverageMeasurementEvent.MeasurementRegistered(registered!!)
                    )

                    return
                }

            } catch (e: Exception) {
                if (attempt >= MAX_RETRY) {
                    _sessionEvents.emit(
                        CoverageMeasurementEvent.MeasurementRegistrationFailed(session, e)
                    )
                    return
                }

                _sessionEvents.emit(
                    CoverageMeasurementEvent.MeasurementRegistrationRetrying(
                        session = session,
                        attempt = attempt,
                        maxAttempts = MAX_RETRY,
                        delayMs = RETRY_DELAY_MS
                    )
                )
            }

            delay(RETRY_DELAY_MS)  // automatically cancels if job is cancelled
            attempt++
        }
    }
}
