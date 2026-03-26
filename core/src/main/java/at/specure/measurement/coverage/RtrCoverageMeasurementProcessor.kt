package at.specure.measurement.coverage

import android.content.Context
import androidx.lifecycle.asFlow
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.specure.client.PingServerException
import at.specure.config.Config
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.MeasurementRepository
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.ip.IpChangeWatcher
import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.network.NetworkInfo
import at.specure.location.LocationDistanceAndSpeedCounter
import at.specure.location.LocationInfo
import at.specure.measurement.coverage.data.FencesDataSource
import at.specure.measurement.coverage.domain.CoverageMeasurementProcessor
import at.specure.measurement.coverage.domain.CoverageMeasurementEvent
import at.specure.measurement.coverage.domain.CoverageLoopManager
import at.specure.measurement.coverage.domain.CoverageTimer
import at.specure.measurement.coverage.domain.PingProcessor
import at.specure.measurement.coverage.domain.models.CoverageMeasurementTerminationCause
import at.specure.measurement.coverage.domain.models.state.CoverageMeasurementState
import at.specure.measurement.coverage.domain.monitors.ConnectivityMonitor
import at.specure.measurement.coverage.domain.validators.CoverageDataValidator
import at.specure.measurement.coverage.domain.validators.LocationValidator
import at.specure.measurement.coverage.presentation.CoverageMeasurementDataStateManager
import at.specure.measurement.coverage.presentation.monitors.CoverageDataSimMonitor
import at.specure.test.DeviceInfo
import at.specure.test.toDeviceInfoLocation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

// TODO: resolve problems with signal uuids and coverage uuids, send coverage results, new coverage request on network change and response
// TODO: make own signal listener because now we do not get null signals on signal loss from SignalMeasurementProcessor

const val MAXIMUM_FENCES_IN_SINGLE_MEASUREMENT = 400
const val MAXIMUM_LOCATIONS_IN_SINGLE_MEASUREMENT = 700
const val MAXIMUM_SIGNALS_IN_SINGLE_MEASUREMENT = 400
const val MINIMUM_POSSIBLE_FENCE_RADIUS_METERS = 1
const val MAXIMUM_POSSIBLE_FENCE_RADIUS_METERS = 1000

@Singleton
class RtrCoverageMeasurementProcessor @Inject constructor(
    private val appContext: Context,
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val testDataRepository: TestDataRepository,
    private val measurementRepository: MeasurementRepository,
    private val config: Config,
    private val coverageLocationValidator: LocationValidator,
    private val mainCoverageDataValidator: CoverageDataValidator,
    private val coveragePingProcessor: PingProcessor,
    private val fencesDataSource: FencesDataSource,
    private val coverageLoopManager: CoverageLoopManager,
    private val connectivityMonitor: ConnectivityMonitor,
    private val scope: CoroutineScope,
    val stateManager: CoverageMeasurementDataStateManager,
    private val ipChangeWatcher: IpChangeWatcher,
) : CoverageMeasurementProcessor, CoroutineScope {

    private val coverageSessionTimer = CoverageTimer(
        scope = CoroutineScope(Dispatchers.Default + CoroutineName("MaxCoverageSessionTimer")),
    )
    private val coverageMeasurementTimer = CoverageTimer(
        scope = CoroutineScope(Dispatchers.Default + CoroutineName("MaxCoverageMeasurementTimer")),
    )
//    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        if (e is HandledException) {
            // do nothing
        } else {
            throw e
        }
    }
    private var loadingFencesJob: Job? = null
    private var sessionCollectorJob: Job? = null
    private var pingJob: Job? = null
    private var processingLocationsJob: Job? = null
    override val coroutineContext = EmptyCoroutineContext + coroutineExceptionHandler
