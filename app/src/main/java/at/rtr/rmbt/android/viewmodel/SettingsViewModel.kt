package at.rtr.rmbt.android.viewmodel

import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.SettingsViewState
import at.specure.data.ClientUUID
import at.specure.location.LocationProviderStateLiveData
import javax.inject.Inject

class SettingsViewModel @Inject constructor(appConfig: AppConfig, clientUUID: ClientUUID, val locationStateLiveData: LocationProviderStateLiveData) :
    BaseViewModel() {

    val state = SettingsViewState(appConfig, clientUUID)

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
}
