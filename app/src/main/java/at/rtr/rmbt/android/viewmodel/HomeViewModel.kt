package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.ui.viewstate.HomeViewState
import at.rtr.rmbt.android.util.map
import at.specure.data.ClientUUID
import at.specure.info.connectivity.ConnectivityInfoLiveData
import at.specure.info.ip.IpV4ChangeLiveData
import at.specure.info.ip.IpV6ChangeLiveData
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.location.LocationInfoLiveData
import at.specure.location.LocationProviderStateLiveData
import at.specure.util.permission.PermissionsWatcher
import javax.inject.Inject

class HomeViewModel @Inject constructor(
    val signalStrengthLiveData: SignalStrengthLiveData,
    connectivityInfoLiveData: ConnectivityInfoLiveData,
    val activeNetworkLiveData: ActiveNetworkLiveData,
    val permissionsWatcher: PermissionsWatcher,
    val locationStateLiveData: LocationProviderStateLiveData,
    val locationInfoLiveData: LocationInfoLiveData,
    val ipV4ChangeLiveData: IpV4ChangeLiveData,
    val ipV6ChangeLiveData: IpV6ChangeLiveData,
    val clientUUID: ClientUUID
) : BaseViewModel() {

    val state = HomeViewState()

    // If ConnectivityInfo is null than no internet connection otherwise internet connection available
    val isConnected: LiveData<Boolean> = connectivityInfoLiveData.map {
        state.isConnected.set(it != null)
        it != null
    }

    init {
        addStateSaveHandler(state)
    }
}