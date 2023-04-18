package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.util.InfoWindowStatus
import at.rtr.rmbt.android.util.InformationAccessProblem
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.specure.data.MeasurementServers
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.ip.IpInfo
import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.location.LocationState

private const val KEY_IAP = "KEY_IAP"

class HomeViewState(
    private val config: AppConfig,
    private val measurementServers: MeasurementServers
) : ViewState {

    val isConnected = ObservableField<Boolean?>()
    val isLocationEnabled = ObservableField<LocationState>()
    val signalStrength = ObservableField<SignalStrengthInfo>()
    val secondary5GSignalStrength = ObservableField<SignalStrengthInfo>()
    val activeNetworkInfo = ObservableField<DetailedNetworkInfo?>()
    val secondary5GActiveNetworkInfo = ObservableField<CellNetworkInfo?>()
    val infoWindowStatus = ObservableField(InfoWindowStatus.NONE)
    val ipV4Info = ObservableField<IpInfo?>()
    val ipV6Info = ObservableField<IpInfo?>()
    val isSignalMeasurementActive = ObservableField<Boolean>()
    val isLoopModeActive = ObservableBoolean(config.loopModeEnabled)
    val expertModeIsEnabled = ObservableField(config.expertModeEnabled)
    val developerModeIsEnabled = ObservableField(config.developerModeIsEnabled)
    val coverageModeIsEnabled = ObservableField(config.coverageModeEnabled)
    val selectedMeasurementServer = ObservableField(measurementServers.selectedMeasurementServer)
    val informationAccessProblem = ObservableField(InformationAccessProblem.NO_PROBLEM)

    init {
        isLoopModeActive.addOnPropertyChanged {
            config.loopModeEnabled = it.get()
        }
    }

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            informationAccessProblem.set(InformationAccessProblem.values()[(it.getInt(KEY_IAP))])
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.putInt(
            KEY_IAP,
            informationAccessProblem.get()?.ordinal ?: InformationAccessProblem.NO_PROBLEM.ordinal
        )
    }

    fun checkConfig() {
        isLoopModeActive.set(config.loopModeEnabled)
        developerModeIsEnabled.set(config.developerModeIsEnabled)
        coverageModeIsEnabled.set(config.coverageModeEnabled)
        selectedMeasurementServer.set(measurementServers.selectedMeasurementServer)
        expertModeIsEnabled.set(config.expertModeEnabled)
    }
}