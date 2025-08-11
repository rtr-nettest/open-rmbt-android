package at.specure.measurement.signal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.specure.client.PingClientConfiguration
import at.specure.client.UdpPingFlow
import at.specure.config.Config
import at.specure.data.SignalMeasurementSettings
import at.specure.data.entity.SignalMeasurementPointRecord
import at.specure.data.entity.SignalMeasurementSession
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.eval.PingEvaluator
import at.specure.info.TransportType
import at.specure.location.LocationInfo
import at.specure.location.isAccuracyEnoughForSignalMeasurement
import at.specure.test.toDeviceInfoLocation
import at.specure.test.toLocation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

private const val SAME_LOCATION_DISTANCE_METERS = 3
private const val PING_INTERVAL_MILLIS: Long = 100
private const val PING_TIMEOUT_MILLIS: Long = 2000
private const val PING_PROTOCOL_HEADER: String = "RP01"
private const val PING_PROTOCOL_SUCCESS_RESPONSE_HEADER: String = "RR01"
private const val PING_PROTOCOL_ERROR_RESPONSE_HEADER: String = "RE01"

@Singleton
class DedicatedSignalMeasurementProcessor @Inject constructor(
    private val signalMeasurementSettings: SignalMeasurementSettings,
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val config: Config,
) : CoroutineScope {

    private val _signalPoints: MutableLiveData<List<SignalMeasurementPointRecord>> = MutableLiveData()
    val signalPoints: LiveData<List<SignalMeasurementPointRecord>> = _signalPoints

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        if (e is HandledException) {
            // do nothing
        } else {
            throw e
        }
    }

    private var loadingPointsJob: Job? = null
    override val coroutineContext = EmptyCoroutineContext + coroutineExceptionHandler
    private var dedicatedSignalMeasurementData: DedicatedSignalMeasurementData? = null

    val currentSessionId: String?
        get() = dedicatedSignalMeasurementData?.signalMeasurementSession?.sessionId

    fun initializeDedicatedMeasurementSession(sessionCreated: ((sessionId: String) -> Unit)?) {
        loadingPointsJob?.cancel()
        val lastSignalMeasurementSessionId =
            signalMeasurementSettings.signalMeasurementLastSessionId
        val shouldContinueInPreviousDedicatedMeasurement =
            signalMeasurementSettings.signalMeasurementShouldContinueInLastSession
        val continueInPreviousSession =
            (shouldContinueInPreviousDedicatedMeasurement && lastSignalMeasurementSessionId != null)
        val onSessionReady: (SignalMeasurementSession) -> Unit = { session ->
            loadingPointsJob = loadPoints(session.sessionId)
            dedicatedSignalMeasurementData = DedicatedSignalMeasurementData(
                signalMeasurementSession = session,
                signalMeasurementSettings = signalMeasurementSettings,
            )
            Timber.d("Session created: ${session.sessionId} invoking callback")
            sessionCreated?.invoke(session.sessionId)
            val isSessionRegistered = session.serverSessionId != null
            if (isSessionRegistered.not()) {
                val pingJob = CoroutineScope(Dispatchers.IO).launch {
                    val isRegistered = signalMeasurementRepository.registerCoverageMeasurement(coverageSessionId = session.sessionId).firstOrNull()
                    if (isRegistered == true) {
                        Timber.d("Starting ping client after registration a new measurement")
                        startPingClient(session)
                    }
                    joinAll()
                }
            } else {
                Timber.d("Starting ping client as continue from previous")
                startPingClient(session)
            }

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

    private fun startPingClient(signalMeasurementSession: SignalMeasurementSession) {
        val pingHost = signalMeasurementSession.pingServerHost
        val pingPort = signalMeasurementSession.pingServerPort
        val pingToken = signalMeasurementSession.pingServerToken

        if (pingHost != null && pingPort != null && pingToken != null) {

            val configuration = PingClientConfiguration(
                host = pingHost,
                port = pingPort,
                token = pingToken,
                protocolId = PING_PROTOCOL_HEADER,
                pingIntervalMillis = PING_INTERVAL_MILLIS,
                pingTimeoutMillis = PING_TIMEOUT_MILLIS,
                successResponseHeader = PING_PROTOCOL_SUCCESS_RESPONSE_HEADER,
                errorResponseHeader = PING_PROTOCOL_ERROR_RESPONSE_HEADER
            )
            Timber.d("Starting ping client: $configuration")
            val evaluator = PingEvaluator(UdpPingFlow(configuration).pingFlow())
            evaluator.start()
        }
    }

    fun onNewLocation(location: LocationInfo, signalRecord: SignalRecord?) {
        // TODO: check how old is signal information + also handle no signal record in SignalMeasurementProcessor
        // TODO: check if airplane mode is enabled or not, check if mobile data are enabled
        if (signalRecord == null || signalRecord.transportType == TransportType.CELLULAR) {
            if (isDistanceToLastSignalPointLocationEnough(location)) {
                val sessionId = dedicatedSignalMeasurementData?.signalMeasurementSession?.sessionId
                if (sessionId == null) {
                    Timber.e("Signal measurement Session not initialized yet - sessionId missing")
                }
                sessionId?.let {
                    Timber.d("Creating point with signal record: $signalRecord")
                    val point = SignalMeasurementPointRecord(
                        sessionId = sessionId,
                        sequenceNumber = getNextSequenceNumber(),
                        location = location.toDeviceInfoLocation(),
                        signalRecordId = signalRecord?.signalMeasurementPointId
                    )
                    saveDedicatedSignalMeasurementPoint(point)
                }
            } else if (isTheSameLocation(location)) {
                val lastPoint = dedicatedSignalMeasurementData?.points?.lastOrNull()
                lastPoint?.let { point ->
                    updatePointAndSave(point, signalRecord)
                }
            }
        }
    }

    /**
     * Location is kept from original point as we could end in the cascade of updates with drifting
     * original location to even few tenth of meters
     */
    private fun updatePointAndSave(
        lastPoint: SignalMeasurementPointRecord?,
        signalRecord: SignalRecord?
    ) = io {
        val updatedPoint = lastPoint?.copy(
            signalRecordId = signalRecord?.signalMeasurementPointId,
            timestamp = System.currentTimeMillis()
        )
        updatedPoint?.let {
            signalMeasurementRepository.updateSignalMeasurementPoint(updatedPoint)
        }
    }

    private fun saveDedicatedSignalMeasurementPoint(point: SignalMeasurementPointRecord) = io {
        signalMeasurementRepository.saveMeasurementPointRecord(point)
    }

    private fun isTheSameLocation(location: LocationInfo): Boolean {
        if (!location.isAccuracyEnoughForSignalMeasurement()) {
            return false
        }
        val lastPoint = dedicatedSignalMeasurementData?.points?.lastOrNull()
        val lastLocation = lastPoint?.location
        return if (lastLocation != null) {
            val distance = location.toLocation().distanceTo(lastLocation.toLocation())
            (distance < SAME_LOCATION_DISTANCE_METERS)
        } else {
            false
        }
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

    private fun getNextSequenceNumber(): Int {
        val lastPointNumber =
            (dedicatedSignalMeasurementData?.points?.lastOrNull()?.sequenceNumber ?: -1)
        val nextPointNumber = lastPointNumber + 1
        return nextPointNumber
    }

    fun onMeasurementStop() {
        Timber.d("On Measurement Stop called")
        // todo: send points
        cleanData()
    }

    fun onNetworkChanged() {
        // todo: what to do on a new network
    }

    private fun loadPoints(sessionId: String) = launch {
        val points =
            signalMeasurementRepository.loadSignalMeasurementPointRecordsForMeasurement(sessionId)
        points.asFlow().flowOn(Dispatchers.IO).collect { loadedPoints ->
            dedicatedSignalMeasurementData = dedicatedSignalMeasurementData?.copy(
                points = loadedPoints
            )
            _signalPoints.postValue(loadedPoints)
            Timber.d("New points loaded ${loadedPoints.size}")
        }
    }

    private fun loadDedicatedMeasurementSession(
        sessionId: String, onSessionReadyCallback: (SignalMeasurementSession) -> Unit
    ) = launch {
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

    private fun createNewDedicatedMeasurementSession(onSessionReadyCallback: (SignalMeasurementSession) -> Unit) =
        launch {
            val session = SignalMeasurementSession()
            signalMeasurementRepository.saveDedicatedMeasurementSession(session)
            Timber.d("Newly created session id: ${session.sessionId}")
            signalMeasurementSettings.signalMeasurementLastSessionId = session.sessionId
            onSessionReadyCallback(session)
        }

    private fun cleanData() {
        loadingPointsJob?.cancel()
        dedicatedSignalMeasurementData = null
        signalMeasurementSettings.signalMeasurementLastSessionId = null
    }
}