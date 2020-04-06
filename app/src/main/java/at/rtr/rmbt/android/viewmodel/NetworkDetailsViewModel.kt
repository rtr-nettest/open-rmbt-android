package at.rtr.rmbt.android.viewmodel

import at.rtr.rmbt.android.ui.viewstate.NetworkDetailsViewState
import at.specure.info.connectivity.ConnectivityInfoLiveData
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.ip.IpV4ChangeLiveData
import at.specure.info.ip.IpV6ChangeLiveData
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.location.LocationProducer
import at.specure.util.permission.PermissionsWatcher
import javax.inject.Inject

class NetworkDetailsViewModel @Inject constructor(
    val connectivityInfoLiveData: ConnectivityInfoLiveData,
    val activeNetworkLiveData: ActiveNetworkLiveData,
    val permissionsWatcher: PermissionsWatcher,
    val signalStrengthLiveData: SignalStrengthLiveData,
    val ipV4ChangeLiveData: IpV4ChangeLiveData,
    val ipV6ChangeLiveData: IpV6ChangeLiveData,
    val locationProducer: LocationProducer
) : BaseViewModel() {

    val state = NetworkDetailsViewState()

    init {
        addStateSaveHandler(state)
    }
}