package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.util.InfoWindowStatus
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.specure.data.MeasurementServers
import at.specure.info.ip.IpInfo
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.location.LocationState

class HomeViewState(
    private val config: AppConfig,
    private val measurementServers: MeasurementServers
) : ViewState {

    val isConnected = ObservableField<Boolean?>()
    val isLocationEnabled = ObservableField<LocationState>()
    val signalStrength = ObservableField<SignalStrengthInfo>()
    val activeNetworkInfo = ObservableField<NetworkInfo?>()
    val infoWindowStatus = ObservableField(InfoWindowStatus.NONE)
    val ipV4Info = ObservableField<IpInfo?>()
    val ipV6Info = ObservableField<IpInfo?>()
    val isSignalMeasurementActive = ObservableField<Boolean>()
    val isLoopModeActive = ObservableBoolean(config.loopModeEnabled)
    val expertModeIsEnabled = ObservableField(config.expertModeEnabled)
    val developerModeIsEnabled = ObservableField(config.developerModeIsEnabled)
    val selectedMeasurementServer = ObservableField(measurementServers.selectedMeasurementServer)

    init {
        isLoopModeActive.addOnPropertyChanged {
            config.loopModeEnabled = it.get()
        }
    }

    override fun onRestoreState(bundle: Bundle?) {
    }

    override fun onSaveState(bundle: Bundle?) {
    }

    fun checkConfig() {
        isLoopModeActive.set(config.loopModeEnabled)
        developerModeIsEnabled.set(config.developerModeIsEnabled)
        selectedMeasurementServer.set(measurementServers.selectedMeasurementServer)
        expertModeIsEnabled.set(config.expertModeEnabled)
    }
}