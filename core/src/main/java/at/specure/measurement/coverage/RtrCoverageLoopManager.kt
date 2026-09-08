package at.specure.measurement.coverage

import android.os.SystemClock
import at.specure.config.Config
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.isRegistered
import at.specure.info.connectivity.ConnectivityWatcher
import at.specure.measurement.coverage.domain.CoverageMeasurementEvent
import at.specure.measurement.coverage.domain.CoverageLoopManager
import at.specure.measurement.coverage.domain.models.CoverageMeasurementTerminationCause
import at.specure.measurement.coverage.domain.models.CoverageRegistrationTimeoutException
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
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

@Singleton
class RtrCoverageLoopManager @Inject constructor(
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
    private val config: Config,
    private val connectivityWatcher: ConnectivityWatcher,
) : CoverageLoopManager {

    private val _sessionEvents = MutableSharedFlow<CoverageMeasurementEvent>()
    override fun loopFlow(): SharedFlow<CoverageMeasurementEvent> = _sessionEvents

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var registrationJob: Job? = null

    // Only attempt registration when there is actual network connectivity, and then no more often
    // than every 15 s. The whole flow gives up after the total time budget below.
    private val RETRY_INTERVAL_MS = 15_000L
    private val REGISTRATION_TIMEOUT_MS = 120L * 60 * 1_000 // 2 hours

    /** How long the app keeps trying to register before giving up, in whole minutes. */
    private val registrationTimeoutMinutes: Int
        get() = (REGISTRATION_TIMEOUT_MS / 60_000L).toInt()

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
    override fun createNewMeasurementInLoop(
        lastCoverageMeasurementSession: CoverageMeasurementSession,
        forceNewSession: Boolean
    ) {
        scope.launch {
            val newMeasurement =
                prepareNextMeasurementOrKeepCurrent(lastCoverageMeasurementSession, forceNewSession)
            handleMeasurementReady(newMeasurement)
        }
    }

    override fun endMeasurementInLoop(lastCoverageMeasurementSession: CoverageMeasurementSession, reasonToTerminate: CoverageMeasurementTerminationCause) {
        scope.launch {
            val updatedSession = lastCoverageMeasurementSession.copy(reasonToTerminate = reasonToTerminate.cause)
            signalMeasurementRepository.saveCoverageMeasurementSession(updatedSession)
            val fences = signalMeasurementRepository.loadSignalMeasurementPointRecordsForMeasurementList(updatedSession.localMeasurementId)
            val fencesCount = fences.size
            val hasRecordedFences = fencesCount != 0

            if (updatedSession.isRegistered() && hasRecordedFences) {
                signalMeasurementRepository.sendFences(updatedSession.localMeasurementId, null)
            }
        }
    }

    override suspend fun endMeasurementLoop() {
        registrationJob?.cancel()
        registrationJob = null
        config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement = coverageMeasurementSettings.baseMinimalDistanceBetweenFenceCentersMeters
        _sessionEvents.emit(CoverageMeasurementEvent.MeasurementLoopEnded)
    }

    private fun prepareNextMeasurementOrKeepCurrent(
        lastCoverageMeasurementSession: CoverageMeasurementSession,
        forceNewSession: Boolean = false
    ): CoverageMeasurementSession {
        val fencesCount = signalMeasurementRepository
            .loadSignalMeasurementPointRecordsForMeasurementList(lastCoverageMeasurementSession.localMeasurementId)
            .size
        val previousWasNotRegistered = lastCoverageMeasurementSession.isRegistered().not()
        val noRecordedFences = fencesCount == 0

        val newMeasurement = if (!forceNewSession && (previousWasNotRegistered || noRecordedFences)) {
            Timber.d("SDT Continue in previous session ${lastCoverageMeasurementSession.localMeasurementId} as it was not registered ${previousWasNotRegistered} or no fences were recorded ${noRecordedFences}")
            lastCoverageMeasurementSession
        } else {
            val newSession = CoverageMeasurementSession(
                sequenceNumber = lastCoverageMeasurementSession.sequenceNumber + 1,
                serverSessionLoopId = lastCoverageMeasurementSession.serverSessionLoopId,
                localLoopId = lastCoverageMeasurementSession.localLoopId,
                startTimeLoopMillis = lastCoverageMeasurementSession.startTimeLoopMillis,
                startLoopResponseReceivedMillis = lastCoverageMeasurementSession.startLoopResponseReceivedMillis
            )
            Timber.d("SDT Creating new session ${newSession.localMeasurementId}as previous was already registered and had fences")
            newSession
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
        coverageMeasurementSettings.onStartMeasurementSession(measurement.localMeasurementId, measurement.localLoopId)
    }

    private fun handleMeasurementReady(session: CoverageMeasurementSession) {
        scope.launch {
            _sessionEvents.emit(CoverageMeasurementEvent.MeasurementCreated(session))
        }

        // Cancel any still-running retry loop (e.g. from a session that got superseded by a network change)
        registrationJob?.cancel()
        registrationJob = null

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
        val deadlineElapsedMs = SystemClock.elapsedRealtime() + REGISTRATION_TIMEOUT_MS
        var attempt = 0

        while (SystemClock.elapsedRealtime() < deadlineElapsedMs) {
            coroutineContext.ensureActive()

            // Only spend an attempt when there is actual connectivity; otherwise just wait for it.
            if (isNetworkAvailable()) {
                attempt++
                try {
                    Timber.d("Registering coverage measurement (attempt $attempt): ${session.localMeasurementId}")
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
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Coverage registration attempt $attempt failed, will retry in ${RETRY_INTERVAL_MS}ms")
                    _sessionEvents.emit(
                        CoverageMeasurementEvent.MeasurementRegistrationRetrying(
                            session = session,
                            attempt = attempt,
                            maxAttempts = -1, // time-budget based, not a fixed attempt count
                            delayMs = RETRY_INTERVAL_MS
                        )
                    )
                }
            } else {
                Timber.d("Skipping coverage registration - no network connectivity, will re-check in ${RETRY_INTERVAL_MS}ms")
            }

            delay(RETRY_INTERVAL_MS) // automatically cancels if job is cancelled
        }

        // Time budget exhausted without registering: give up with a dedicated no-connectivity timeout
        // so the UI can show a clear message (and not a generic "unknown" error).
        _sessionEvents.emit(
            CoverageMeasurementEvent.MeasurementRegistrationFailed(
                session,
                CoverageRegistrationTimeoutException(registrationTimeoutMinutes)
            )
        )
    }

    /** True when the device currently has a default (internet-capable) network. */
    private fun isNetworkAvailable(): Boolean = connectivityWatcher.network != null
}
