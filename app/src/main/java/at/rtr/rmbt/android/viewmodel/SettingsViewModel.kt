package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.SettingsViewState
import at.specure.data.ClientUUID
import at.specure.location.LocationProviderStateLiveData
import javax.inject.Inject

class SettingsViewModel @Inject constructor(appConfig: AppConfig, clientUUID: ClientUUID, val locationStateLiveData: LocationProviderStateLiveData) :
    BaseViewModel() {

    val state = SettingsViewState(appConfig, clientUUID)

    private val _openCodeWindow = MutableLiveData<Boolean>()
    private var count: Int = 0

    val openCodeWindow: LiveData<Boolean>
        get() = _openCodeWindow

    init {
        addStateSaveHandler(state)
    }

    fun isLoopModeWaitingTimeValid(value: Int, minValue: Int, maxValue: Int): Boolean {

        if (value in minValue..maxValue) {
            state.loopModeWaitingTimeMin.set(value)
            return true
        }
        return false
    }

    fun isLoopModeDistanceMetersValid(value: Int, minValue: Int, maxValue: Int): Boolean {
        if (value in minValue..maxValue) {
            state.loopModeDistanceMeters.set(value)
            return true
        }
        return false
    }

    /**
     * Function is used for check input code
     */
    fun isCodeValid(code: String): Int {

        if (code.isNotBlank()) {

            state.developerModeIsEnabled.get()?.let {

                return if (it) {
                    when (code) {
                        DEVELOPER_DEACTIVATE_CODE -> {
                            state.developerModeIsEnabled.set(false)
                            R.string.preferences_developer_options_disabled
                        }
                        ALL_DEACTIVATE_CODE -> {
                            state.developerModeIsEnabled.set(false)
                            R.string.preferences_developer_options_disabled
                        }
                        else -> {
                            R.string.preferences_developer_try_again
                        }
                    }
                } else {
                    when (code) {
                        DEVELOPER_ACTIVATE_CODE -> {
                            state.developerModeIsEnabled.set(true)
                            R.string.preferences_developer_options_available
                        }
                        else -> {
                            R.string.preferences_developer_try_again
                        }
                    }
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
        private const val ALL_DEACTIVATE_CODE: String = "00000000"
    }
}
