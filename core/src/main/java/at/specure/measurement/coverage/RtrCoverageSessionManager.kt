package at.specure.measurement.coverage

import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.measurement.coverage.domain.CoverageSessionEvent
import at.specure.measurement.coverage.domain.CoverageSessionManager
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
class RtrCoverageSessionManager @Inject constructor(
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
) : CoverageSessionManager {

    private val _sessionEvents = MutableSharedFlow<CoverageSessionEvent>()
    override fun sessionFlow(): SharedFlow<CoverageSessionEvent> = _sessionEvents

    private var registrationJob: Job? = null

    private val MAX_RETRY = 100 // todo: adjust according the needs, maybe change to indefinitely
    private val RETRY_DELAY_MS = 2_000L

    override fun createSession(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            _sessionEvents.emit(CoverageSessionEvent.SessionInitializing)
        }
        val lastSessionId = coverageMeasurementSettings.signalMeasurementLastSessionId
        val shouldContinue = coverageMeasurementSettings.signalMeasurementShouldContinueInLastSession
        val continuePrevious = (shouldContinue && lastSessionId != null)

        if (continuePrevious) {
            loadExistingSession(lastSessionId!!, coroutineScope)
        } else {
            createNewSession(coroutineScope)
        }
    }

    override suspend fun endSession() {
        registrationJob?.cancel()
        registrationJob = null

        _sessionEvents.emit(CoverageSessionEvent.SessionEnded)
    }

    private fun loadExistingSession(
        sessionId: String,
        coroutineScope: CoroutineScope
    ) = coroutineScope.launch(CoroutineName("loadSession")) {

        Timber.d("Continue in coverage measurement: $sessionId")

        val loaded = signalMeasurementRepository.getDedicatedMeasurementSession(sessionId)

        if (loaded != null) {
            handleSessionReady(loaded, coroutineScope)
        } else {
            createNewSession(coroutineScope)
        }
    }

    private fun createNewSession(
        coroutineScope: CoroutineScope
    ) = coroutineScope.launch(CoroutineName("createNewSession")) {

        val session = CoverageMeasurementSession()
        Timber.d("Creating new coverage measurement: ${session.sessionId}")
        signalMeasurementRepository.saveDedicatedMeasurementSession(session)

        coverageMeasurementSettings.signalMeasurementLastSessionId = session.sessionId

        handleSessionReady(session, coroutineScope)
    }

    private fun handleSessionReady(
        session: CoverageMeasurementSession,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch {
            _sessionEvents.emit(CoverageSessionEvent.SessionCreated(session))
        }

        // Already registered → resume
        if (session.serverSessionId != null) {
            coroutineScope.launch {
                _sessionEvents.emit(CoverageSessionEvent.SessionRegistered(session))
            }
            return
        }

        // Not registered → start retry loop
        registrationJob = coroutineScope.launch(Dispatchers.IO + CoroutineName("registerSession")) {
            registerSessionWithRetry(session)
        }
    }

    private suspend fun registerSessionWithRetry(
        session: CoverageMeasurementSession
    ) {
        var attempt = 1

        while (attempt <= MAX_RETRY) {

            coroutineContext.ensureActive()

            try {
                val ok = signalMeasurementRepository
                    .registerCoverageMeasurement(session.sessionId, null)
                    .first()

                if (ok) {
                    val registered =
                        signalMeasurementRepository.getDedicatedMeasurementSession(session.sessionId)

                    _sessionEvents.emit(
                        CoverageSessionEvent.SessionRegistered(registered!!)
                    )

                    return
                }

            } catch (e: Exception) {

                if (attempt >= MAX_RETRY) {
                    _sessionEvents.emit(
                        CoverageSessionEvent.SessionRegistrationFailed(session, e)
                    )
                    return
                }

                _sessionEvents.emit(
                    CoverageSessionEvent.SessionRegistrationRetrying(
                        session = session,
                        attempt = attempt,
                        maxAttempts = MAX_RETRY,
                        delayMs = RETRY_DELAY_MS
                    )
                )
            }

            delay(RETRY_DELAY_MS)  // cancels automatically when Job is cancelled
            attempt++
        }
    }
}
