package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.IpProtocol
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.SettingsViewState
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.specure.data.ClientUUID
import at.specure.data.ControlServerSettings
import at.specure.data.MeasurementServers
import at.specure.data.repository.SettingsRepository
import at.specure.info.ip.IpInfo
import at.specure.info.ip.IpV4ChangeLiveData
import at.specure.info.ip.IpV6ChangeLiveData
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val locationWatcher: LocationWatcher,
    clientUUID: ClientUUID,
    val measurementServers: MeasurementServers,
    settingsRepository: SettingsRepository,
    controlServerSettings: ControlServerSettings,
    val ipV4ChangeLiveData: IpV4ChangeLiveData,
    val ipV6ChangeLiveData: IpV6ChangeLiveData
) :
    BaseViewModel() {

    val state = SettingsViewState(appConfig, clientUUID, measurementServers, controlServerSettings, settingsRepository)

    private val _openCodeWindow = MutableLiveData<Boolean>()
    private var count: Int = 0

    val openCodeWindow: LiveData<Boolean>
        get() = _openCodeWindow

    val locationStateLiveData: LiveData<LocationState?>
        get() = locationWatcher.stateLiveData

    // Emitted (with the offending protocol) when IPv4-only / IPv6-only was switched on but that
    // protocol has no connectivity; the fragment shows a toast and the switch is reset.
    private val _connectivityErrorEvent = MutableLiveData<IpProtocol>()
    val connectivityErrorEvent: LiveData<IpProtocol>
        get() = _connectivityErrorEvent

    init {
        addStateSaveHandler(state)

        state.expertModeUseIpV4Only.addOnPropertyChanged { field ->
            val enabled = field.get() ?: false
            appConfig.expertModeUseIpV4Only = enabled
            if (enabled) {
                // IPv4-only and IPv6-only are mutually exclusive.
                if (state.expertModeUseIpV6Only.get() == true) {
                    state.expertModeUseIpV6Only.set(false)
                }
                if (!hasConnectivity(ipV4ChangeLiveData.value)) {
                    state.expertModeUseIpV4Only.set(false)
                    _connectivityErrorEvent.value = IpProtocol.V4
                }
            }
        }

        state.expertModeUseIpV6Only.addOnPropertyChanged { field ->
            val enabled = field.get() ?: false
            appConfig.expertModeUseIpV6Only = enabled
            if (enabled) {
                if (state.expertModeUseIpV4Only.get() == true) {
                    state.expertModeUseIpV4Only.set(false)
                }
                if (!hasConnectivity(ipV6ChangeLiveData.value)) {
                    state.expertModeUseIpV6Only.set(false)
                    _connectivityErrorEvent.value = IpProtocol.V6
                }
            }
        }
    }

    /** Connectivity for the protocol is assumed when a public address was reachable over it. */
    private fun hasConnectivity(ipInfo: IpInfo?): Boolean = ipInfo?.publicAddress != null

    fun isLoopModeWaitingTimeValid(value: Int, minValue: Int, maxValue: Int): Boolean {

        // allow any time interval to coverage mode and developer mode
        if (value in minValue..maxValue || (state.developerModeIsEnabled.get() == true) || (state.coverageModeEnabled.get() == true )) {
            state.loopModeWaitingTimeMin.set(value)
            return true
        }
        return false
    }

    fun isLoopModeDistanceMetersValid(value: Int, minValue: Int, maxValue: Int): Boolean {
        if (value in minValue..maxValue || state.developerModeIsEnabled.get() == true) {
            state.loopModeDistanceMeters.set(value)
            return true
        }
        return false
    }

    fun isLoopModeNumberOfTestValid(value: Int, minValue: Int, maxValue: Int): Boolean {
        if (value in minValue..maxValue || state.developerModeIsEnabled.get() == true) {
            state.loopModeNumberOfTests.set(value)
            return true
        }
        return false
    }

    /**
     * Function is used for check input code
     */
    fun isCodeValid(code: String): Int {

        if (code.isNotBlank()) {

            return when (code) {
                appConfig.secretCodeDeveloperModeOn -> {
                    state.developerModeIsEnabled.set(true)
                    R.string.preferences_developer_options_available
                }
                appConfig.secretCodeDeveloperModeOff -> {
                    state.developerModeIsEnabled.set(false)
                    state.controlServerOverrideEnabled.set(false)
                    state.mapServerOverrideEnabled.set(false)
                    R.string.preferences_developer_options_disabled
                }
                appConfig.secretCodeCoverageModeOn -> {
                    state.coverageModeEnabled.set(true)
                    // disable qos measurement when coverage mode is activated
                    state.qosMeasurement.set(false)
                    state.developerModeTag.set("{Cov}")
                    R.string.preferences_coverage_mode_available
                }
                appConfig.secretCodeCoverageModeOff -> {
                    state.coverageModeEnabled.set(false)
                    state.developerModeTag.set(null)
                    R.string.preferences_coverage_mode_disabled
                }
                appConfig.secretCodeAllModesOff -> {
                    state.developerModeIsEnabled.set(false)
                    state.coverageModeEnabled.set(false)
                    state.developerModeTag.set(null)
                    state.controlServerOverrideEnabled.set(false)
                    state.mapServerOverrideEnabled.set(false)
                    R.string.preferences_all_disabled
                }
                else -> {
                    R.string.preferences_developer_try_again
                }
            }
        }
        return R.string.preferences_developer_try_again
    }

    /**
     * Function is used for show input dialog when user 10 time click
     */
    fun onVersionClicked() {

        if (state.developerModeIsAvailable) {

            _openCodeWindow.postValue(false)
            if (count < 9) {
                count += 1
            } else {
                count = 0
                _openCodeWindow.postValue(true)
            }
        }
    }
}
