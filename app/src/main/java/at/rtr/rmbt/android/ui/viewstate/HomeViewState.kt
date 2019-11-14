package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableField
import at.rtr.rmbt.android.util.InfoWindowStatus
import at.specure.info.ip.IpInfo
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.location.LocationProviderState

class HomeViewState : ViewState {

    val isConnected = ObservableField<Boolean?>()
    val isLocationEnabled = ObservableField<LocationProviderState>()
    val signalStrength = ObservableField<SignalStrengthInfo>()
    val activeNetworkInfo = ObservableField<NetworkInfo?>()
    val infoWindowStatus = ObservableField(InfoWindowStatus.NONE)
    val ipV4Info = ObservableField<IpInfo?>()
    val ipV6Info = ObservableField<IpInfo?>()

    override fun onRestoreState(bundle: Bundle?) {
    }

    override fun onSaveState(bundle: Bundle?) {
    }
}