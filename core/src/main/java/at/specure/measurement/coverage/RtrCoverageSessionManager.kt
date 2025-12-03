package at.specure.measurement.coverage

import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.measurement.coverage.domain.CoverageMeasurementEvent
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

    private val _sessionEvents = MutableSharedFlow<CoverageMeasurementEvent>()
    override fun sessionFlow(): SharedFlow<CoverageMeasurementEvent> = _sessionEvents

    private var registrationJob: Job? = null

    private val MAX_RETRY = 100 // todo: adjust according the needs, maybe change to indefinitely
    private val RETRY_DELAY_MS = 2_000L

    override fun createMeasurement(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            _sessionEvents.emit(CoverageMeasurementEvent.MeasurementInitializing)
        }
        val lastSessionId = coverageMeasurementSettings.signalMeasurementLastSessionId
        val shouldContinue = coverageMeasurementSettings.signalMeasurementShouldContinueInLastSession
        val continuePrevious = (shouldContinue && lastSessionId != null)

        if (continuePrevious) {
            loadExistingMeasurement(lastSessionId!!, coroutineScope)
        } else {
            createNewMeasurement(coroutineScope)
        }
    }

    override suspend fun endMeasurement() {
        registrationJob?.cancel()
        registrationJob = null

        _sessionEvents.emit(CoverageMeasurementEvent.MeasurementEnded)
    }

    private fun loadExistingMeasurement(
        measurementId: String,
        coroutineScope: CoroutineScope
    ) = coroutineScope.launch(CoroutineName("loadSession")) {

        Timber.d("Continue in coverage measurement: $measurementId")

        val loaded = signalMeasurementRepository.getCoverageMeasurementSession(measurementId)

        if (loaded != null) {
            handleMeasurementReady(loaded, coroutineScope)
        } else {
            createNewMeasurement(coroutineScope)
        }
    }

    private fun createNewMeasurement(
        coroutineScope: CoroutineScope
    ) = coroutineScope.launch(CoroutineName("createNewSession")) {

        val session = CoverageMeasurementSession()
        Timber.d("Creating new coverage measurement: ${session.localMeasurementId}")
        signalMeasurementRepository.saveCoverageMeasurementSession(session)

        coverageMeasurementSettings.signalMeasurementLastSessionId = session.localMeasurementId

        handleMeasurementReady(session, coroutineScope)
    }

    private fun handleMeasurementReady(
        session: CoverageMeasurementSession,
        coroutineScope: CoroutineScope
    ) {
        coroutineScope.launch {
            _sessionEvents.emit(CoverageMeasurementEvent.MeasurementCreated(session))
        }

        // Already registered → resume
        if (session.serverMeasurementId != null) {
            coroutineScope.launch {
                _sessionEvents.emit(CoverageMeasurementEvent.MeasurementRegistered(session))
            }
            return
        }

        // Not registered → start retry loop
        registrationJob = coroutineScope.launch(Dispatchers.IO + CoroutineName("registerSession")) {
            registerMeasurementWithRetry(session)
        }
    }

    private suspend fun registerMeasurementWithRetry(
        session: CoverageMeasurementSession
    ) {
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

            delay(RETRY_DELAY_MS)  // cancels automatically when Job is cancelled
            attempt++
        }
    }
}
