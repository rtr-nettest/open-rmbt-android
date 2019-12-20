package at.rtr.rmbt.android.ui.viewstate

import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.specure.data.ClientUUID
import at.specure.location.LocationProviderState

class SettingsViewState constructor(
    private val appConfig: AppConfig,
    val clientUUID: ClientUUID
) : ViewState {

    val isNDTEnabled = ObservableField(appConfig.NDTEnabled)
    val qosMeasurement = ObservableField(!appConfig.skipQoSTests)
    val canManageLocationSettings = ObservableField(appConfig.canManageLocationSettings)
    val loopModeEnabled = ObservableField(appConfig.loopModeEnabled)
    val expertModeEnabled = ObservableField(appConfig.expertModeEnabled)
    val expertModeUseIpV4Only = ObservableField(appConfig.expertModeUseIpV4Only)
    val loopModeWaitingTimeMin = ObservableField(appConfig.loopModeWaitingTimeMin)
    val loopModeDistanceMeters = ObservableField(appConfig.loopModeDistanceMeters)
    val developerModeIsAvailable = appConfig.developerModeIsAvailable
    val developerModeIsEnabled = ObservableField(appConfig.developerModeIsEnabled)
    val controlServerOverrideEnabled = ObservableField(appConfig.controlServerOverrideEnabled)
    val controlServerHost = ObservableField(appConfig.controlServerHost)
    val controlServerPort = ObservableField(appConfig.controlServerPort)
    val controlServerUseSSL = ObservableField(appConfig.controlServerUseSSL)
    val isLocationEnabled = ObservableField<LocationProviderState>()
    val numberOfTests = ObservableInt(appConfig.testCounter)

    init {
        isNDTEnabled.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.NDTEnabled = it
            }
        }
        qosMeasurement.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.skipQoSTests = !it
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
        developerModeIsEnabled.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.developerModeIsEnabled = it
            }
        }
        controlServerHost.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.controlServerHost = it
            }
        }
        controlServerPort.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.controlServerPort = it
            }
        }
        controlServerUseSSL.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.controlServerUseSSL = it
            }
        }
        controlServerOverrideEnabled.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.controlServerOverrideEnabled = it
            }
        }
    }
}