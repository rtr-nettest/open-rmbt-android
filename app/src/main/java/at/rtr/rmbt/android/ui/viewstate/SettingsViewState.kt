package at.rtr.rmbt.android.ui.viewstate

import androidx.databinding.ObservableField
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.specure.data.ClientUUID
import at.specure.location.LocationProviderState

class SettingsViewState constructor(
    private val appConfig: AppConfig,
    val clientUUID: ClientUUID
) : ViewState {

    val isNDTEnabled = ObservableField(appConfig.NDTEnabled)
    val skipQoSTests = ObservableField(appConfig.skipQoSTests)
    val canManageLocationSettings = ObservableField(appConfig.canManageLocationSettings)
    val loopModeEnabled = ObservableField(appConfig.loopModeEnabled)
    val expertModeEnabled = ObservableField(appConfig.expertModeEnabled)
    val expertModeUseIpV4Only = ObservableField(appConfig.expertModeUseIpV4Only)
    val loopModeWaitingTimeMin = ObservableField(appConfig.loopModeWaitingTimeMin)
    val loopModeDistanceMeters = ObservableField(appConfig.loopModeDistanceMeters)

    val isLocationEnabled = ObservableField<LocationProviderState>()

    init {
        isNDTEnabled.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.NDTEnabled = it
            }
        }
        skipQoSTests.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.skipQoSTests = it
            }
        }
        canManageLocationSettings.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.canManageLocationSettings = it
            }
        }
        loopModeEnabled.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.loopModeEnabled = it
            }
        }
        expertModeEnabled.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.expertModeEnabled = it
            }
        }
        expertModeUseIpV4Only.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.expertModeUseIpV4Only = it
            }
        }
        loopModeWaitingTimeMin.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.loopModeWaitingTimeMin = it
            }
        }
        loopModeDistanceMeters.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.loopModeDistanceMeters = it
            }
        }
    }
}