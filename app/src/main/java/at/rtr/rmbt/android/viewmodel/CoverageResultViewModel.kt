package at.rtr.rmbt.android.viewmodel

import androidx.core.graphics.scale
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.CoverageResultViewState
import at.specure.data.ControlServerSettings
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.FencesResultItemRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.TestResultDetailsRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.entity.generateHash
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.TestResultsRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.measurement.coverage.RtrCoverageMeasurementProcessor
import at.specure.measurement.coverage.domain.models.CoverageMeasurementData
import at.specure.measurement.coverage.domain.validators.LocationValidator
import at.specure.test.DeviceInfo
import at.specure.util.map.CustomMarker
import at.specure.util.map.getMarkerColorInt
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

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
    var coverageSessionId : String? = null
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

    val coverageMeasurementDataLiveData : LiveData<CoverageMeasurementData?>
        get() = _coverageMeasurementDataLiveData

//    val dedicatedSignalMeasurementSessionIdLiveData : LiveData<String?>
//        get() = _dedicatedSignalMeasurementSessionIdLiveData

    val fencesLiveData: LiveData<List<FencesResultItemRecord>>
        get() = testResultsRepository.getFencesDataLiveData(state.testUUID)

    val testResultDetailsLiveData: LiveData<List<TestResultDetailsRecord>>
        get() = testResultsRepository.getTestDetailsResult(state.testUUID)

    val loadingLiveData: LiveData<Boolean>
        get() = _loadingLiveData

    private val _loadingLiveData = MutableLiveData<Boolean>()

    val customMarkerProvider: CustomMarker
        get() = customMarker

    private val markerIconCache = mutableMapOf<MobileNetworkType, BitmapDescriptor>()

    init {
        addStateSaveHandler(state)
        coverageMeasurementSettings.signalMeasurementLastSessionId?.let {
            loadSessionPoints(it)
        }
    }

    fun loadSessionPoints(sessionId: String) {
        loadPointJob = loadPoints(sessionId)
    }

    private fun loadPoints(sessionId: String) = launch(CoroutineName("LoadPointsHomeViewModel")) {
        val points =
            signalMeasurementRepository.loadSignalMeasurementPointRecordsForMeasurement(sessionId)
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
                    emit(false)
                    postError(it)
                } else {
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

    fun updateMapPoints(map: GoogleMap?, points: List<FencesResultItemRecord>?) {
        val currentMap = map ?: return
        val pts = points ?: return

        viewModelScope.launch(Dispatchers.Default) {
            // Filter only points that haven't been displayed yet
            val newPoints = pts.filter { !state.displayedPointIds.contains(it.generateHash()) }

            // Prepare MarkerOptions in background
            val markerOptionsList = newPoints.mapNotNull { point ->
                val latLng = point.toLatLng() ?: return@mapNotNull null
                val tech = MobileNetworkType.fromValue(point.networkTechnologyId ?: 0)
                MarkerOptions()
                    .position(latLng)
                    .icon(getCachedIcon(tech))
                    .title(tech.displayName)
                    .anchor(0.5f, 0.5f)
            }

            // Prepare CircleOptions for last point if needed
            val lastPoint = newPoints.lastOrNull() ?: pts.lastOrNull()
            val circleOptionsList = lastPoint?.let { point ->
                val latLng = point.toLatLng() ?: return@let null
                val tech = MobileNetworkType.fromValue(point.networkTechnologyId ?: 0)
                val colorInt = tech.getMarkerColorInt()
                val fillColor = makeSemiTransparent(colorInt)
                listOf(
                    CircleOptions()
                        .center(latLng)
                        .radius(point.fenceRadiusMeters?.toDouble() ?: 0.0)
                        .strokeColor(colorInt)
                        .strokeWidth(2f)
                        .fillColor(fillColor)
                )
            } ?: emptyList()

            // Switch to main thread to add markers and circles
            withContext(Dispatchers.Main) {
                // Add new markers
                markerOptionsList.forEachIndexed { index, options ->
                    currentMap.addMarker(options)?.let { marker ->
//                        activeMarkers.add(marker)
                        state.displayedPointIds.add(newPoints[index].generateHash())
                    }
                }

                // Add/update circles for last point
                circleOptionsList.forEach { options ->
                    currentMap.addCircle(options)?.let {
                        updateCircle(it)
                    }
                }

                // Map click listeners
                currentMap.setOnMarkerClickListener { marker ->
                    state.markerDetailsDisplayed.set(true)
                    false
                }
                currentMap.setOnMapClickListener {
                    state.markerDetailsDisplayed.set(false)
                }
            }
        }
    }

    fun clearPerformanceImprovementLists() {
        state.activeCircles.clear()
        state.displayedPointIds.clear()
    }

    fun getCurrentNetworkTypeName(networkInfo: NetworkInfo?): String? {
        return when(networkInfo?.type) {
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

    private fun updateCircle(it: Circle) {
        state.activeCircles.forEach { it.remove() }
        state.activeCircles.clear()
        state.activeCircles.add(it)
    }

    fun isLocationInfoMeetingQualityCriteria(location: DeviceInfo.Location?): Boolean {
        val isNotNull = location != null
        return isNotNull && isLocationAccuracyGoodEnough(location)
    }

    private fun isLocationAccuracyGoodEnough(location: DeviceInfo.Location?): Boolean {
        return locationValidator.isLocationFreshAndAccurate(location)
    }

    fun onCoverageSessionLoaded(sessionId: String?) {
        coverageSessionId = sessionId
        sessionId?.let {
            loadSessionPoints(it)
        }
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