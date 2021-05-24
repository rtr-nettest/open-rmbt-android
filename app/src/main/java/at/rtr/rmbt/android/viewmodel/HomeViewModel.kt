package at.rtr.rmbt.android.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.NewsItem
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.HomeViewState
import at.rtr.rmbt.android.util.map
import at.specure.data.ClientUUID
import at.specure.data.MeasurementServers
import at.specure.data.repository.NewsRepository
import at.specure.data.repository.SettingsRepository
import at.specure.info.TransportType
import at.specure.info.connectivity.ConnectivityInfoLiveData
import at.specure.info.ip.IpV4ChangeLiveData
import at.specure.info.ip.IpV6ChangeLiveData
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.measurement.signal.SignalMeasurementProducer
import at.specure.measurement.signal.SignalMeasurementService
import at.specure.test.SignalMeasurementType
import at.specure.util.permission.PermissionsWatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    private val locationWatcher: LocationWatcher,
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
    measurementServers: MeasurementServers
) : BaseViewModel() {

    val state = HomeViewState(appConfig, measurementServers)

    // If ConnectivityInfo is null than no internet connection otherwise internet connection available
    val isConnected: LiveData<Boolean> = connectivityInfoLiveData.map {
        state.isConnected.set(it != null)
        it != null
    }

    val locationStateLiveData: LiveData<LocationState?>
        get() = locationWatcher.stateLiveData

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

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
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
        }

        override fun onServiceDisconnected(name: ComponentName?) {
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
                    it.stopMeasurement(false)
                } else {
                    it.startMeasurement(false, SignalMeasurementType.DEDICATED)
                    it.setEndAlarm()
                }
            }
        }
    }

    fun getNews() = launch {
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
        producer?.startMeasurement(false, signalMeasurementType)
    }

    fun stopSignalMeasurement() {
        producer?.stopMeasurement(false)
    }

    fun pauseSignalMeasurement() {
        producer?.pauseMeasurement(false)
    }

    fun resumeSignalMeasurement() {
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
        return ((isExpertModeOn) && (activeNetworkLiveData.value?.type == TransportType.WIFI || activeNetworkLiveData.value?.type == TransportType.CELLULAR))
    }
}