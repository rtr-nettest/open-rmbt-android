package at.rtr.rmbt.android.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.HomeViewState
import at.rtr.rmbt.android.util.map
import at.specure.data.ClientUUID
import at.specure.data.MeasurementServers
import at.specure.info.connectivity.ConnectivityInfoLiveData
import at.specure.info.ip.IpV4ChangeLiveData
import at.specure.info.ip.IpV6ChangeLiveData
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.location.LocationProviderStateLiveData
import at.specure.measurement.signal.SignalMeasurementProducer
import at.specure.measurement.signal.SignalMeasurementService
import at.specure.util.permission.PermissionsWatcher
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    val signalStrengthLiveData: SignalStrengthLiveData,
    connectivityInfoLiveData: ConnectivityInfoLiveData,
    val activeNetworkLiveData: ActiveNetworkLiveData,
    val permissionsWatcher: PermissionsWatcher,
    val locationStateLiveData: LocationProviderStateLiveData,
    val ipV4ChangeLiveData: IpV4ChangeLiveData,
    val ipV6ChangeLiveData: IpV6ChangeLiveData,
    val clientUUID: ClientUUID,
    appConfig: AppConfig,
    val measurementServers: MeasurementServers
) : BaseViewModel() {

    val state = HomeViewState(appConfig, measurementServers)

    // If ConnectivityInfo is null than no internet connection otherwise internet connection available
    val isConnected: LiveData<Boolean> = connectivityInfoLiveData.map {
        state.isConnected.set(it != null)
        it != null
    }

    private var producer: SignalMeasurementProducer? = null
    private var _activeMeasurementSource: LiveData<Boolean>? = null
    private val _activeMeasurementMediator = MediatorLiveData<Boolean>()

    private var _pausedMeasurementSource: LiveData<Boolean>? = null
    private var _pausedMeasurementMediator = MediatorLiveData<Boolean>()

    val activeSignalMeasurementLiveData: LiveData<Boolean>
        get() = _activeMeasurementMediator

    val pausedSignalMeasurementLiveData: LiveData<Boolean>
        get() = _pausedMeasurementMediator

    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            producer = service as SignalMeasurementProducer

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
    }

    fun toggleService() {
        producer?.let {
            if (it.isActive) {
                it.stopMeasurement()
            } else {
                it.startMeasurement()
            }
        }
    }

    fun startSignalMeasurement() {
        producer?.startMeasurement()
    }

    fun stopSignalMeasurement() {
        producer?.stopMeasurement()
    }

    fun pauseSignalMeasurement() {
        producer?.pauseMeasurement()
    }

    fun resumeSignalMeasurement() {
        producer?.resumeMeasurement()
    }

    fun attach(context: Context) {
        context.bindService(SignalMeasurementService.intent(context), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun detach(context: Context) {
        serviceConnection.onServiceDisconnected(null)
        context.unbindService(serviceConnection)
    }
}