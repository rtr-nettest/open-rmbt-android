package at.rtr.rmbt.android.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import at.rmbt.client.control.NewsItem
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.HomeViewState
import at.specure.data.ClientUUID
import at.specure.data.MeasurementServers
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.NewsRepository
import at.specure.data.repository.SettingsRepository
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.connectivity.ConnectivityInfoLiveData
import at.specure.info.ip.IpV4ChangeLiveData
import at.specure.info.ip.IpV6ChangeLiveData
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.measurement.coverage.RtrCoverageMeasurementProcessor
import at.specure.measurement.signal.SignalMeasurementProducer
import at.specure.measurement.signal.SignalMeasurementService
import at.rmbt.client.control.data.SignalMeasurementType
import at.specure.measurement.coverage.data.CoverageMeasurementData
import at.specure.measurement.coverage.domain.validators.GpsValidator
import at.specure.test.toDeviceInfoLocation
import at.specure.util.map.CustomMarker
import at.specure.util.permission.PermissionsWatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

const val LOCATION_ACCURACY_WARNING_DIALOG_SILENCED_TIME_MILLIS = 60_000L

class HomeViewModel @Inject constructor(
    @Named("GPSAndFusedLocationProvider") private val locationWatcher: LocationWatcher,
    val signalStrengthLiveData: SignalStrengthLiveData,
    connectivityInfoLiveData: ConnectivityInfoLiveData,
    val activeNetworkLiveData: ActiveNetworkLiveData,
    val permissionsWatcher: PermissionsWatcher,
    val ipV4ChangeLiveData: IpV4ChangeLiveData,
    val ipV6ChangeLiveData: IpV6ChangeLiveData,
    val clientUUID: ClientUUID,
    private val appConfig: AppConfig,
    private val newsRepository: NewsRepository,
    private val settingsRepository: SettingsRepository,
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val rtrCoverageMeasurementProcessor: RtrCoverageMeasurementProcessor,
    measurementServers: MeasurementServers,
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
    private val customMarker: CustomMarker,
    private val gpsValidator: GpsValidator,
) : BaseViewModel() {

    val state = HomeViewState(appConfig, measurementServers)

    // If ConnectivityInfo is null than no internet connection otherwise internet connection available
    val isConnected: LiveData<Boolean> = connectivityInfoLiveData.map {
        state.isConnected.set(it != null)
        it != null
    }

    private var loadPointJob: Job? = null

    val locationStateLiveData: LiveData<LocationState?>
        get() = locationWatcher.stateLiveData

    val locationLiveData: LiveData<LocationInfo?>
        get() = locationWatcher.liveData

    private var _pointsLiveData = MutableLiveData<List<CoverageMeasurementFenceRecord>>()
    private var _dedicatedSignalMeasurementSessionIdLiveData : LiveData<String?> = MutableLiveData<String>(null)
    private var _coverageMeasurementDataLiveData : LiveData<CoverageMeasurementData?> = rtrCoverageMeasurementProcessor.coverageMeasurementData

    private var producer: SignalMeasurementProducer? = null
    private var _activeMeasurementSource: LiveData<Boolean>? = null
    private val _activeMeasurementMediator = MediatorLiveData<Boolean>()

    private var _pausedMeasurementSource: LiveData<Boolean>? = null
    private var _pausedMeasurementMediator = MediatorLiveData<Boolean>()
    private var _currentSignalMeasurementMapPointsLiveData: LiveData<List<CoverageMeasurementFenceRecord>>? = null
    private var toggleService: Boolean = false

    private var _getNewsLiveData = MutableLiveData<List<NewsItem>?>()

    val coverageMeasurementDataLiveData : LiveData<CoverageMeasurementData?>
        get() = _coverageMeasurementDataLiveData

    val dedicatedSignalMeasurementSessionIdLiveData : LiveData<String?>
        get() = _dedicatedSignalMeasurementSessionIdLiveData

    val currentSignalMeasurementMapPointsLiveData: LiveData<List<CoverageMeasurementFenceRecord>>
        get() = rtrCoverageMeasurementProcessor.signalPoints // _pointsLiveData

    val activeSignalMeasurementLiveData: LiveData<Boolean>
        get() = _activeMeasurementMediator

    val pausedSignalMeasurementLiveData: LiveData<Boolean>
        get() = _pausedMeasurementMediator

    val newsLiveData: LiveData<List<NewsItem>?>
        get() = _getNewsLiveData

    val isExpertModeOn: Boolean
        get() = appConfig.expertModeEnabled

    val isalwaysAllowCellInfosOn: Boolean
        get() = appConfig.alwaysAllowCellInfos

    val customMarkerProvider: CustomMarker
        get() = customMarker

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("Signal measurement service connected")
            producer = service as SignalMeasurementProducer

            if (producer != null && toggleService) {
                toggleService = false
                toggleSignalMeasurementService()
            }


            _activeMeasurementSource = producer?.activeStateLiveData
            _activeMeasurementSource?.let { lv ->
                _activeMeasurementMediator.addSource(lv) {
                    _activeMeasurementMediator.postValue(it)
                }
            }

            _pausedMeasurementSource = producer?.pausedStateLiveData
            _pausedMeasurementSource?.let { lv ->
                _pausedMeasurementMediator.addSource(lv) {
                    _pausedMeasurementMediator.postValue(it)
                }
            }

            producer?.let {
                _dedicatedSignalMeasurementSessionIdLiveData = it.signalMeasurementSessionIdLiveData
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.d("Signal measurement service disconnected")
            _activeMeasurementSource?.let {
                _activeMeasurementMediator.removeSource(it)
            }

            _pausedMeasurementSource?.let {
                _pausedMeasurementMediator.removeSource(it)
            }

            producer = null
            _activeMeasurementSource = null
            _pausedMeasurementSource = null
        }
    }

    init {
        addStateSaveHandler(state)
        _activeMeasurementMediator.postValue(false)
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

    fun toggleSignalMeasurementService() {
        if (producer == null) {
            toggleService = true
        } else {
            producer?.let {
                if (it.isActive) {
                    it.stopMeasurement(false)
                } else {
                    it.startMeasurement(false, SignalMeasurementType.DEDICATED)
                    it.setEndAlarm()
                }
            }
        }
    }

    fun getNews() = launch(CoroutineName("HomeViewModelGetNews")) {
        settingsRepository.refreshSettingsByFlow()
            .flowOn(Dispatchers.IO)
            .collect {
                Timber.d("OkHttp Settings request response received")
            }
        newsRepository.getNews()
            .flowOn(Dispatchers.IO)
            .collect {
                _getNewsLiveData.postValue(it)
            }
    }

    fun startSignalMeasurement(signalMeasurementType: SignalMeasurementType) {
        coverageMeasurementSettings.signalMeasurementIsRunning = true
        producer?.startMeasurement(false, signalMeasurementType)
    }

    fun stopSignalMeasurement() {
        coverageMeasurementSettings.signalMeasurementIsRunning = false
        producer?.stopMeasurement(false)
    }

    fun pauseSignalMeasurement() {
        coverageMeasurementSettings.signalMeasurementIsRunning = false
        producer?.pauseMeasurement(false)
    }

    fun resumeSignalMeasurement() {
        coverageMeasurementSettings.signalMeasurementIsRunning = true
        producer?.resumeMeasurement(false)
    }

    fun attach(context: Context) {
        context.bindService(SignalMeasurementService.intent(context), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun detach(context: Context) {
        serviceConnection.onServiceDisconnected(null)
        context.unbindService(serviceConnection)
    }

    fun setNewsShown(newItem: NewsItem) {
        newsRepository.setNewsShown(newItem)
    }

    fun getLatestNewsShown(): Long? {
        return newsRepository.getLatestNewsShown()
    }

    fun shouldAskForPermission(): Boolean {
        return (appConfig.lastPermissionAskedTimestampMillis + askPermissionsAgainTimesMillis) < System.currentTimeMillis()
    }

    fun permissionsWereAsked() {
        appConfig.lastPermissionAskedTimestampMillis = System.currentTimeMillis()
    }

    fun shouldDisplayNetworkDetails(): Boolean {
        // allow cell infos is expert mode is enabled or if always enabled by configuration
        return ((isExpertModeOn || isalwaysAllowCellInfosOn ) && (state.activeNetworkInfo.get()?.networkInfo?.type == TransportType.WIFI || state.activeNetworkInfo.get()?.networkInfo?.type == TransportType.CELLULAR))
    }

    fun isMobileNetworkActive(): Boolean {
        return state.activeNetworkInfo.get()?.networkInfo?.type != TransportType.WIFI
    }

    fun isOnlyOneSimActive(): Boolean {
        return if (state.activeNetworkInfo.get()?.networkInfo is CellNetworkInfo && appConfig.shouldCheckActiveSimsCount) {
            (state.activeNetworkInfo.get()?.networkInfo as CellNetworkInfo).subscriptionsCount <= 1
        } else {
            true
        }
    }

    fun setIsCloseDialogShown(isShown: Boolean) {
        state.closeDialogDisplayed.set(isShown)
    }

    fun silenceLocationDialogWarning() {
        state.locationWarningDialogSilenced.set(true)
        launch(CoroutineName("SilenceLocationDialogWarning")) {
            delay(LOCATION_ACCURACY_WARNING_DIALOG_SILENCED_TIME_MILLIS)
            state.locationWarningDialogSilenced.set(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadPointJob?.cancel()
    }

    fun isLocationInfoMeetingQualityCriteria(): Boolean {
        val isNotNull = locationLiveData.value != null
        return isNotNull && isLocationAccuracyGoodEnough()
    }

    private fun isLocationAccuracyGoodEnough(): Boolean {
        return gpsValidator.isLocationAccuracyPreciseEnough(locationLiveData.value?.toDeviceInfoLocation())
    }

    fun shouldOpenSignalMeasurementScreen(): Boolean {
        return coverageMeasurementSettings.signalMeasurementIsRunning
    }

    fun setSignalMeasurementShouldContinueInLastSession(shouldContinueInLastSession: Boolean) {
        coverageMeasurementSettings.signalMeasurementShouldContinueInLastSession = shouldContinueInLastSession
    }

    fun shouldSignalMeasurementContinueInLastSession(): Boolean {
        return coverageMeasurementSettings.signalMeasurementShouldContinueInLastSession
    }

    suspend fun getSignalData(id: String?): SignalRecord? {
        val record = signalMeasurementRepository.getSignalMeasurementRecord(id)
        return record
    }

}
