package at.specure.measurement.coverage

import at.rmbt.util.exception.HandledException
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.measurement.coverage.domain.CoverageSessionManager
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RtrCoverageSessionManager @Inject constructor(
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
): CoverageSessionManager {

    override fun createSession(
        onSessionCreated: ((CoverageMeasurementSession) -> Unit)?,
        onSessionRegistered: ((CoverageMeasurementSession) -> Unit)?,
        onSessionCreationError: ((Exception) -> Unit)?,
        coroutineScope: CoroutineScope
    ) {
        val lastSignalMeasurementSessionId =
            coverageMeasurementSettings.signalMeasurementLastSessionId
        val shouldContinueInPreviousDedicatedMeasurement =
            coverageMeasurementSettings.signalMeasurementShouldContinueInLastSession
        val continueInPreviousSession =
            (shouldContinueInPreviousDedicatedMeasurement && lastSignalMeasurementSessionId != null)
        val onSessionReady: (CoverageMeasurementSession) -> Unit = { session ->
            onSessionCreated?.invoke(session)
            val isSessionRegistered = session.serverSessionId != null
            if (isSessionRegistered.not()) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val isRegistered = signalMeasurementRepository.registerCoverageMeasurement(coverageSessionId = session.sessionId, measurementId = null)
                            .collect { isRegistered ->
                                if (isRegistered) {
                                    val registeredSession = signalMeasurementRepository.getDedicatedMeasurementSession(
                                        session.sessionId
                                    )
                                    // TODO: implement retry mechanism when it was not able to register session
                                    Timber.d("Starting ping client after registration a new measurement with session: $registeredSession")
                                    registeredSession?.let {
                                        onSessionRegistered?.invoke(registeredSession)
                                    }
                                }
                            }
                    } catch (e: HandledException) {
                        onSessionCreationError?.invoke(e)
                    }
                }
            } else {
                Timber.d("Starting ping client as continue from previous")
                onSessionRegistered?.invoke(session)
            }

        }
        Timber.d("Continue?: $shouldContinueInPreviousDedicatedMeasurement && has id?: $lastSignalMeasurementSessionId")
        if (continueInPreviousSession) {
            Timber.d("Continue in dedicated signal measurement: $lastSignalMeasurementSessionId")
            loadRunningCoverageMeasurementSession(lastSignalMeasurementSessionId!!, onSessionReady, coroutineScope)
        } else {
            Timber.d("Creating new dedicated signal measurement: $lastSignalMeasurementSessionId")
            createNewCoverageMeasurementSession(onSessionReady, coroutineScope)
        }
    }

    override fun endSession(resultsSent: (() -> Unit)?, resultsSendError: ((Exception) -> Unit)?) {
        TODO("Not yet implemented")
    }

    private fun loadRunningCoverageMeasurementSession(
        sessionId: String,
        onSessionReadyCallback: (CoverageMeasurementSession) -> Unit,
        coroutineScope: CoroutineScope,
    ) = coroutineScope.launch(CoroutineName("loadDedicatedMeasurementSession")) {
        val loadedSession = signalMeasurementRepository.getDedicatedMeasurementSession(
            sessionId
        )
        if (loadedSession != null) {
            Timber.d("Loaded session with ID: ${loadedSession.sessionId}")
            onSessionReadyCallback(loadedSession)
        } else {
            createNewCoverageMeasurementSession(onSessionReadyCallback, coroutineScope)
        }
    }

    private fun createNewCoverageMeasurementSession(
        onSessionReadyCallback: (CoverageMeasurementSession) -> Unit,
        coroutineScope: CoroutineScope,
    ) = coroutineScope.launch(CoroutineName("createNewDedicatedMeasurementSession")) {
            val session = CoverageMeasurementSession()
            signalMeasurementRepository.saveDedicatedMeasurementSession(session)
            Timber.d("Newly created session id: ${session.sessionId}")
            coverageMeasurementSettings.signalMeasurementLastSessionId = session.sessionId
            onSessionReadyCallback(session)
        }

}