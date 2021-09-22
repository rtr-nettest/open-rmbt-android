package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import at.rmbt.util.io
import at.rtr.rmbt.android.BuildConfig
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.specure.data.ClientUUID
import at.specure.data.ControlServerSettings
import at.specure.data.MeasurementServers
import at.specure.data.repository.SettingsRepository
import at.specure.location.LocationState

private val KEY_PERMISSIONS_MODE: String = "KEY_PERMISSIONS_MODE"

class SettingsViewState constructor(
    val appConfig: AppConfig,
    val clientUUID: ClientUUID,
    private val measurementServers: MeasurementServers,
    private val controlServerSettings: ControlServerSettings,
    private val settingsRepository: SettingsRepository
) : ViewState {

    var showPermissionsOnly: Boolean = false
    val qosMeasurement = ObservableField(!appConfig.skipQoSTests)
    val canManageLocationSettings = ObservableField(appConfig.canManageLocationSettings)
    val loopModeEnabled = ObservableField(appConfig.loopModeEnabled)
    val expertModeEnabled = ObservableField(appConfig.expertModeEnabled)
    val expertModeUseIpV4Only = ObservableField(appConfig.expertModeUseIpV4Only)
    val loopModeWaitingTimeMin = ObservableField(appConfig.loopModeWaitingTimeMin)
    val loopModeDistanceMeters = ObservableField(appConfig.loopModeDistanceMeters)
    val loopModeNumberOfTests = ObservableInt(appConfig.loopModeNumberOfTests)
    val developerModeIsAvailable = appConfig.developerModeIsAvailable
    val developerModeIsEnabled = ObservableField(appConfig.developerModeIsEnabled)
    val developerModeTag = ObservableField(appConfig.measurementTag)
    val controlServerOverrideEnabled = ObservableField(appConfig.controlServerOverrideEnabled)
    val controlServerHost = ObservableField(controlServerSettings.controlServerOverrideUrl)
    val controlServerPort = ObservableField(controlServerSettings.controlServerOverridePort)
    val controlServerUseSSL = ObservableField(appConfig.controlServerUseSSL)
    val isLocationEnabled = ObservableField<LocationState>()
    val dataPrivacyPolicyUrl = ObservableField(appConfig.dataPrivacyAndTermsUrl)
    val termsUrl = ObservableField(appConfig.termsAcceptanceDefaultUrl)
    val mapServerOverrideEnabled = ObservableField(appConfig.mapServerOverrideEnabled)
    val mapServerHost = ObservableField(appConfig.mapServerHost)
    val mapServerPort = ObservableField(appConfig.mapServerPort)
    val mapServerUseSSL = ObservableField(appConfig.mapServerUseSSL)
    val qosSSL = ObservableField(appConfig.qosSSL)
    val selectedMeasurementServer = ObservableField(measurementServers.selectedMeasurementServer)
    val clientUUIDFormatted = ObservableField(if (clientUUID.value.isNullOrEmpty()) "" else "U{$clientUUID.value}")
    val persistentClientUUIDEnabled = ObservableField(appConfig.persistentClientUUIDEnabled)
    val analyticsEnabled = ObservableField(appConfig.analyticsEnabled)
    val webPageUrl = ObservableField(appConfig.aboutWebPageUrl)

    private fun setControlServerAddress() {
        if ((appConfig.controlServerOverrideEnabled) && (appConfig.developerModeIsEnabled)) {
            controlServerHost.get()?.let {
                appConfig.controlServerHost = it
            }
            controlServerPort.get()?.let {
                appConfig.controlServerPort = it.toInt()
            }
        } else {
            appConfig.controlServerHost = BuildConfig.CONTROL_SERVER_HOST.value
            appConfig.controlServerPort = BuildConfig.CONTROL_SERVER_PORT.value.toInt()
        }

        refreshSettings()
    }

    private fun refreshSettings() {
        io {
            settingsRepository.refreshSettings()
        }
    }

    init {
        if (controlServerPort.get().isNullOrEmpty()) {
            controlServerPort.set(appConfig.controlServerPort.toString())
            controlServerSettings.controlServerOverridePort = controlServerPort.get()
        }
        if (controlServerHost.get().isNullOrEmpty()) {
            controlServerHost.set(appConfig.controlServerHost)
            controlServerSettings.controlServerOverrideUrl = controlServerHost.get()
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
        persistentClientUUIDEnabled.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.persistentClientUUIDEnabled = it
                if (!it) {
                    refreshSettings()
                }
            }
        }
        expertModeEnabled.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.expertModeEnabled = it
                if (it) {
                    refreshSettings()
                }
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
        loopModeNumberOfTests.addOnPropertyChanged { value ->
            value.get().let {
                appConfig.loopModeNumberOfTests = it
            }
        }
        developerModeIsEnabled.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.developerModeIsEnabled = it
                setControlServerAddress()
            }
        }
        controlServerHost.addOnPropertyChanged { value ->
            value.get()?.let {
                controlServerSettings.controlServerOverrideUrl = it
                setControlServerAddress()
            }
        }
        controlServerPort.addOnPropertyChanged { value ->
            value.get()?.let {
                controlServerSettings.controlServerOverridePort = it
                setControlServerAddress()
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
                setControlServerAddress()
            }
        }
        mapServerHost.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.mapServerHost = it
            }
        }
        mapServerPort.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.mapServerPort = it
            }
        }
        mapServerUseSSL.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.mapServerUseSSL = it
            }
        }
        mapServerOverrideEnabled.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.mapServerOverrideEnabled = it
                if (!it) {
                    io {
                        settingsRepository.refreshSettings()
                    }
                }
            }
        }
        developerModeTag.addOnPropertyChanged { value ->
            appConfig.measurementTag = value.get()
        }
        qosSSL.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.qosSSL = it
            }
        }
        selectedMeasurementServer.addOnPropertyChanged { value ->
            value.get().let {
                measurementServers.selectedMeasurementServer = it
            }
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        super.onSaveState(bundle)
        bundle?.apply {
            putBoolean(KEY_PERMISSIONS_MODE, showPermissionsOnly)
        }
    }

    override fun onRestoreState(bundle: Bundle?) {
        super.onRestoreState(bundle)
        bundle?.let {
            showPermissionsOnly = it.getBoolean(KEY_PERMISSIONS_MODE, false)
        }
    }
}
