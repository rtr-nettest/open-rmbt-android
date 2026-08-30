package at.rtr.rmbt.android.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import at.rmbt.client.control.ControlServerModule
import at.rmbt.client.control.IpProtocol
import at.rmbt.client.control.NewsItem
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.location.LocationModule
import at.rtr.rmbt.android.ui.viewstate.HomeViewState
import at.specure.data.ClientUUID
import at.specure.data.MeasurementServers
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.repository.NewsRepository
import at.specure.data.repository.SettingsRepository
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.connectivity.ConnectivityInfoLiveData
import at.specure.info.ip.IpInfo
import at.specure.info.ip.IpV4ChangeLiveData
import at.specure.info.ip.IpV6ChangeLiveData
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.measurement.signal.SignalMeasurementProducer
import at.specure.measurement.signal.SignalMeasurementService
import at.rmbt.client.control.data.SignalMeasurementType
import at.specure.util.permission.PermissionsWatcher
import at.specure.worker.WorkLauncher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.milliseconds

const val LOCATION_ACCURACY_WARNING_DIALOG_SILENCED_TIME_MILLIS = 60_000L

class HomeViewModel @Inject constructor(
    private val locationWatcher: LocationWatcher,
    // GNSS-only watcher (reported source "gps"). The signal (coverage) measurement uses GPS only,
    // so its start precheck must be evaluated against this watcher, never the combined one.
    @Named(LocationModule.GPS_LOCATION) private val gpsLocationWatcher: LocationWatcher,
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
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
    private val controlServerModule: ControlServerModule,
    measurementServers: MeasurementServers,
) : BaseViewModel() {

    var shouldStartDedicatedMeasurementStateChecker: () -> Boolean = { false }

    val state = HomeViewState(appConfig, measurementServers)

    // If ConnectivityInfo is null than no internet connection otherwise internet connection available
    val isConnected: LiveData<Boolean> = connectivityInfoLiveData.map {
        state.isConnected.set(it != null)
        it != null
    }
    private var _dedicatedSignalMeasurementSessionIdLiveData : LiveData<String?> = MutableLiveData<String>(null)
    val dedicatedSignalMeasurementSessionIdLiveData : LiveData<String?>
        get() = _dedicatedSignalMeasurementSessionIdLiveData

    val locationStateLiveData: LiveData<LocationState?>
        get() = locationWatcher.stateLiveData

    val locationLiveData: LiveData<LocationInfo?>
        get() = locationWatcher.liveData

    /**
     * GNSS-only location. Observing this (e.g. from the home screen) keeps the GPS source warm so the
     * signal-measurement start precheck can be evaluated against a real "gps" fix.
     */
    val gpsLocationLiveData: LiveData<LocationInfo?>
        get() = gpsLocationWatcher.liveData

    private var producer: SignalMeasurementProducer? = null
    private var _activeMeasurementSource: LiveData<Boolean>? = null
    private val _activeMeasurementMediator = MediatorLiveData<Boolean>()

    private var _pausedMeasurementSource: LiveData<Boolean>? = null
    private var _pausedMeasurementMediator = MediatorLiveData<Boolean>()
    private var toggleService: Boolean = false

    private var _getNewsLiveData = MutableLiveData<List<NewsItem>?>()

    val activeSignalMeasurementLiveData: LiveData<Boolean>
        get() = _activeMeasurementMediator

    val pausedSignalMeasurementLiveData: LiveData<Boolean>
        get() = _pausedMeasurementMediator

    val newsLiveData: LiveData<List<NewsItem>?>
        get() = _getNewsLiveData

    val isExpertModeOn: Boolean
        get() = appConfig.expertModeEnabled

    val isDeveloperModeOn: Boolean
        get() = appConfig.developerModeIsEnabled

    val isalwaysAllowCellInfosOn: Boolean
        get() = appConfig.alwaysAllowCellInfos

    val shouldRequestBackgroundLocationPermission: Boolean
        get() = appConfig.shouldRequestBackgroundLocation

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Timber.d("Signal measurement service connected")
            producer = service as SignalMeasurementProducer?

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
    }

    fun toggleSignalMeasurementService() {
        if (producer == null) {
            toggleService = true
        } else {
            producer?.let {
                if (it.isActive) {
                    Timber.d("Stopping coverage session HVM1")
                    it.stopMeasurement(false)
                } else {
                    if (shouldStartDedicatedMeasurementStateChecker()) {
                        Timber.d("Starting coverage session HVM2")
                        it.startMeasurement(false, SignalMeasurementType.DEDICATED)
                    }
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
        if (shouldStartDedicatedMeasurementStateChecker()) {
            coverageMeasurementSettings.signalMeasurementIsRunning = true
            Timber.d("Starting coverage session HVM1")
            producer?.startMeasurement(false, signalMeasurementType)
        }
    }

    fun stopSignalMeasurement(): LiveData<Boolean>? {
        coverageMeasurementSettings.signalMeasurementIsRunning = false
        Timber.d("Stopping coverage session HVM2")
        producer?.stopMeasurement(false)
        return producer?.activeStateLiveData
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
            delay(LOCATION_ACCURACY_WARNING_DIALOG_SILENCED_TIME_MILLIS.milliseconds)
            state.locationWarningDialogSilenced.set(false)
        }
    }

    fun silenceNetworkWarning() {
        state.networkWarningDialogSilenced.set(true)
        launch(CoroutineName("SilenceNetworkDialogWarning")) {
            delay(LOCATION_ACCURACY_WARNING_DIALOG_SILENCED_TIME_MILLIS.milliseconds)
            state.networkWarningDialogSilenced.set(false)
        }
    }

    fun shouldOpenSignalMeasurementScreen(): Boolean {
        return state.isSignalMeasurementActive.get() == true
    }

    fun setSignalMeasurementShouldContinueInLastSession(shouldContinueInLastSession: Boolean) {
        coverageMeasurementSettings.signalMeasurementShouldContinueInLastSession = shouldContinueInLastSession
    }

    fun syncCoverageOnRequests(context: Context) {
        controlServerModule.onResponseInterceptor = { response ->
            if (response.isSuccessful && coverageMeasurementSettings.hasUnsyncedCoverage) {
                coverageMeasurementSettings.hasUnsyncedCoverage = false
                WorkLauncher.enqueueCoverageSyncRequest(context)
            }
        }
    }

    /**
     * When IPv4-only or IPv6-only expert mode is active, returns the selected [IpProtocol] if that
     * protocol currently has no connectivity (no public address in the latest /ip responses) - which
     * would make the test fail, e.g. after switching to a network that lacks the forced protocol.
     * Returns null when the selected protocol is available or no protocol restriction is active.
     */
    fun unavailableForcedIpProtocol(): IpProtocol? {
        // only relevant in expert mode
        if (!appConfig.expertModeEnabled) return null
        // can't decide if no Internet at all
        if (!hasConnectivity(ipV4ChangeLiveData.value) && !hasConnectivity(ipV6ChangeLiveData.value)) return null

        return when {
            appConfig.expertModeUseIpV4Only && !hasConnectivity(ipV4ChangeLiveData.value) -> IpProtocol.V4
            appConfig.expertModeUseIpV6Only && !hasConnectivity(ipV6ChangeLiveData.value) -> IpProtocol.V6
            else -> null
        }
    }

    /** Connectivity for the protocol is assumed when a public address was reachable over it. */
    private fun hasConnectivity(ipInfo: IpInfo?): Boolean = ipInfo?.publicAddress != null

    /**
     * Returns true when the current GPS fix is good enough to START a signal (coverage) measurement.
     * Uses exactly the same minimum quality that is required for a fix to be usable DURING the
     * measurement: not older than [Config.maxAgeOfLocationInformationForSignalMeasurementMillis] and
     * accuracy better than [Config.minLocationAccuracyMetersDuringSignalMeasurement].
     *
     * The signal measurement uses GNSS only, so this is evaluated against the GPS-only watcher - never
     * the combined one, which could otherwise green-light a start on a network/fused fix.
     */
    fun isGpsQualitySufficientForSignalMeasurement(): Boolean {
        val location = currentGpsLocation() ?: return false
        if (!location.hasAccuracy) return false
        val ageMillis = location.ageNanos / 1_000_000L
        return location.accuracy <= appConfig.minLocationAccuracyMetersDuringSignalMeasurement &&
            ageMillis <= appConfig.maxAgeOfLocationInformationForSignalMeasurementMillis
    }

    /** Latest GNSS-only fix, or null. Prefers the observed LiveData value, falling back to the hot one. */
    fun currentGpsLocation(): LocationInfo? =
        gpsLocationLiveData.value ?: gpsLocationWatcher.latestLocation
}
