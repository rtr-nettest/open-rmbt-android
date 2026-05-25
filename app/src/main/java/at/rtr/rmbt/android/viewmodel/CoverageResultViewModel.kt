package at.rtr.rmbt.android.viewmodel

import android.os.SystemClock
import androidx.core.graphics.scale
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.map.DefaultLocation
import at.rtr.rmbt.android.ui.viewstate.CoverageResultViewState
import at.rtr.rmbt.android.viewmodel.viewData.CoverageMarkerDetailsData
import at.specure.data.ControlServerSettings
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.FencesResultItemRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.TestResultDetailsRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.entity.generateHash
import at.specure.data.entity.isNotFinished
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.TestResultsRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.measurement.coverage.RtrCoverageMeasurementProcessor
import at.specure.measurement.coverage.domain.models.CoverageMeasurementData
import at.specure.measurement.coverage.domain.models.state.CoverageMeasurementState
import at.specure.measurement.coverage.domain.validators.LocationValidator
import at.specure.test.DeviceInfo
import at.specure.util.map.CustomMarker
import at.specure.util.map.getMarkerColorInt
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

const val MAX_MARKER_COUNT_DISPLAYED_THRESHOLD = 100
const val TOTAL_MAX_MARKER_COUNT_DISPLAYED_THRESHOLD = 3000
const val MIN_MAP_UPDATE_RATE =
    700 // do not set it bellow maybe 500ms as map is very sensitive to frequent updates

class CoverageResultViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val testResultsRepository: TestResultsRepository,
    private val controlServerSettings: ControlServerSettings,
    private val rtrCoverageMeasurementProcessor: RtrCoverageMeasurementProcessor,
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
    private val customMarker: CustomMarker,
    private val locationValidator: LocationValidator,
) : BaseViewModel() {

    val state = CoverageResultViewState(appConfig)

    private var loadPointJob: Job? = null
    private var _testResultLiveData: LiveData<TestResultRecord?>? = null
    var coverageSessionId: String? = null
        private set

    private var _pointsLiveData = MutableLiveData<List<CoverageMeasurementFenceRecord>>()

    //    private var _dedicatedSignalMeasurementSessionIdLiveData : LiveData<String?> = MutableLiveData<String>(null)
    private val _coverageMeasurementDataLiveData: LiveData<CoverageMeasurementData?> =
        rtrCoverageMeasurementProcessor.stateManager.state.asLiveData(viewModelScope.coroutineContext)

    val testServerResultLiveData: LiveData<TestResultRecord?>
        get() {
            if (_testResultLiveData == null) {
                _testResultLiveData = testResultsRepository.getServerTestResult(state.testUUID)
            }
            return this._testResultLiveData!!
        }

    val coverageMeasurementDataLiveData: LiveData<CoverageMeasurementData?>
        get() = _coverageMeasurementDataLiveData

//    val dedicatedSignalMeasurementSessionIdLiveData : LiveData<String?>
//        get() = _dedicatedSignalMeasurementSessionIdLiveData

    val fencesLiveData: LiveData<List<FencesResultItemRecord>>
        get() {
            return testResultsRepository.getFencesDataLiveData(state.testUUID)
        }

    val testResultDetailsLiveData: LiveData<List<TestResultDetailsRecord>>
        get() {
            return testResultsRepository.getTestDetailsResult(state.testUUID)
        }

    val loadingLiveData: LiveData<Boolean>
        get() = _loadingLiveData

    private val _loadingLiveData = MutableLiveData<Boolean>()

    val customMarkerProvider: CustomMarker
        get() = customMarker

    private val markerIconCache = mutableMapOf<MobileNetworkType, BitmapDescriptor>()

    init {
        addStateSaveHandler(state)
        coverageMeasurementSettings.signalMeasurementLastMeasurementLoopId?.let {
            loadSessionPoints(it)
        }
    }

    fun loadSessionPoints(loopLocalSessionId: String) {
        loadPointJob = loadPoints(loopLocalSessionId)
    }

    private fun loadPoints(loopLocalSessionId: String) =
        launch(CoroutineName("LoadPointsHomeViewModel")) {
            val points =
                signalMeasurementRepository.loadSignalMeasurementPointRecordsForLoopMeasurement(
                    loopLocalSessionId
                )
            points.asFlow().flowOn(Dispatchers.IO).collect { loadedPoints ->
                _pointsLiveData.postValue(loadedPoints)
                Timber.d("New points loaded ${loadedPoints.size}")
            }
        }

    fun onCoverageConfigurationChanged() {
        rtrCoverageMeasurementProcessor.onCoverageConfigurationChanged()
    }

    override fun onCleared() {
        super.onCleared()
        loadPointJob?.cancel()
    }

    fun loadTestResults() = launch(CoroutineName("ResultViewModelLoadTestResults")) {
        testResultsRepository.loadTestResults(state.testUUID).zip(
            testResultsRepository.loadTestDetailsResult(state.testUUID)
        ) { a, b -> a && b }
            .flowOn(Dispatchers.IO)
            .catch {
                if (it is HandledException) {
                    Timber.e("Loaded points problem handled")
                    emit(false)
                    postError(it)
                } else {
                    Timber.e("Loaded points problem")
                    throw it
                }
            }
            .collect {
                _loadingLiveData.postValue(it)
            }
    }

    fun getCachedIcon(type: MobileNetworkType): BitmapDescriptor {
        return markerIconCache.getOrPut(type) {
            val bmp = this.customMarkerProvider.getMarker(type)
            val smallBmp = bmp.scale(48, 48)
            BitmapDescriptorFactory.fromBitmap(smallBmp)
        }
    }

    suspend fun zoomMapToShowAllMarkers(markersOptions: List<MarkerOptions>, map: GoogleMap) {
        if (markersOptions.isEmpty()) return

        withContext(Dispatchers.Main) {
            map.awaitMapLoad()
        }

        val boundsBuilder = LatLngBounds.Builder()
        markersOptions.forEach { it ->
            boundsBuilder.include(it.position)
        }
        val bounds = boundsBuilder.build()
        val padding = 100 // padding in pixels from edges

        viewModelScope.launch(Dispatchers.Main) {
            map.setMaxZoomPreference(DefaultLocation.defaultMaximumZoomLevelForCoverage)
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, padding)
            )
            map.resetMinMaxZoomPreference()
        }
    }

    fun onConfigurationChanged(map: GoogleMap?) {
        clearPerformanceImprovementLists(map)
    }

    fun onSendingResultErrorClearPressed() {
        rtrCoverageMeasurementProcessor.stateManager.removeSendingResultError()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun GoogleMap.awaitMapLoad() = suspendCancellableCoroutine<Unit> { cont ->
        setOnMapLoadedCallback { cont.resume(Unit) {} }
    }

    private var lastCircle: Circle? = null
    private val markerCache = mutableMapOf<Long, Marker>()
    private var lastMapUpdate = 0L

    private fun shouldUpdateMap(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastMapUpdate < MIN_MAP_UPDATE_RATE) return false
        lastMapUpdate = now
        return true
    }

    fun updateMapPoints(
        map: GoogleMap?,
        points: List<FencesResultItemRecord>?,
        coverageMeasurementState: CoverageMeasurementState?,
        maxLimitToDisplay: Int? = MAX_MARKER_COUNT_DISPLAYED_THRESHOLD
    ) {
        state.coverageSessionStart =
            coverageMeasurementDataLiveData.value?.coverageMeasurementSession?.startTimeLoopMillis

        val currentMap = map ?: return
        val pts = points ?: return
        val isMeasurementInProgress =
            coverageMeasurementState != null && coverageMeasurementState != CoverageMeasurementState.FINISHED_LOOP_CORRECTLY
        if (!shouldUpdateMap() && isMeasurementInProgress) return

        viewModelScope.launch(Dispatchers.Default) {
            clearPerformanceListsIfTheyAreFromPreviousMeasurement(currentMap, pts)

            // Filter only points that haven't been displayed yet
            val newPoints = pts.filter { !state.displayedPointIds.contains(it.generateHash()) }
            val markerDetailsMap = mutableMapOf<Long, CoverageMarkerDetailsData>()

            // Switch to main thread to add markers and circles
            withContext(Dispatchers.Main) {

                // Read the live network type on the main thread so the last ongoing marker
                // always reflects the current technology (not the stale value stored in DB).
                val liveNetworkType: MobileNetworkType? = if (isMeasurementInProgress) {
                    (coverageMeasurementDataLiveData.value?.currentNetworkInfo as? CellNetworkInfo)?.networkType
                } else null

                // Prepare last point for circle needed
                val lastPoint = newPoints.lastOrNull() ?: pts.lastOrNull()

                // Prepare MarkerOptions in main thread because we need liveNetworkType
                val markerOptionsList = newPoints.mapIndexedNotNull { index, point ->
                    val isLastDuringMeasurement =
                        index == newPoints.lastIndex && isMeasurementInProgress
                    
                    val isLastOngoingPoint = isLastDuringMeasurement && point.isNotFinished()
                    val tech = if (isLastOngoingPoint && liveNetworkType != null) {
                        liveNetworkType
                    } else {
                        MobileNetworkType.fromValue(point.networkTechnologyId ?: 0)
                    }

                    markerDetailsMap[point.id] = CoverageMarkerDetailsData(
                        id = point.id,
                        networkType = tech.intValue,
                        tech.displayName,
                        provider = null, // todo: map provider
                        signalClass = null,
                        signalStrength = point.signalMainDbm,
                        pingMillis = (point.averagePingMillis?.times(1000000))?.toLong(),
                        timestamp = point.fenceTimestampMillis,
                        isNotFinished = isLastOngoingPoint
                    )
                    val latLng = point.toLatLng() ?: return@mapIndexedNotNull null
                    MarkerOptions()
                        .position(latLng)
                        .icon(getCachedIcon(tech))
                        .anchor(0.5f, 0.5f)
                }

                if (coverageMeasurementState == null || coverageMeasurementState == CoverageMeasurementState.FINISHED_LOOP_CORRECTLY) {
                    zoomMapToShowAllMarkers(markersOptions = markerOptionsList, map = map)
                }

                // Update previously last marker with fresh data after leaving it
                val previouslyLastMarker = state.markers.lastOrNull()
                previouslyLastMarker?.let { marker ->
                    val data = marker.tag as? CoverageMarkerDetailsData
                    data?.id?.let { lastId ->
                        val updatedPoint = pts.find { it.id == lastId }

                        updatedPoint?.let { point ->
                            val isLastOngoingPoint = isMeasurementInProgress && lastPoint?.id == point.id && point.isNotFinished()
                            // For the current unfinished fence use the live technology so the
                            // marker colour updates immediately when the technology changes
                            // (the DB record is only written when the fence is finalised).
                            val tech = if (isLastOngoingPoint && liveNetworkType != null) {
                                liveNetworkType
                            } else {
                                MobileNetworkType.fromValue(point.networkTechnologyId ?: 0)
                            }
                            val updatedData = CoverageMarkerDetailsData(
                                id = point.id,
                                networkType = tech.intValue,
                                tech.displayName,
                                provider = null,
                                signalClass = null,
                                signalStrength = point.signalMainDbm,
                                pingMillis = (point.averagePingMillis?.times(1000000))?.toLong(),
                                timestamp = point.fenceTimestampMillis,
                                isNotFinished = isLastOngoingPoint,
                            )

                            marker.tag = updatedData

                            // refresh icon in case the technology has changed
                            marker.setIcon(getCachedIcon(tech))
                        }
                    }
                }

                // Add new markers
                markerOptionsList.forEachIndexed { index, options ->
                    currentMap.addMarker(options)?.let { marker ->
                        val point = newPoints[index]
                        state.displayedPointIds.add(point.generateHash())
                        marker.tag = markerDetailsMap[point.id]

                        state.markers.addLast(marker)

                        // remove oldest markers if exceeding limit
                        while (state.markers.size > (maxLimitToDisplay
                                ?: TOTAL_MAX_MARKER_COUNT_DISPLAYED_THRESHOLD)
                        ) {
                            val oldMarker = state.markers.removeAt(0)
                            oldMarker.remove()
                        }
                    }
                }

                // Add/update circles for last point
                lastPoint?.let { point ->

                    val latLng = point.toLatLng() ?: return@let
                    // Use live network type for the ongoing fence so the circle colour
                    // matches the current technology in real-time.
                    val tech = if (isMeasurementInProgress && point.isNotFinished() && liveNetworkType != null) {
                        liveNetworkType
                    } else {
                        MobileNetworkType.fromValue(point.networkTechnologyId ?: 0)
                    }
                    val colorInt = tech.getMarkerColorInt()
                    val fillColor = makeSemiTransparent(colorInt)
                    val radius = point.fenceRadiusMeters?.toDouble() ?: 0.0

                    if (lastCircle == null) {
                        val options = CircleOptions()
                            .center(latLng)
                            .radius(radius)
                            .strokeColor(colorInt)
                            .strokeWidth(2f)
                            .fillColor(fillColor)

                        lastCircle = currentMap.addCircle(
                            options
                        )
                    } else {
                        lastCircle?.center = latLng
                        lastCircle?.radius = radius
                        lastCircle?.strokeColor = colorInt
                        lastCircle?.fillColor = fillColor
                    }
                }
            }
        }
    }

    private fun clearPerformanceListsIfTheyAreFromPreviousMeasurement(
        map: GoogleMap?,
        pts: List<FencesResultItemRecord>
    ) {
        if (state.displayedPointIds.size > pts.size) {
            clearPerformanceImprovementLists(map)
        }
    }

    fun clearPerformanceImprovementLists(map: GoogleMap?) {
        state.displayedPointIds.clear()
        viewModelScope.launch(Dispatchers.Main) {
            map?.clear()
            lastCircle = null
        }
        state.markers.clear()
        state.markerDetailsDisplayed.set(false)
        Timber.d("Lists optimisation cleared")
    }

    fun getCurrentNetworkTypeName(networkInfo: NetworkInfo?): String? {
        return when (networkInfo?.type) {
            TransportType.CELLULAR -> (networkInfo as CellNetworkInfo).networkType.displayName
            TransportType.WIFI,
            TransportType.BLUETOOTH,
            TransportType.ETHERNET,
            TransportType.VPN,
            TransportType.WIFI_AWARE,
            TransportType.LOWPAN,
            TransportType.BROWSER,
            TransportType.UNKNOWN -> networkInfo.type.name

            null -> null
        }
    }

    private fun makeSemiTransparent(color: Int, alpha: Int = 1): Int {
        // alpha: 0..255, 128 = 50% transparency
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    fun isLocationInfoMeetingQualityCriteria(location: DeviceInfo.Location?): Boolean {
        val isNotNull = location != null
        return isNotNull && isLocationAccuracyGoodEnough(location)
    }

    private fun isLocationAccuracyGoodEnough(location: DeviceInfo.Location?): Boolean {
        return locationValidator.isLocationFreshAndAccurate(location)
    }

    fun clearMeasurementData() {
        rtrCoverageMeasurementProcessor.cleanData()
    }

    fun onCoverageSessionLoaded(sessionId: String?) {
        coverageSessionId = sessionId
        sessionId?.let {
            loadSessionPoints(it)
        }
    }

    fun shouldRunCoverageMeasurement(): Boolean {
        val measurementNotFinishedOrNotStarted =
            coverageMeasurementDataLiveData.value?.state == null
                    || coverageMeasurementDataLiveData.value?.state != CoverageMeasurementState.FINISHED_LOOP_CORRECTLY
        Timber.d("Current last state of data: ${coverageMeasurementDataLiveData.value?.state}")
        return measurementNotFinishedOrNotStarted
    }

    fun shouldSignalMeasurementContinueInLastSession(): Boolean {
        return coverageMeasurementSettings.signalMeasurementShouldContinueInLastSession
    }

    suspend fun getSignalData(id: String?): SignalRecord? {
        val record = signalMeasurementRepository.getSignalMeasurementRecord(id)
        return record
    }
}

fun FencesResultItemRecord.toLatLng(): LatLng? {
    if (this.latitude == null || this.longitude == null) return null
    return LatLng(this.latitude!!, this.longitude!!)
}

fun DeviceInfo.Location?.toLatLng(): LatLng? {
    this?.let {
        return LatLng(it.lat, it.long)
    }
    return null
}