package at.rtr.rmbt.android.viewmodel

import android.os.SystemClock
import androidx.core.graphics.toColorInt
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
import at.specure.util.map.blendedColorInt
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

const val MAX_MARKER_COUNT_DISPLAYED_THRESHOLD = 500
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

    private val markerIconCache = mutableMapOf<Int, Map<String, Int>>()

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

    private fun getCachedIcon(
        type: MobileNetworkType,
        point: FencesResultItemRecord? = null
    ): Map<String, Int> {
        val fillColor = type.blendedColorInt(
            point?.signalMainDbm,
            point?.averagePingMillis
        )

        return markerIconCache.getOrPut(fillColor) {
            return mapOf(
                "strokeColor" to "#ffffff".toColorInt(),
                "strokeWidth" to 1,
                "fillColor" to fillColor,
            )
        }
    }

    private fun calculateMarkerRadius(zoom: Float): Double {
        return round(2.0.pow(20.0 - zoom.toDouble()) * 0.8)
    }

    private var zoomUpdateJob: Job? = null
    private var lastMarkerRadius: Double? = null
    fun updateMarkersRadius(zoom: Float) {
        zoomUpdateJob?.cancel()
        zoomUpdateJob = viewModelScope.launch {
            delay(100)
            val newRadius = calculateMarkerRadius(zoom)
            if (newRadius == lastMarkerRadius) return@launch
            lastMarkerRadius = newRadius
            state.markers.forEach { circle ->
                circle.radius = newRadius
            }
        }
    }

    suspend fun zoomMapToShowAllMarkers(markersOptions: List<CircleOptions>, map: GoogleMap) {
        if (markersOptions.isEmpty()) return

        withContext(Dispatchers.Main) {
            map.awaitMapLoad()
        }

        val boundsBuilder = LatLngBounds.Builder()
        markersOptions.forEach {
            it.center?.let { point -> boundsBuilder.include(point) }
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

    private var lastMarker: Circle? = null
    private var lastMapUpdate = 0L
    private var updateMapJob: Job? = null
    private var lastVisibilityStep: Int? = null

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
    ) {
        state.coverageSessionStart =
            coverageMeasurementDataLiveData.value?.coverageMeasurementSession?.startTimeLoopMillis

        val currentMap = map ?: return
        val pts = points ?: return
        val isMeasurementInProgress =
            coverageMeasurementState != null && coverageMeasurementState != CoverageMeasurementState.FINISHED_LOOP_CORRECTLY
        if (!shouldUpdateMap() && isMeasurementInProgress) return

        updateMapJob?.cancel()
        updateMapJob = viewModelScope.launch(Dispatchers.Default) {
            clearPerformanceListsIfTheyAreFromPreviousMeasurement(currentMap, pts)

            // Filter only points that haven't been displayed yet
            val newPoints = pts.filter { !state.displayedPointIds.contains(it.generateHash()) }
            if (newPoints.isEmpty()) {
                return@launch
            }

            // Switch to main thread to add markers and circles
            withContext(Dispatchers.Main) {
                val markerDetailsMap = mutableMapOf<Long, CoverageMarkerDetailsData>()
                val liveNetworkType: MobileNetworkType? = if (isMeasurementInProgress) {
                    (coverageMeasurementDataLiveData.value?.currentNetworkInfo as? CellNetworkInfo)?.networkType
                } else null
                val lastPoint = newPoints.lastOrNull() ?: pts.lastOrNull()

                updateCurrentLastMarkerIfFinished(pts)

                val markerOptionsList =
                    getUpdatedMarkerOptions(
                        newPoints,
                        isMeasurementInProgress,
                        liveNetworkType,
                        markerDetailsMap
                    )

                if (coverageMeasurementState == null || coverageMeasurementState == CoverageMeasurementState.FINISHED_LOOP_CORRECTLY) {
                    zoomMapToShowAllMarkers(markersOptions = markerOptionsList, map = map)
                }

                // Add new markers
                markerOptionsList.forEachIndexed { index, options ->
                    currentMap.addCircle(options).let { marker ->
                        val point = newPoints[index]
                        state.displayedPointIds.add(point.generateHash())
                        marker.tag = markerDetailsMap[point.id]

                        state.markers.addLast(marker)
                    }
                }

                // Add/update circles for last point
                lastPoint?.let { point ->

                    val latLng = point.toLatLng() ?: return@let
                    // Use live network type for the ongoing fence so the circle colour
                    // matches the current technology in real-time.
                    val tech =
                        if (isMeasurementInProgress && point.isNotFinished() && liveNetworkType != null) {
                            liveNetworkType
                        } else {
                            MobileNetworkType.fromValue(point.networkTechnologyId ?: 0)
                        }
                    val icon = getCachedIcon(tech, point)
                    val colorInt = icon["fillColor"]!!
                    val fillColor = makeSemiTransparent(colorInt)
                    val radius = point.fenceRadiusMeters ?: 0.0

                    if (lastMarker == null) {
                        val options = CircleOptions()
                            .center(latLng)
                            .radius(radius)
                            .strokeColor(colorInt)
                            .strokeWidth(2f)
                            .fillColor(fillColor)
                            .zIndex(101f)

                        lastMarker = currentMap.addCircle(
                            options
                        )
                    } else {
                        lastMarker?.center = latLng
                        lastMarker?.radius = radius
                        lastMarker?.strokeColor = colorInt
                        lastMarker?.fillColor = fillColor
                    }
                }

                togglePointsVisibility()
            }
        }
    }

    private fun updateCurrentLastMarkerIfFinished(pts: List<FencesResultItemRecord>) {
        val currentLastMarker = state.markers.lastOrNull()
        currentLastMarker?.let { marker ->
            val data = marker.tag as? CoverageMarkerDetailsData
            data?.id?.let { lastId ->
                val updatedPoint = pts.find { it.id == lastId }

                updatedPoint?.let { point ->
                    if (point.isNotFinished()) return@let
                    val tech = MobileNetworkType.fromValue(point.networkTechnologyId ?: 0)
                    val updatedData = CoverageMarkerDetailsData(
                        id = point.id,
                        networkType = tech.intValue,
                        tech.displayName,
                        provider = null,
                        signalClass = null,
                        signalStrength = point.signalMainDbm,
                        pingMillis = (point.averagePingMillis?.times(1000000))?.toLong(),
                        timestamp = point.fenceTimestampMillis,
                        isNotFinished = false,
                    )

                    marker.tag = updatedData

                    // refresh icon in case the technology has changed
                    val icon = getCachedIcon(tech, point)
                    marker.fillColor = icon["fillColor"]!!
                }
            }
        }
    }

    private fun togglePointsVisibility() {
        val step =
            floor(state.displayedPointIds.size.toDouble() / MAX_MARKER_COUNT_DISPLAYED_THRESHOLD).toInt() + 1
        if (step == lastVisibilityStep) return
        lastVisibilityStep = step
        state.markers.forEachIndexed { index, circle ->
            val shouldBeVisible = index % step == 0
            if (circle.isVisible != shouldBeVisible) {
                circle.isVisible = shouldBeVisible
            }
        }
    }

    private fun getUpdatedMarkerOptions(
        newPoints: List<FencesResultItemRecord>,
        isMeasurementInProgress: Boolean,
        liveNetworkType: MobileNetworkType?,
        markerDetailsMap: MutableMap<Long, CoverageMarkerDetailsData>
    ): List<CircleOptions> {
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
            val icon = getCachedIcon(tech, point)

            CircleOptions()
                .center(latLng)
                .radius(calculateMarkerRadius(state.zoom))
                .strokeColor(icon["strokeColor"]!!)
                .strokeWidth(icon["strokeWidth"]!!.toFloat())
                .fillColor(icon["fillColor"]!!)
                .clickable(true)
                .zIndex(100f)
        }
        return markerOptionsList
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
        updateMapJob?.cancel()
        updateMapJob = null
        state.displayedPointIds.clear()
        state.markerDetailsDisplayed.set(false)
        viewModelScope.launch(Dispatchers.Main) {
            // markers and map.clear() must both run on Main to avoid
            // ConcurrentModificationException with togglePointsVisibility
            state.markers.clear()
            lastVisibilityStep = null
            lastMarker = null
            map?.clear()
        }
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
}

fun FencesResultItemRecord.toLatLng(): LatLng? {
    if (this.latitude == null || this.longitude == null) return null
    return LatLng(this.latitude!!, this.longitude!!)
}