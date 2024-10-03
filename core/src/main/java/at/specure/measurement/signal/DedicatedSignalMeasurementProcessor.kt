package at.specure.measurement.signal

import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.SignalMeasurementSettings
import at.specure.data.entity.SignalMeasurementPointRecord
import at.specure.data.entity.SignalMeasurementSession
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.info.TransportType
import at.specure.location.LocationInfo
import at.specure.location.isAccuracyEnoughForSignalMeasurement
import at.specure.test.toDeviceInfoLocation
import at.specure.test.toLocation
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
    private val config: Config,
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

    fun onNewLocation(location: LocationInfo, signalRecord: SignalRecord?) {
        // TODO: check how old is signal information
        if (signalRecord?.transportType == TransportType.CELLULAR) {
            if (isDistanceToLastSignalPointLocationEnough(location)) {
                val sessionId = dedicatedSignalMeasurementData?.signalMeasurementSession?.sessionId
                    ?: throw Exception("Session not initialized - sessionId missing")
                Timber.d("Creating point with signal record: $signalRecord")
                val point = SignalMeasurementPointRecord(
                    sessionId = sessionId,
                    sequenceNumber = getNextSequenceNumber(),
                    location = location.toDeviceInfoLocation(),
                    signalRecordId = signalRecord.signalMeasurementPointId
                )
                saveDedicatedSignalMeasurementPoint(point)
            }
        }
    }

    private fun saveDedicatedSignalMeasurementPoint(point: SignalMeasurementPointRecord) = io {
        signalMeasurementRepository.saveMeasurementPointRecord(point)
    }

    private fun isDistanceToLastSignalPointLocationEnough(location: LocationInfo): Boolean {
        if (!location.isAccuracyEnoughForSignalMeasurement()) {
            Timber.d("Accuracy is not enough")
            return false
        }
        val minDistance = config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement
        val lastPoint = dedicatedSignalMeasurementData?.points?.lastOrNull()
        val lastLocation = lastPoint?.location
        return if (lastLocation != null) {
            val distance = location.toLocation().distanceTo(lastLocation.toLocation())
            Timber.d("Distance is: $distance")
            (distance >= minDistance)
        } else {
            Timber.d("Distance is good because no previous point loaded")
            true
        }
    }

    fun getNextSequenceNumber(): Int {
        val lastPointNumber = (dedicatedSignalMeasurementData?.points?.lastOrNull()?.sequenceNumber ?: -1)
        val nextPointNumber = lastPointNumber + 1
        return nextPointNumber
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