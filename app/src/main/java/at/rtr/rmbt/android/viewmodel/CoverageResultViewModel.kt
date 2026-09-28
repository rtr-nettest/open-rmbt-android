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
import at.specure.util.map.blendedColorInt
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import android.location.Location
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.StyleSpan
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
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.pow
import kotlin.math.round

const val MIN_MAP_UPDATE_RATE =
    700 // do not set it bellow maybe 500ms as map is very sensitive to frequent updates

// At/above this zoom the map shows individual point circles (only those in the current viewport);
// below it, the whole run is drawn as one colored track polyline instead. This keeps the number of
// live map overlays bounded regardless of how many fences the measurement produced (fixes OOM).
const val POINTS_ZOOM_THRESHOLD = 14f
// Break the track line where two consecutive fences are farther apart than this (avoids long
// bridging lines across gaps, e.g. after a pause or loss of coverage).
const val TRACK_GAP_BREAK_METERS = 500f
const val TRACK_LINE_WIDTH_PX = 12f


class CoverageResultViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val testResultsRepository: TestResultsRepository,
    private val rtrCoverageMeasurementProcessor: RtrCoverageMeasurementProcessor,
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
    private val locationValidator: LocationValidator,
) : BaseViewModel() {

    val state = CoverageResultViewState(appConfig)

    private var loadPointJob: Job? = null
    private var _testResultLiveData: LiveData<TestResultRecord?>? = null
    var coverageSessionId: String? = null
        private set

    private var _pointsLiveData = MutableLiveData<List<CoverageMeasurementFenceRecord>>()

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

    private fun getIcon(
        type: MobileNetworkType,
        signalDbm: Int? = null,
        pingMillis: Double? = null
    ): Map<String, Int> {
        val fillColor = type.blendedColorInt(
            signalDbm,
            pingMillis
        )

        return mapOf(
            "strokeColor" to "#ffffff".toColorInt(),
            "strokeWidth" to 1,
            "fillColor" to fillColor,
        )
    }

    private fun calculateMarkerRadius(zoom: Float): Double {
        return max(round(2.0.pow(20.0 - zoom.toDouble()) * 0.8), 2.0)
    }

    private var zoomUpdateJob: Job? = null
    private var lastMarkerRadius: Double? = null
    fun updateMarkersRadius(zoom: Float) {
        zoomUpdateJob?.cancel()
        zoomUpdateJob = viewModelScope.launch(Dispatchers.Main) {
            delay(100.milliseconds)
            state.zoom = zoom
            val newRadius = calculateMarkerRadius(zoom)
            if (newRadius != lastMarkerRadius) {
                lastMarkerRadius = newRadius
                circlesByHash.values.forEach { it.radius = newRadius }
            }
            // A zoom may cross the line/points threshold and a pan may change the visible viewport, so
            // re-render from the cached data (no new fences needed).
            cachedMap?.let { renderForCurrentZoom(it, cachedPoints) }
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
        setOnMapLoadedCallback { cont.resume(Unit) }
    }

    private enum class RenderMode { LINE, POINTS }

    private var lastMarker: Circle? = null
    private var lastMapUpdate = 0L
    private var updateMapJob: Job? = null
    // Point circles currently on the map, keyed by point hash so the viewport cull can add/remove
    // individual ones. In LINE mode this is empty and the track is drawn as polyline(s) instead.
    private val circlesByHash = LinkedHashMap<String, Circle>()
    private val trackPolylines = mutableListOf<Polyline>()
    private var currentRenderMode: RenderMode? = null
    private var polylinePointCount = -1 // point count the track line was last built for
    private var lastKnownPointCount = 0 // to detect a new (reset) measurement
    private var lastPointWasOngoing = false
    // Cached inputs so a camera zoom/pan can re-render without new fence data arriving.
    private var cachedMap: GoogleMap? = null
    private var cachedPoints: List<FencesResultItemRecord> = emptyList()
    private val distanceResult = FloatArray(1)

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
        val shouldForceUpdate = hasRecentlyClosedLastPoint(pts)
        if (!shouldUpdateMap() && isMeasurementInProgress && !shouldForceUpdate) return

        cachedMap = currentMap
        cachedPoints = pts

        updateMapJob?.cancel()
        updateMapJob = viewModelScope.launch(Dispatchers.Main) {
            // A new (reset) measurement: the point list shrank -> drop all overlays and start fresh.
            if (pts.size < lastKnownPointCount) clearOverlaysForNewMeasurement(currentMap)
            lastKnownPointCount = pts.size

            renderForCurrentZoom(currentMap, pts)

            if (
                coverageMeasurementState == null
                || coverageMeasurementState == CoverageMeasurementState.FINISHED_LOOP_CORRECTLY
            ) {
                zoomMapToFitPoints(pts, currentMap)
            }

            // The live "current fence" circle (big radius) is drawn separately and shown in both modes.
            val liveNetworkInfo =
                coverageMeasurementDataLiveData.value?.currentNetworkInfo as? CellNetworkInfo
            updateLastPointCircle(
                pts.lastOrNull(),
                isMeasurementInProgress,
                if (isMeasurementInProgress) liveNetworkInfo?.networkType else null,
                if (isMeasurementInProgress) liveNetworkInfo?.signalStrength?.value else null,
                if (isMeasurementInProgress) coverageMeasurementDataLiveData.value?.currentPingMs else null,
                currentMap
            )
        }
    }

    /**
     * Renders the run according to the current zoom: below [POINTS_ZOOM_THRESHOLD] as one (or a few,
     * gap-broken) colored track polyline(s); at/above it as individual point circles, but only those
     * inside the current viewport, so the number of live overlays stays bounded (no decimation).
     */
    private fun renderForCurrentZoom(
        map: GoogleMap,
        pts: List<FencesResultItemRecord>
    ) {
        val mode = if (state.zoom >= POINTS_ZOOM_THRESHOLD) RenderMode.POINTS else RenderMode.LINE
        when (mode) {
            RenderMode.LINE -> {
                removeAllCircles()
                // Rebuild the line only when entering line mode or when the point set changed.
                if (currentRenderMode != RenderMode.LINE || polylinePointCount != pts.size) {
                    buildTrackPolylines(map, pts)
                    polylinePointCount = pts.size
                }
            }
            RenderMode.POINTS -> {
                removeTrackPolylines()
                renderPointsInViewport(map, pts)
            }
        }
        currentRenderMode = mode
    }

    /** Draws the run as colored polyline(s): one span per segment, colored by signal; broken on gaps. */
    private fun buildTrackPolylines(map: GoogleMap, pts: List<FencesResultItemRecord>) {
        removeTrackPolylines()
        val segLatLngs = ArrayList<LatLng>()
        val segColors = ArrayList<Int>()

        fun flush() {
            if (segLatLngs.size >= 2) {
                val options = PolylineOptions()
                    .width(TRACK_LINE_WIDTH_PX)
                    .clickable(false)
                    .geodesic(false)
                    .zIndex(50f)
                    .addAll(segLatLngs)
                // One span per segment (i-1)->i, colored by the destination point's signal.
                for (i in 1 until segLatLngs.size) options.addSpan(StyleSpan(segColors[i]))
                trackPolylines.add(map.addPolyline(options))
            }
            segLatLngs.clear()
            segColors.clear()
        }

        var prev: LatLng? = null
        for (point in pts) {
            val latLng = point.toLatLng() ?: continue
            if (prev != null && distanceMeters(prev, latLng) > TRACK_GAP_BREAK_METERS) flush()
            segLatLngs.add(latLng)
            segColors.add(pointColor(point))
            prev = latLng
        }
        flush()
    }

    /** Adds circles only for points inside the current viewport, removing those that scrolled out. */
    private fun renderPointsInViewport(map: GoogleMap, pts: List<FencesResultItemRecord>) {
        val bounds = try {
            map.projection.visibleRegion.latLngBounds
        } catch (e: Exception) {
            Timber.d(e, "Map projection not ready; skipping viewport point render")
            null
        } ?: return

        val radius = calculateMarkerRadius(state.zoom)
        val visible = HashSet<String>()
        for (point in pts) {
            val latLng = point.toLatLng() ?: continue
            if (!bounds.contains(latLng)) continue
            val hash = point.generateHash()
            visible.add(hash)
            if (circlesByHash.containsKey(hash)) continue

            val tech = MobileNetworkType.fromValue(point.networkTechnologyId ?: 0)
            val icon = getIcon(tech, point.signalMainDbm, point.averagePingMillis)
            val circle = map.addCircle(
                CircleOptions()
                    .center(latLng)
                    .radius(radius)
                    .strokeColor(icon["strokeColor"]!!)
                    .strokeWidth(icon["strokeWidth"]!!.toFloat())
                    .fillColor(icon["fillColor"]!!)
                    .clickable(true)
                    .zIndex(100f)
            )
            circle.tag = CoverageMarkerDetailsData(
                id = point.id,
                networkType = tech.intValue,
                tech.displayName,
                provider = null,
                signalClass = null,
                signalStrength = point.signalMainDbm,
                pingMillis = (point.averagePingMillis?.times(1000000))?.toLong(),
                timestamp = point.fenceTimestampMillis,
                isNotFinished = point.isNotFinished(),
                hash = hash
            )
            circlesByHash[hash] = circle
        }
        val iterator = circlesByHash.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in visible) {
                entry.value.remove()
                iterator.remove()
            }
        }
    }

    private fun pointColor(point: FencesResultItemRecord): Int {
        val tech = MobileNetworkType.fromValue(point.networkTechnologyId ?: 0)
        return getIcon(tech, point.signalMainDbm, point.averagePingMillis)["fillColor"]!!
    }

    private fun distanceMeters(a: LatLng, b: LatLng): Float {
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, distanceResult)
        return distanceResult[0]
    }

    private fun removeAllCircles() {
        if (circlesByHash.isEmpty()) return
        circlesByHash.values.forEach { it.remove() }
        circlesByHash.clear()
    }

    private fun removeTrackPolylines() {
        if (trackPolylines.isEmpty()) {
            polylinePointCount = -1
            return
        }
        trackPolylines.forEach { it.remove() }
        trackPolylines.clear()
        polylinePointCount = -1
    }

    private fun clearOverlaysForNewMeasurement(map: GoogleMap) {
        removeAllCircles()
        removeTrackPolylines()
        lastMarker = null
        currentRenderMode = null
        map.clear()
    }

    private suspend fun zoomMapToFitPoints(pts: List<FencesResultItemRecord>, map: GoogleMap) {
        val latLngs = pts.mapNotNull { it.toLatLng() }
        if (latLngs.isEmpty()) return

        withContext(Dispatchers.Main) { map.awaitMapLoad() }

        val boundsBuilder = LatLngBounds.Builder()
        latLngs.forEach { boundsBuilder.include(it) }
        val bounds = boundsBuilder.build()
        val padding = 100 // pixels

        viewModelScope.launch(Dispatchers.Main) {
            map.setMaxZoomPreference(DefaultLocation.defaultMaximumZoomLevelForCoverage)
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            map.resetMinMaxZoomPreference()
        }
    }

    private fun updateLastPointCircle(
        lastPoint: FencesResultItemRecord?,
        isMeasurementInProgress: Boolean,
        liveNetworkType: MobileNetworkType?,
        liveSignal: Int?,
        livePing: Double?,
        currentMap: GoogleMap
    ) {
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
            val signal =
                if (isMeasurementInProgress && point.isNotFinished()) liveSignal else point.signalMainDbm
            val ping =
                if (isMeasurementInProgress && point.isNotFinished()) livePing else point.averagePingMillis

            val icon = getIcon(tech, signal, ping)
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
    }

    private fun hasRecentlyClosedLastPoint(pts: List<FencesResultItemRecord>): Boolean {
        val last = pts.lastOrNull() ?: return false
        val nowOngoing = last.isNotFinished()
        val closed = lastPointWasOngoing && !nowOngoing
        lastPointWasOngoing = nowOngoing
        return closed
    }

    fun clearPerformanceImprovementLists(map: GoogleMap?) {
        updateMapJob?.cancel()
        updateMapJob = null
        state.markerDetailsDisplayed.set(false)
        viewModelScope.launch(Dispatchers.Main) {
            // Overlay mutations must run on Main to avoid concurrent modification.
            removeAllCircles()
            removeTrackPolylines()
            currentRenderMode = null
            lastKnownPointCount = 0
            lastMarker = null
            map?.clear()
        }
        Timber.d("Coverage map overlays cleared")
    }

    fun getCurrentNetworkTypeName(networkInfo: NetworkInfo?): String? {
        return when (networkInfo?.type) {
            TransportType.CELLULAR ->
                (networkInfo as CellNetworkInfo).networkType.generationDisplayName(nrFlavor = true)
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

    /** Accuracy (in meters) at/below which a fix is precise enough for signal measurement. */
    val minLocationAccuracyMetersDuringSignalMeasurement: Int
        get() = appConfig.minLocationAccuracyMetersDuringSignalMeasurement

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

    /**
     * Called once the user has left the result page of a finished measurement. From now on the loop
     * is historic and its data (fences, sessions) may be purged by the retention sweep.
     */
    fun onFinishedResultLeft() {
        coverageMeasurementSettings.clearProtectedCoverageLoopId()
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

    /**
     * Reads the current coverage-measurement state synchronously from the (hot) processor state flow.
     * Unlike [coverageMeasurementDataLiveData] (cold until observed) this is safe to call in onCreate.
     */
    fun currentMeasurementState(): CoverageMeasurementState =
        rtrCoverageMeasurementProcessor.stateManager.state.value.state
}

fun FencesResultItemRecord.toLatLng(): LatLng? {
    if (this.latitude == null || this.longitude == null) return null
    return LatLng(this.latitude!!, this.longitude!!)
}