package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.SettingsViewState
import at.specure.data.ClientUUID
import at.specure.data.MeasurementServers
import at.specure.location.LocationProviderStateLiveData
import javax.inject.Inject

class SettingsViewModel @Inject constructor(appConfig: AppConfig, clientUUID: ClientUUID, val locationStateLiveData: LocationProviderStateLiveData, val measurementServers: MeasurementServers) :
    BaseViewModel() {

    val state = SettingsViewState(appConfig, clientUUID, measurementServers)

    private val _openCodeWindow = MutableLiveData<Boolean>()
    private var count: Int = 0

    val openCodeWindow: LiveData<Boolean>
        get() = _openCodeWindow

    init {
        addStateSaveHandler(state)
    }

    fun isLoopModeWaitingTimeValid(value: Int, minValue: Int, maxValue: Int): Boolean {

        if (value in minValue..maxValue || state.developerModeIsEnabled.get() == true) {
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
                DEVELOPER_ACTIVATE_CODE -> {
                    state.developerModeIsEnabled.set(true)
                    R.string.preferences_developer_options_available
                }
                DEVELOPER_DEACTIVATE_CODE -> {
                    state.developerModeIsEnabled.set(false)
                    R.string.preferences_developer_options_disabled
                }
                SERVER_SELECTION_ACTIVATE_CODE -> {
                    state.userServerSelectionEnabled.set(true)
                    R.string.preferences_server_selection_available
                }
                SERVER_SELECTION_DEACTIVATE_CODE -> {
                    state.userServerSelectionEnabled.set(false)
                    R.string.preferences_server_selection_disabled
                }
                ALL_DEACTIVATE_CODE -> {
                    state.developerModeIsEnabled.set(false)
                    state.userServerSelectionEnabled.set(false)
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

    companion object {
        private const val DEVELOPER_ACTIVATE_CODE: String = "23656990"
        private const val DEVELOPER_DEACTIVATE_CODE: String = "69652357"
        private const val SERVER_SELECTION_ACTIVATE_CODE: String = "17031552"
        private const val SERVER_SELECTION_DEACTIVATE_CODE: String = "15031722"
        private const val ALL_DEACTIVATE_CODE: String = "00000000"
    }
}
