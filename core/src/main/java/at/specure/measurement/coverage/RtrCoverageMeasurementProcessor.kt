package at.specure.measurement.coverage

import androidx.lifecycle.LiveData
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
import at.specure.measurement.coverage.domain.models.CoverageMeasurementData
import at.specure.measurement.coverage.domain.CoverageTimer
import at.specure.measurement.coverage.domain.PingProcessor
import at.specure.measurement.coverage.domain.validators.DurationValidator
import at.specure.measurement.coverage.domain.validators.GpsValidator
import at.specure.measurement.coverage.domain.validators.NetworkValidator
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
    private val coverageGpsValidator: GpsValidator,
    private val coverageNetworkValidator: NetworkValidator,
    private val coverageDurationValidator: DurationValidator,
    private val coveragePingProcessor: PingProcessor
) : CoroutineScope {

    val coverageMeasurementData: MutableLiveData<CoverageMeasurementData?> = MutableLiveData()
    private val _signalPoints: MutableLiveData<List<CoverageMeasurementFenceRecord>> = MutableLiveData()
    val signalPoints: LiveData<List<CoverageMeasurementFenceRecord>> = _signalPoints
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
    private var loadingPointsJob: Job? = null
    override val coroutineContext = EmptyCoroutineContext + coroutineExceptionHandler

    fun initializeDedicatedMeasurementSession(sessionCreated: ((sessionId: String) -> Unit)?, sessionCreationError: ((e: Exception) -> Unit)?) {
        loadingPointsJob?.cancel()
        val lastSignalMeasurementSessionId =
            coverageMeasurementSettings.signalMeasurementLastSessionId
        val shouldContinueInPreviousDedicatedMeasurement =
            coverageMeasurementSettings.signalMeasurementShouldContinueInLastSession
        val continueInPreviousSession =
            (shouldContinueInPreviousDedicatedMeasurement && lastSignalMeasurementSessionId != null)
        val onSessionReady: (CoverageMeasurementSession) -> Unit = { session ->
            loadingPointsJob = loadPoints(session.sessionId)
            coverageMeasurementData.postValue(
                CoverageMeasurementData(
                    coverageMeasurementSession = session,
                    coverageMeasurementSettings = coverageMeasurementSettings,
                )
            )
            Timber.d("Session created: ${session.sessionId} invoking callback")
            sessionCreated?.invoke(session.sessionId)
            val isSessionRegistered = session.serverSessionId != null
            if (isSessionRegistered.not()) {
                scope.launch(Dispatchers.IO) {
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
                                        onStartAndRegistrationCompleted(registeredSession)
                                    }
                                }
                            }
                    } catch (e: HandledException) {
                        onError(e)
                        sessionCreationError?.invoke(e)
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
            coverageSessionTimer.start(maxCoverageSessionSeconds.seconds, { onMeasurementSessionStop() })
        }
    }

    fun onNewLocation(location: LocationInfo, signalRecord: SignalRecord?, networkInfo: NetworkInfo?) {
        if (networkInfo == null) {
            coverageMeasurementData.postValue(
                coverageMeasurementData.value?.copy(
                    currentNetworkType = null
                )
            )
        } else {
            coverageMeasurementData.postValue(
                coverageMeasurementData.value?.copy(
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
            if (isDistanceToLastSignalPointLocationEnough(location?.toDeviceInfoLocation())) {
                val sessionId = coverageMeasurementData.value?.coverageMeasurementSession?.sessionId
                if (sessionId == null) {
                    Timber.e("Signal measurement Session not initialized yet - sessionId missing")
                }
                sessionId?.let { sessionIdLocal ->
                    Timber.d("Creating a new point with signal record: $signalRecord")
                    val lastPoint = coverageMeasurementData.value?.points?.lastOrNull()
                    updateSignalFenceAndSaveOnLeaving(lastPoint)
                    createSignalFence(sessionIdLocal, location, signalRecord)
                }
            } else if (isTheSameLocation(location.toDeviceInfoLocation())) { // todo verify what to do on the same location and what values needs to be replaced - how to replace ping, ...
                val lastPoint = coverageMeasurementData.value?.points?.lastOrNull()
                lastPoint?.let { point ->
                    replaceSignalFenceAndSave(point, signalRecord)
                }
            }
        }
    }

    private fun createSignalFence(sessionId: String, location: LocationInfo, signalRecord: SignalRecord?) {
        val point = CoverageMeasurementFenceRecord(
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

    // TODO: Take network info when leaving the point - possible problem with changing the network type on map when created and when leaving
    private fun updateSignalFenceAndSaveOnLeaving(
        lastPoint: CoverageMeasurementFenceRecord?,
    ) = io {
        val updatedPoint = lastPoint?.copy(
            leaveTimestampMillis = System.currentTimeMillis(),
            avgPingMillis = coveragePingProcessor.onNewFenceStarted()?.average
        )
        updatedPoint?.let {
            signalMeasurementRepository.updateSignalMeasurementPoint(updatedPoint)
        }
    }

    private fun saveDedicatedSignalMeasurementPoint(point: CoverageMeasurementFenceRecord) = io {
        signalMeasurementRepository.saveMeasurementPointRecord(point)
    }

    private fun isTheSameLocation(location: DeviceInfo.Location?): Boolean {
        return coverageGpsValidator.isTheSameLocation(
            newLocation = location,
            lastSavedLocation = coverageMeasurementData.value?.points?.lastOrNull()?.location
        )
    }

    private fun isDistanceToLastSignalPointLocationEnough(location: DeviceInfo.Location?): Boolean {
        return coverageGpsValidator.isLocationDistantEnough(
            newLocation = location,
            lastSavedLocation = coverageMeasurementData.value?.points?.lastOrNull()?.location
        )
    }

    private fun getNextSequenceNumber(): Int {
        val lastPointNumber =
            (coverageMeasurementData.value?.points?.lastOrNull()?.sequenceNumber ?: -1)
        val nextPointNumber = lastPointNumber + 1
        return nextPointNumber
    }

    /**
     * Stop of single measurement in a loop
     */
    fun onMeasurementStop() {
        // todo
    }

    /**
     * Stop of whole measurement loop aka session
     */
    fun onMeasurementSessionStop() {
        this.launch(CoroutineName("OnDedicatedSignalMeasurementStop")) {
            try {
                coveragePingProcessor.stopPing()
                val lastPoint = coverageMeasurementData.value?.points?.lastOrNull()
                updateSignalFenceAndSaveOnLeaving(lastPoint)
            } finally {
                signalMeasurementRepository.sendFences(
                    coverageMeasurementData.value?.coverageMeasurementSession?.sessionId ?: "",
                    coverageMeasurementData.value?.points ?: emptyList()
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
            coverageMeasurementData.postValue( coverageMeasurementData.value?.copy(
                points = loadedPoints
            ))
            _signalPoints.postValue(loadedPoints)
            Timber.d("New points loaded ${loadedPoints.size}")
        }
    }

    private fun loadDedicatedMeasurementSession(
        sessionId: String, onSessionReadyCallback: (CoverageMeasurementSession) -> Unit
    ) = launch(CoroutineName("loadDedicatedMeasurementSession")) {
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

    private fun createNewDedicatedMeasurementSession(onSessionReadyCallback: (CoverageMeasurementSession) -> Unit) =
        launch(CoroutineName("createNewDedicatedMeasurementSession")) {
            val session = CoverageMeasurementSession()
            signalMeasurementRepository.saveDedicatedMeasurementSession(session)
            Timber.d("Newly created session id: ${session.sessionId}")
            coverageMeasurementSettings.signalMeasurementLastSessionId = session.sessionId
            onSessionReadyCallback(session)
        }

    private fun cleanData() {
        loadingPointsJob?.cancel()
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