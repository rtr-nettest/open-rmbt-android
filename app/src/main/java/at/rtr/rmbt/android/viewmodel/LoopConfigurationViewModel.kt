package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.LoopConfigurationViewState
import at.specure.info.connectivity.ConnectivityInfoLiveData
import javax.inject.Inject

class LoopConfigurationViewModel @Inject constructor(val config: AppConfig, connectivityInfoLiveData: ConnectivityInfoLiveData) : BaseViewModel() {

    val state = LoopConfigurationViewState(config)

    init {
        addStateSaveHandler(state)
    }

    val isConnected: LiveData<Boolean> = connectivityInfoLiveData.map {
        it != null
    }

    fun isWaitingTimeValid(value: Int, minValue: Int, maxValue: Int) =
        if (value in minValue..maxValue || config.developerModeIsEnabled || config.coverageModeEnabled) {
            state.waitingTime.set(value)
            true
        } else false

    fun isDistanceValid(value: Int, minValue: Int, maxValue: Int) =
        if (value in minValue..maxValue || config.developerModeIsEnabled) {
            state.distance.set(value)
            true
        } else false

    fun isNumberValid(value: Int, minValue: Int, maxValue: Int) =
        if (value in minValue..maxValue || config.developerModeIsEnabled) {
            state.numberOfTests.set(value)
            true
        } else false

    fun shouldAskForPermission(): Boolean {
        return (config.lastPermissionAskedTimestampMillis + askPermissionsAgainTimesMillis) < System.currentTimeMillis()
    }

    fun shouldAskForBackgroundPermission(): Boolean {
        return (config.lastBackgroundPermissionAskedTimestampMillis + askPermissionsAgainTimesMillis) < System.currentTimeMillis()
    }

    fun permissionsWereAsked() {
        config.lastPermissionAskedTimestampMillis = System.currentTimeMillis()
    }

    fun backgroundPermissionsWereAsked() {
        config.lastBackgroundPermissionAskedTimestampMillis = System.currentTimeMillis()
    }
}