//    val stateManager = CoverageMeasurementDataStateManager(coverageMeasurementSettings, scope)
    val dataSimMonitor = CoverageDataSimMonitor(scope = scope)
    var dataSimMonitorJob: Job? = null
    private val locationUpdatesFlow = MutableSharedFlow<Triple<LocationInfo?, DetailedNetworkInfo?, Float?>>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun startCoverageSession(
        sessionCreated: ((CoverageMeasurementSession) -> Unit)?,
        sessionCreationError: ((Exception) -> Unit)?,
        sessionStopped: (() -> Unit)?,
    ) {
        processingLocationsJob?.cancel(kotlinx.coroutines.CancellationException("New measurement started"))
        processingLocationsJob = scope.launch(Dispatchers.IO + CoroutineName("LocationFlowProcessor")) {
            locationUpdatesFlow
                .conflate()
                .collect { (location, networkInfo, currentTemperature) ->
                    processLocation(location, networkInfo, currentTemperature)
                }
        }
        connectivityMonitor.start(
            onAirplaneEnabled = {
                Timber.d("✈️ Airplane mode changed to ENABLED → stopping coverage session")
                stopCoverageSession(CoverageMeasurementTerminationCause.EndedByAirplaneModeEnabled())
            },
            onAirplaneDisabled = {
                Timber.d("📶 Airplane mode changed to DISABLED → resuming session")
                resumeCoverageSession()
            },
            onMobileDataEnabled =  {
                Timber.d("📶 Mobile data changed to ENABLED → resuming session")
                resumeCoverageSession()
            },
            onMobileDataDisabled = {
                Timber.d("📶 Mobile data changed to DISABLED → stopping coverage session")
                stopCoverageSession(CoverageMeasurementTerminationCause.EndedByMobileDataDisabled())
            },
            onIpAddressChanged = {
                Timber.d("🌐 IP address changed to $it → stopping measurement")
//                handled by onNetworkChanged()
//                onMeasurementStop()
            },
        )
        dataSimMonitorJob = scope.launch() {
            dataSimMonitor.activeDataSim
                .onEach { Timber.d("data sim subsId changed to: $it") }
                .drop(1)
                .debounce { 1_000 }
                .collect { subscriptionId ->
                    Timber.d("📶 Active data sim changed to subId: $subscriptionId -> stopping measurement")
                    onMeasurementStopAndStartNewMeasurement(CoverageMeasurementTerminationCause.EndedByActiveSimChange())
            }
        }

        dataSimMonitor.start()

        stateManager.initData()
        coverageLoopManager.startOrContinueInLoop()

        if (sessionCollectorJob == null) {
            sessionCollectorJob = scope.launch {
                try {
                    coverageLoopManager.loopFlow().collect { event ->
                        when (event) {

                            is CoverageMeasurementEvent.MeasurementInitializing -> {
                                // optional: show loading UI
                            }

                            is CoverageMeasurementEvent.MeasurementCreated -> {
                                Timber.d("Session created with id: ${event.session.localMeasurementId} seq: ${event.session.sequenceNumber}")
                                val session = event.session
                                val localMeasurementId = event.session.localMeasurementId
                                stateManager.onSessionCreated(session)
                                measurementRepository.saveCapabilities(localMeasurementId, null)
                                measurementRepository.saveTelephonyInfo(localMeasurementId)
                                measurementRepository.savePermissionsStatus(localMeasurementId, null)
                                sessionCreated?.invoke(session)
                                loadingFencesJob?.cancel()
                                loadingFencesJob = loadPoints(session.localLoopId)
                                Timber.d("SDT Updating main state with session id: ${event.session.localMeasurementId} and server: ${event.session.serverMeasurementId}")
                            }

                            is CoverageMeasurementEvent.MeasurementRegistered -> {
                                Timber.d("SDT Session created or continue with id: ${event.session.localMeasurementId} and server: ${event.session.serverMeasurementId}")
                                onStartAndRegistrationCompleted(event.session)
                            }

                            is CoverageMeasurementEvent.MeasurementRegistrationRetrying -> {
                                // optional: show retry attempt info in UI
                            }

                            is CoverageMeasurementEvent.MeasurementRegistrationFailed -> {
                                onError(event.error ?: Exception("Unknown registration failure"))
                                sessionCreationError?.invoke(event.error ?: Exception("Unknown registration failure"))
                            }

                            is CoverageMeasurementEvent.MeasurementCreationError -> {
                                onError(event.error)
                                sessionCreationError?.invoke(event.error)
                            }

                            CoverageMeasurementEvent.MeasurementLoopEnded -> {
                                sessionStopped?.invoke()
                                sessionCollectorJob?.cancel()
                                sessionCollectorJob = null
                                cancelPingJob()
                            }

                            CoverageMeasurementEvent.MeasurementEnded -> {
                                // todo:check if there is anything necessary to do
                            }
                        }
                    }
                } finally {
                    sessionCollectorJob = null
                }
            }
        }
    }

    override fun stopCoverageSession(reasonToTerminate: CoverageMeasurementTerminationCause) {
        if (stateManager.state.value.state != CoverageMeasurementState.FINISHED_LOOP_CORRECTLY) {
            this.launch(CoroutineName("OnDedicatedSignalMeasurementStop")) {
                try {
                    updateLastFenceOnLeaving()
                    val data = stateManager.state.value
                    val session = data.coverageMeasurementSession
                    session?.let {
                        signalMeasurementRepository.saveCoverageMeasurementSession(it.copy(
                            reasonToTerminate = reasonToTerminate.cause
                        ))
                    }

                    stateManager.onUpdateCoverageDataState(CoverageMeasurementState.FINISHED_LOOP_CORRECTLY)
                    stateManager.startSendingResults()

                    Timber.d("Sending coverageResult")
                    signalMeasurementRepository.sendFences(
                        data.coverageMeasurementSession?.localMeasurementId ?: "",
                        { sentSuccessfully: Boolean ->
                            stateManager.onSignalResultSent(sentSuccessfully)
                        }
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    stateManager.onSignalResultSent(false)
                } finally {
//                cleanData()
                    coverageMeasurementSettings.onStopMeasurementSession()
                    coverageLoopManager.endMeasurementLoop()
                    dataSimMonitorJob?.cancel()
                    dataSimMonitorJob = null
                    connectivityMonitor.stop()
                }
            }
        }
    }

    private suspend fun updateLastFenceOnLeaving() {
        val sessionId = stateManager.state.value.coverageMeasurementSession?.localMeasurementId
        val avgPingMillis = coveragePingProcessor.stopPing()?.average
        sessionId?.let {
            fencesDataSource.updateSignalFenceAndSaveOnLeaving(
                sessionId,
                leaveTimestampMillis = System.currentTimeMillis(),
                avgPingMillis = avgPingMillis,
                networkInfo = stateManager.state.value.currentNetworkInfo,
                lastFenceMinTechSignal = stateManager.getMinSignalForTechnologyForCurrentFence(stateManager.state.value.currentNetworkInfo)
            )
        } ?: Timber.e("Session id is null - impossible to update last fence")
    }

    override fun pauseCoverageSession() {
        stateManager.onUpdateCoverageDataState(CoverageMeasurementState.PAUSED)
    }

    override fun resumeCoverageSession() {
        val recoveredState = recoverCoverageState()
        stateManager.onUpdateCoverageDataState(recoveredState)
    }

    fun recoverCoverageState(): CoverageMeasurementState {
        val session = stateManager.state.value.coverageMeasurementSession
        val newState = when {
            session == null -> CoverageMeasurementState.IDLE
            session.serverMeasurementId != null -> CoverageMeasurementState.RUNNING
            session.localMeasurementId != null -> CoverageMeasurementState.CREATED
            else -> CoverageMeasurementState.IDLE
         }
        return newState
    }

    override fun getData(): CoverageMeasurementSession {
        TODO("Not yet implemented")
    }

    private suspend fun onStartAndRegistrationCompleted(registeredAndStartedSession: CoverageMeasurementSession) {
        stateManager.onSessionRegistered(registeredAndStartedSession)
        stateManager.onUpdateCoverageDataState(CoverageMeasurementState.RUNNING)
        Timber.d("Starting ping")
        cancelPingJob()
        pingJob = scope.launch(CoroutineName("PingJobCoroutine")) {
            coveragePingProcessor.startPing(registeredAndStartedSession).collect { pingData ->
                if (pingData.error is PingServerException) {
                    onNetworkChanged()
                } else {
                    stateManager.updatePingData(pingData)
                }
            }
        }
        startMaxCoverageMeasurementSecondsReachedJob(session = registeredAndStartedSession)
        startMaxCoverageSessionSecondsReachedJob(session = registeredAndStartedSession)
        Timber.d("Starting cancellation jobs")
    }

    private fun cancelPingJob() {
        pingJob?.cancel()
        pingJob = null
    }

    private fun onError(e: Exception) {
        stateManager.onException(e)
    }

    private fun startMaxCoverageMeasurementSecondsReachedJob(session: CoverageMeasurementSession) {
        session.maxCoverageMeasurementSeconds?.let { maxCoverageMeasurementSeconds ->
            Timber.d("Starting maxCoverageMeasurementSeconds timer with ${maxCoverageMeasurementSeconds.seconds.inWholeSeconds} seconds")
            coverageMeasurementTimer.start(maxCoverageMeasurementSeconds.seconds, {
                this.scope.launch {
                    Timber.d("Stopping coverage measurement because of max time reached")
                    onMeasurementStopAndStartNewMeasurement(CoverageMeasurementTerminationCause.EndedByMeasurementTimeExpired())
                }
            })
        }
    }

    private fun startMaxCoverageSessionSecondsReachedJob(session: CoverageMeasurementSession) {
        session.maxCoverageLoopSeconds?.let { maxCoverageSessionSeconds ->
            if (session.isFirstMeasurementInLoop()) {
                Timber.d("Starting maxCoverageSessionSeconds timer with ${maxCoverageSessionSeconds.seconds.inWholeSeconds} seconds")
                coverageSessionTimer.start(maxCoverageSessionSeconds.seconds, {
                    Timber.d("Stopping coverage session because of max time reached")
                    stopCoverageSession(CoverageMeasurementTerminationCause.EndedByMeasurementLoopTimeExpired())
                })
            }
        }
    }

    private fun computeSpeedAndUpdateFenceRadius(oldLocation: LocationInfo?, currentLocation: LocationInfo?): Int {
        if (oldLocation == null || currentLocation == null) {
            return config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement
        }
        val speedMetersPerSecond = LocationDistanceAndSpeedCounter.getSpeedMetersPerSecond(
            lat1 = oldLocation.latitude,
            lon1 = oldLocation.longitude,
            lat2 = currentLocation.latitude,
            lon2 = currentLocation.longitude,
            timestampMilliseconds1 = oldLocation.time,
            timestampMilliseconds2 = currentLocation.time
        )
        val currentAccuracy = currentLocation.accuracy

        val fenceRadiusFromGps = coverageMeasurementSettings.baseMinimalDistanceBetweenFenceCentersMeters + 2 * currentAccuracy
        val fenceRadiusFromSpeed = speedMetersPerSecond

        val fenceRadius = max(fenceRadiusFromSpeed, fenceRadiusFromGps).toInt()

        if (fenceRadius == 0) {
            Timber.e("fence radius updated: $fenceRadius * factor from $fenceRadiusFromGps And $fenceRadiusFromSpeed")
            return config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement
        }
        Timber.d("fence radius updated: $fenceRadius * factor from $fenceRadiusFromGps And $fenceRadiusFromSpeed")
//        config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement = fenceRadius
//        onCoverageConfigurationChanged()
        return fenceRadius
    }

    fun onNewLocation(location: LocationInfo?, networkInfo: DetailedNetworkInfo?, batteryTemperature: Float?) {
        locationUpdatesFlow.tryEmit(Triple(location, networkInfo, batteryTemperature))
    }

    private suspend fun processLocation(location: LocationInfo?, networkInfo: DetailedNetworkInfo?, currentTemperature: Float?) {
        // TODO: check how old is signal information + also handle no signal record in SignalMeasurementProcessor
        Timber.d("LPT PROCESSING LOCATION...")
        val coverageMeasurementDataValue = stateManager.state.value ?: return

        if (coverageMeasurementDataValue.state == CoverageMeasurementState.FINISHED_LOOP_CORRECTLY
            || coverageMeasurementDataValue.state == CoverageMeasurementState.IDLE) return

        val session = coverageMeasurementDataValue.coverageMeasurementSession
        Timber.d("LPT SDT SESSION: $session")

        val sessionWithTemperature = updateCurrentTemperature(session, currentTemperature)
        val sessionWithIpAddress = updateCurrentLocalIpAddress(sessionWithTemperature)
        sessionWithIpAddress?.let {
            if (session?.localIpAddress != sessionWithIpAddress.localIpAddress || session?.temperature != sessionWithIpAddress.temperature) {
                signalMeasurementRepository.saveCoverageMeasurementSession(it)
                stateManager.onSessionUpdate(it)
            }
        }

        val lastRecordedFence = loadLastFenceForSessionLoop( session?.localLoopId)
        Timber.d("LPT LAST FENCE #${lastRecordedFence?.sequenceNumber}: $lastRecordedFence")

        var jobCellularMetadata: Job? = null
        var jobLocationMetadata: Job? = null
        session?.localMeasurementId?.let { localMeasurementId ->
            session.startMeasurementTimeResponseReceivedNanos.let {startTimeNanos ->

                jobLocationMetadata = launch(Dispatchers.IO) {
                    testDataRepository.saveLocationMetadataForCoverage(location, localMeasurementId, startTimeNanos)
                }

                jobCellularMetadata = launch(Dispatchers.IO) {
                    testDataRepository.saveCellMetadataForCoverage(networkInfo, localMeasurementId, startTimeNanos)
                }
            }
        }

        val newFenceRadiusBase = computeSpeedAndUpdateFenceRadius(coverageMeasurementDataValue.currentLocation, location)

        stateManager.updateLocation(location)
        val isBackOnMobileData = mainCoverageDataValidator.isBackToMobile(coverageMeasurementDataValue.currentNetworkInfo, networkInfo?.networkInfo)

        stateManager.updateNetworkInfo(networkInfo?.networkInfo)

        if (isBackOnMobileData && lastRecordedFence != null) {
            onMeasurementStopAndStartNewMeasurement(CoverageMeasurementTerminationCause.EndedByBackOnMobileData())
            waitForCompletition(jobLocationMetadata)
            waitForCompletition(jobCellularMetadata)
            return
        }


        val reasonToTerminate = getReasonToTerminateMeasurementBecauseOfSomeCause(session?.localMeasurementId)
        if (reasonToTerminate != null) {
            onMeasurementStopAndStartNewMeasurement(reasonToTerminate)
            waitForCompletition(jobLocationMetadata)
            waitForCompletition(jobCellularMetadata)
            return
        }

        val isConnectionStateValid = checkForTheConnectionState()
        if (!isConnectionStateValid) {
            waitForCompletition(jobLocationMetadata)
            waitForCompletition(jobCellularMetadata)
            return
        }

        if (!stateManager.isInStateToAddNewFences()) {
            waitForCompletition(jobLocationMetadata)
            waitForCompletition(jobCellularMetadata)
            return
        }
        if (location == null) {
            waitForCompletition(jobLocationMetadata)
            waitForCompletition(jobCellularMetadata)
            return
        }

        val newTimestamp = System.currentTimeMillis()
        val newLocation = location.toDeviceInfoLocation()

        val isDataValidToSaveNewFence = mainCoverageDataValidator.isDataValidToSaveNewFence(
            newTimestamp = newTimestamp,
            newLocation = newLocation,
            newNetworkInfo = networkInfo?.networkInfo,
            lastRecordedFenceRecord = lastRecordedFence
        )
        val sessionId = session?.localMeasurementId
        if (sessionId == null) {
            Timber.e("Signal measurement Session not initialized yet - sessionId missing")
            waitForCompletition(jobLocationMetadata)
            waitForCompletition(jobCellularMetadata)
            return
        }
        Timber.d("Current radius to save new fence: ${config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement} vs new one $newFenceRadiusBase")
        if (isDataValidToSaveNewFence) {
            config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement = newFenceRadiusBase
            Timber.d("Current radius to save new fence Saving new with base: ${config.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement} vs $newFenceRadiusBase")
            val newFenceRadius = newFenceRadiusBase * config.minDistanceFactorCoverageMeasurement.toDouble()
            saveNewFence(
                sessionId = sessionId,
                newLocation = newLocation!!,
                newTimestamp = newTimestamp,
                networkInfo = networkInfo?.networkInfo,
                fenceRadiusMeters = newFenceRadius,
                lastFenceMinTechSignal = stateManager.getMinSignalForTechnologyForCurrentFence(networkInfo?.networkInfo)
            )
        } else {
            Timber.d("Not saving new fence")
        }
        waitForCompletition(jobLocationMetadata)
        waitForCompletition(jobCellularMetadata)
        /*
        TODO: Same location to replace points?
        else if (isTheSameLocation(location.toDeviceInfoLocation())) { // todo verify what to do on the same location and what values needs to be replaced - how to replace ping, ...
            val lastPoint = coverageMeasurementData.value?.points?.lastOrNull()
            lastPoint?.let { point ->
                replaceSignalFenceAndSave(point, signalRecord)
            }
        }*/
    }

    private suspend fun waitForCompletition(job: Job?) {
        job?.join()
    }

    private fun updateCurrentTemperature(session: CoverageMeasurementSession?, currentTemperature: Float?): CoverageMeasurementSession? {
        if (session == null || currentTemperature == null) return session

        val lastTemperature = session.temperature
        if (currentTemperature != lastTemperature) {
            val updatedSession = session.copy(
                temperature = currentTemperature
            )
            return updatedSession
        }
        return session
    }

    private fun updateCurrentLocalIpAddress(session: CoverageMeasurementSession?): CoverageMeasurementSession? {
        val ipv4 = ipChangeWatcher.lastIPv4Address.privateAddress
        val ipv6 = ipChangeWatcher.lastIPv6Address.privateAddress

        val localIpAddress = ipv4 ?: ipv6

        if (session == null || localIpAddress == null) return session

        val lastLocalIpAddress = session.localIpAddress
        if (lastLocalIpAddress == null) {
            val updatedSession = session.copy(
                localIpAddress = localIpAddress
            )
            return updatedSession
        }
        return session
    }

    private fun getReasonToTerminateMeasurementBecauseOfSomeCause(sessionId: String?): CoverageMeasurementTerminationCause? {
        if (sessionId == null) return null

        val fencesCount = fencesDataSource.loadCoverageMeasurementFences(sessionId).count()
        Timber.d("Check fences count: $fencesCount")
        if (fencesCount >= MAXIMUM_FENCES_IN_SINGLE_MEASUREMENT) {
            return CoverageMeasurementTerminationCause.EndedByTooManyFences()
        }

        val locationsCount = testDataRepository.getLocationMetadataCountForCoverageMeasurement(localMeasurementId = sessionId)
        Timber.d("Check locations count: $locationsCount")
        if (locationsCount >= MAXIMUM_LOCATIONS_IN_SINGLE_MEASUREMENT) {
            return CoverageMeasurementTerminationCause.EndedByTooManyGeolocations()
        }

        val signalCountNew = testDataRepository.getSignalsCountForCoverageMeasurement(sessionId)
        Timber.d("Check signals count: $signalCountNew")
        if (signalCountNew >= MAXIMUM_SIGNALS_IN_SINGLE_MEASUREMENT) {
            return CoverageMeasurementTerminationCause.EndedByTooManySignals()
        }

        return null
    }

    private fun checkForTheConnectionState(): Boolean {
        val airplaneModeEnabled = connectivityMonitor.isAirplaneModeCurrentlyEnabled()
        val mobileDataEnabled = connectivityMonitor.isMobileDataEnabled()
        Timber.d("CMPS airplane mode: $airplaneModeEnabled, mobileDataEnabled: $mobileDataEnabled")
        if (airplaneModeEnabled || !mobileDataEnabled) {
            pauseCoverageSession()
            return false
        }
        resumeCoverageSession()
        return true
    }

    private suspend fun loadLastFenceForSessionLoop(sessionLoopId: String?): CoverageMeasurementFenceRecord? {
        if (sessionLoopId == null) return null
        return signalMeasurementRepository.loadLastSignalMeasurementPointRecordsForLoopMeasurementList(localLoopSessionId = sessionLoopId, limit = 1).firstOrNull()
    }

    private suspend fun saveNewFence(
        sessionId: String,
        newLocation: DeviceInfo.Location,
        newTimestamp: Long,
        networkInfo: NetworkInfo?,
        fenceRadiusMeters: Double,
        lastFenceMinTechSignal: Int?,
    ) {
        Timber.d("ENDING SESSION: FENCE CREATED: session: $sessionId")
        fencesDataSource.createSignalFenceAndUpdateLastOne(
            sessionId = sessionId,
            location = newLocation,
            signalRecord = null,
            radiusMeters = fenceRadiusMeters,
            entryTimestampMillis = newTimestamp,
            networkInfo = networkInfo,
            lastFenceMinTechSignal = lastFenceMinTechSignal,
            avgPingMillisForLastFence = coveragePingProcessor.onNewFenceStarted()?.average
        )
        stateManager.onFenceExitClean(networkInfo)
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
            signalMeasurementRepository.updateSignalMeasurementFence(updatedPoint)
        }
    }

    private fun isTheSameLocation(location: DeviceInfo.Location?, lastSavedLocation: DeviceInfo.Location?): Boolean {
        return coverageLocationValidator.isTheSameLocation(
            newLocation = location,
            lastSavedLocation = lastSavedLocation
        )
    }

    /**
     * Stop of single measurement in a loop
     */
    suspend fun onMeasurementStopAndStartNewMeasurement(reasonToTerminate: CoverageMeasurementTerminationCause) {
        stateManager.state.value.coverageMeasurementSession?.let { lastMeasurement ->
            updateLastFenceOnLeaving()
            coverageLoopManager.endMeasurementInLoop(lastMeasurement, reasonToTerminate)
            coverageLoopManager.createNewMeasurementInLoop(lastMeasurement)
        }
    }

    suspend fun onNetworkChanged() {
        stateManager.state.value.coverageMeasurementSession?.let { lastMeasurement ->
            updateLastFenceOnLeaving()
            coverageLoopManager.endMeasurementInLoop(lastMeasurement, CoverageMeasurementTerminationCause.EndedByNetworkChange())
            Timber.d("Starting new measurement because of network change")
            coverageLoopManager.createNewMeasurementInLoop(lastMeasurement)
        }
    }

    fun cleanData() {
        stateManager.initData()
        coverageMeasurementSettings.signalMeasurementLastMeasurementId = null
    }

    private fun loadPoints(localLoopSessionId: String) = scope.launch(Dispatchers.IO) {
        fencesDataSource.loadCoverageLoopFences(localLoopSessionId)
            .asFlow().
            flowOn(Dispatchers.IO)
                .collect { loadedPoints ->
                    stateManager.updatePoints(loadedPoints)
                }
    }

    fun onCoverageConfigurationChanged() {
        // do nothing
    }

}