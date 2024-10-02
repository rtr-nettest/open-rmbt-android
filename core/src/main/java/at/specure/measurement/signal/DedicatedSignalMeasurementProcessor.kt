package at.specure.measurement.signal

import at.rmbt.util.exception.HandledException
import at.specure.data.SignalMeasurementSettings
import at.specure.data.entity.SignalMeasurementSession
import at.specure.data.repository.SignalMeasurementRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

@Singleton
class DedicatedSignalMeasurementProcessor @Inject constructor(
    private val signalMeasurementSettings: SignalMeasurementSettings,
    private val signalMeasurementRepository: SignalMeasurementRepository,
): CoroutineScope {

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        if (e is HandledException) {
            // do nothing
        } else {
            throw e
        }
    }

    override val coroutineContext = EmptyCoroutineContext + coroutineExceptionHandler

    private var dedicatedSignalMeasurementData: DedicatedSignalMeasurementData? = null


    fun initializeDedicatedMeasurementSession() {
        val lastSignalMeasurementSessionId =
            signalMeasurementSettings.signalMeasurementLastSessionId
        val shouldContinueInPreviousDedicatedMeasurement =
            signalMeasurementSettings.signalMeasurementShouldContinueInLastSession
        val continueInPreviousSession =
            (shouldContinueInPreviousDedicatedMeasurement && lastSignalMeasurementSessionId != null)
        val onSessionReady: (SignalMeasurementSession) -> Unit = { session ->
            dedicatedSignalMeasurementData = DedicatedSignalMeasurementData(
                signalMeasurementSession = session,
                signalMeasurementSettings = signalMeasurementSettings,
            )
        }
        Timber.d("Continue?: $shouldContinueInPreviousDedicatedMeasurement && has id?: $lastSignalMeasurementSessionId")
        if (continueInPreviousSession) {
            Timber.d("Continue in dedicated signal measurement: $lastSignalMeasurementSessionId")
            loadDedicatedMeasurementSession(lastSignalMeasurementSessionId!!, onSessionReady)
        } else {
            Timber.d("Creating new dedicated signal measurement: $lastSignalMeasurementSessionId")
            createNewDedicatedMeasurementSession(onSessionReady)
        }
    }

    fun onMeasurementStop() {
        dedicatedSignalMeasurementData = null
        signalMeasurementSettings.signalMeasurementLastSessionId = null
    }

    private fun loadDedicatedMeasurementSession(sessionId: String, onSessionReadyCallback: (SignalMeasurementSession) -> Unit) = launch {
        val loadedSession = signalMeasurementRepository.getDedicatedMeasurementSession(
            sessionId
        )
        if (loadedSession != null) {
            Timber.d("Loaded session with ID: ${loadedSession.sessionId}")
            onSessionReadyCallback(loadedSession)
        } else {
            createNewDedicatedMeasurementSession(onSessionReadyCallback)
        }
    }

    private fun createNewDedicatedMeasurementSession(onSessionReadyCallback: (SignalMeasurementSession) -> Unit) = launch {
        val session = SignalMeasurementSession()
        signalMeasurementRepository.saveDedicatedMeasurementSession(session)
        Timber.d("Newly created session id: ${session.sessionId}")
        signalMeasurementSettings.signalMeasurementLastSessionId = session.sessionId
        onSessionReadyCallback(session)
    }

}