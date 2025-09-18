package at.specure.measurement.signal

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.specure.client.PingClientConfiguration
import at.specure.client.UdpHmacPingFlow
import at.specure.config.Config
import at.specure.data.SignalMeasurementSettings
import at.specure.data.entity.SignalMeasurementFenceRecord
import at.specure.data.entity.SignalMeasurementSession
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.eval.PingEvaluator
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.NetworkInfo
import at.specure.location.LocationInfo
import at.specure.location.isAccuracyEnoughForSignalMeasurement
import at.specure.test.toDeviceInfoLocation
import at.specure.test.toLocation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val SAME_LOCATION_DISTANCE_METERS = 3
private const val PING_INTERVAL_MILLIS: Long = 100
private const val PING_TIMEOUT_MILLIS: Long = 2000
private const val PING_PROTOCOL_HEADER: String = "RP01"
private const val PING_PROTOCOL_SUCCESS_RESPONSE_HEADER: String = "RR01"
private const val PING_PROTOCOL_ERROR_RESPONSE_HEADER: String = "RE01"

// TODO: resolve problems with signal uuids and coverage uuids, send coverage results, new coverage request on network change and response
// TODO: make own signal listener because now we do not get null signals on signal loss from SignalMeasurementProcessor

@Singleton
class DedicatedSignalMeasurementProcessor @Inject constructor(
    private val signalMeasurementSettings: SignalMeasurementSettings,
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val config: Config,
) : CoroutineScope {

    private val _signalPoints: MutableLiveData<List<SignalMeasurementFenceRecord>> = MutableLiveData()
    val dedicatedSignalMeasurementData: MutableLiveData<DedicatedSignalMeasurementData?> = MutableLiveData()
    val signalPoints: LiveData<List<SignalMeasurementFenceRecord>> = _signalPoints
    private var pingEvaluator: PingEvaluator? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var maxCoverageMeasurementSecondsReachedJob: Job? = null
    private var maxCoverageSessionSecondsReachedJob: Job? = null

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        if (e is HandledException) {
            // do nothing
        } else {
            throw e
        }
    }

    private var loadingPointsJob: Job? = null
    override val coroutineContext = EmptyCoroutineContext + coroutineExceptionHandler

    val currentSessionId: String?
        get() = dedicatedSignalMeasurementData.value?.signalMeasurementSession?.sessionId

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
            dedicatedSignalMeasurementData.postValue(
                DedicatedSignalMeasurementData(
                            signalMeasurementSession = session,
                            signalMeasurementSettings = signalMeasurementSettings,
                        )
            )
            Timber.d("Session created: ${session.sessionId} invoking callback")
            sessionCreated?.invoke(session.sessionId)
            val isSessionRegistered = session.serverSessionId != null
            if (isSessionRegistered.not()) {
                scope.launch(Dispatchers.IO) {
                    val isRegistered = signalMeasurementRepository.registerCoverageMeasurement(coverageSessionId = session.sessionId, measurementId = null).collect { isRegistered ->
                        if (isRegistered) {
                            val registeredSession = signalMeasurementRepository.getDedicatedMeasurementSession(
                                session.sessionId
                            )
                            // TODO: implement retry mechanism when it was not able to register session
                            Timber.d("Starting ping client after registration a new measurement with session: $registeredSession")
                            registeredSession?.let {
                                onStartAndRegistrationCompleted(registeredSession)
                            }
                        }
                    }
                }
            } else {
                Timber.d("Starting ping client as continue from previous")
                CoroutineScope(Dispatchers.IO).launch(Dispatchers.IO) {
                    onStartAndRegistrationCompleted(session)

                }
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

    private suspend fun onStartAndRegistrationCompleted(registeredAndStartedSession: SignalMeasurementSession) {
        startPingClient(registeredAndStartedSession)
        startMaxCoverageMeasurementSecondsReachedJob(session = registeredAndStartedSession)
        startMaxCoverageSessionSecondsReachedJob(session = registeredAndStartedSession)
    }

    private fun startMaxCoverageMeasurementSecondsReachedJob(session: SignalMeasurementSession) {
        maxCoverageMeasurementSecondsReachedJob?.cancel()
        maxCoverageMeasurementSecondsReachedJob = CoroutineScope(Dispatchers.Default).launch {
            session.maxCoverageMeasurementSeconds?.let { maxCoverageMeasurementSeconds ->
                launch {
                    delay( maxCoverageMeasurementSeconds.seconds) // todo: alter time as session begin one time but response was another time
                    onMeasurementStop() // todo check if this is correct logic to happen
                    cancel("MaxCoverageMeasurementSeconds elapsed")
                }
            }
        }
    }

    private fun startMaxCoverageSessionSecondsReachedJob(session: SignalMeasurementSession) {
        maxCoverageSessionSecondsReachedJob?.cancel()
        maxCoverageSessionSecondsReachedJob = CoroutineScope(Dispatchers.Default).launch {
            session.maxCoverageSessionSeconds?.let { maxCoverageSessionSeconds ->
                launch {
                    delay( maxCoverageSessionSeconds.seconds) // todo: alter time as session begin one time but response was another time
                    onMeasurementStop() // todo check if this is correct logic to happen
                    cancel("MaxCoverageSessionSeconds elapsed")
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun startPingClient(signalMeasurementSession: SignalMeasurementSession) {
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
//            val evaluator = PingEvaluator(UdpPingFlow(configuration).pingFlow())
            pingEvaluator = PingEvaluator(UdpHmacPingFlow(configuration).pingFlow())
            pingEvaluator?.start()
                ?.sample(1000)
                ?.collect { pingResult ->
                dedicatedSignalMeasurementData.postValue(
                    dedicatedSignalMeasurementData.value?.copy(
                        currentPingMs = pingResult?.getRTTMillis()
                    )
                )
            }
        }
    }

    fun onNewLocation(location: LocationInfo, signalRecord: SignalRecord?, networkInfo: NetworkInfo?) {
        if (networkInfo == null) {
            dedicatedSignalMeasurementData.postValue(
                dedicatedSignalMeasurementData.value?.copy(
                    currentNetworkType = null
                )
            )
        } else {
            dedicatedSignalMeasurementData.postValue(
                dedicatedSignalMeasurementData.value?.copy(
                    currentNetworkType = when(networkInfo.type) {
                        TransportType.CELLULAR -> (networkInfo as CellNetworkInfo).networkType.displayName
                        TransportType.WIFI,
                        TransportType.BLUETOOTH,
                        TransportType.ETHERNET,
                        TransportType.VPN,
                        TransportType.WIFI_AWARE,
                        TransportType.LOWPAN,
                        TransportType.BROWSER,
                        TransportType.UNKNOWN -> networkInfo.type.name
                    }
                )
            )
        }

        // TODO: check how old is signal information + also handle no signal record in SignalMeasurementProcessor
        // TODO: check if airplane mode is enabled or not, check if mobile data are enabled
        if (signalRecord == null || signalRecord.transportType == TransportType.CELLULAR) {
            if (isDistanceToLastSignalPointLocationEnough(location)) {
                val sessionId = dedicatedSignalMeasurementData.value?.signalMeasurementSession?.sessionId
                if (sessionId == null) {
                    Timber.e("Signal measurement Session not initialized yet - sessionId missing")
                }
                sessionId?.let { sessionIdLocal ->
                    Timber.d("Creating a new point with signal record: $signalRecord")
                    val lastPoint = dedicatedSignalMeasurementData.value?.points?.lastOrNull()
                    updateSignalFenceAndSaveOnLeaving(lastPoint)
                    createSignalFence(sessionIdLocal, location, signalRecord)
                }
            } else if (isTheSameLocation(location)) { // todo verify what to do on the same location and what values needs to be replaced - how to replace ping, ...
                val lastPoint = dedicatedSignalMeasurementData.value?.points?.lastOrNull()
                lastPoint?.let { point ->
                    replaceSignalFenceAndSave(point, signalRecord)
                }
            }
        }
    }

    private fun createSignalFence(sessionId: String, location: LocationInfo, signalRecord: SignalRecord?) {
        val point = SignalMeasurementFenceRecord(
            sessionId = sessionId,
            sequenceNumber = getNextSequenceNumber(),
            location = location.toDeviceInfoLocation(),
            signalRecordId = signalRecord?.signalMeasurementPointId, // todo: because of signal measurement it is removed when chunk is sent
            entryTimestampMillis = System.currentTimeMillis(),
            leaveTimestampMillis = 0,
            radiusMeters = config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement,
            technologyId = signalRecord?.mobileNetworkType?.intValue,
            signalStrength = signalRecord?.lteRsrp, // todo: extract signal value correctly
            avgPingMillis = null,
        )
        saveDedicatedSignalMeasurementPoint(point)
    }

    /**
     * Location is kept from original fence as we could end in the cascade of updates with drifting
     * original location to even few tenth of meters
     */
    private fun replaceSignalFenceAndSave(
        lastPoint: SignalMeasurementFenceRecord?,
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

    // TODO: Take network info when leaving the point - possible problem with changing the network type on map when created and when leaving
    private fun updateSignalFenceAndSaveOnLeaving(
        lastPoint: SignalMeasurementFenceRecord?,
    ) = io {
        val updatedPoint = lastPoint?.copy(
            leaveTimestampMillis = System.currentTimeMillis(),
            avgPingMillis = pingEvaluator?.evaluateAndReset()?.average
        )
        updatedPoint?.let {
            signalMeasurementRepository.updateSignalMeasurementPoint(updatedPoint)
        }
    }

    private fun saveDedicatedSignalMeasurementPoint(point: SignalMeasurementFenceRecord) = io {
        signalMeasurementRepository.saveMeasurementPointRecord(point)
    }

    private fun isTheSameLocation(location: LocationInfo): Boolean {
        if (!location.isAccuracyEnoughForSignalMeasurement()) {
            return false
        }
        val lastPoint = dedicatedSignalMeasurementData.value?.points?.lastOrNull()
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
        val lastPoint = dedicatedSignalMeasurementData.value?.points?.lastOrNull()
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
            (dedicatedSignalMeasurementData.value?.points?.lastOrNull()?.sequenceNumber ?: -1)
        val nextPointNumber = lastPointNumber + 1
        return nextPointNumber
    }

    fun onMeasurementStop() {
        this.launch {
            try {
                pingEvaluator?.evaluateAndStop()
                val lastPoint = dedicatedSignalMeasurementData.value?.points?.lastOrNull()
                updateSignalFenceAndSaveOnLeaving(lastPoint)
            } finally {
                signalMeasurementRepository.sendFences(
                    dedicatedSignalMeasurementData.value?.signalMeasurementSession?.sessionId ?: "",
                    dedicatedSignalMeasurementData.value?.points ?: emptyList()
                )
                cleanData()
            }
        }
    }

    fun onNetworkChanged() {
        // todo: what to do on a new network
    }

    private fun loadPoints(sessionId: String) = scope.launch(Dispatchers.IO) {
        val points =
            signalMeasurementRepository.loadSignalMeasurementPointRecordsForMeasurement(sessionId)
        points.asFlow().flowOn(Dispatchers.IO).collect { loadedPoints ->
            dedicatedSignalMeasurementData.postValue( dedicatedSignalMeasurementData.value?.copy(
                points = loadedPoints
            ))
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
        dedicatedSignalMeasurementData.postValue(null)
        signalMeasurementSettings.signalMeasurementLastSessionId = null
    }
}