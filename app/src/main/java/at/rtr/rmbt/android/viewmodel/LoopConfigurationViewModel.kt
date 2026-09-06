package at.rtr.rmbt.android.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.LoopConfigurationViewState
import at.rtr.rmbt.android.util.recordBackgroundPermissionResult
import at.rtr.rmbt.android.util.shouldAskForBackgroundPermission
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

    fun shouldAskForNotificationPermission(): Boolean {
        return (config.lastNotificationPermissionAskedTimestampMillis + askPermissionsAgainTimesMillis) < System.currentTimeMillis()
    }

    /**
     * Whether to show the background-location permission info screen and request the permission.
     * See the shared [shouldAskForBackgroundPermission] helper.
     */
    fun shouldAskForBackgroundPermission(context: Context): Boolean =
        shouldAskForBackgroundPermission(config, context)

    /**
     * Records the outcome of an in-app background-location permission request. See the shared
     * [recordBackgroundPermissionResult] helper.
     */
    fun recordBackgroundPermissionResult(granted: Boolean) =
        recordBackgroundPermissionResult(config, granted)

    fun notificationPermissionsWereAsked() {
        config.lastNotificationPermissionAskedTimestampMillis = System.currentTimeMillis()
    }
}