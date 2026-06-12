package at.rtr.rmbt.android.viewmodel

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.LoopConfigurationViewState
import at.specure.info.connectivity.ConnectivityInfoLiveData
import javax.inject.Inject

private const val REQUEST_CODE_BACKGROUND = 1

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

    fun shouldAskForBackgroundPermission(): Boolean {
        return config.shouldRequestBackgroundLocation && config.lastBackgroundPermissionAskedTimestampMillis == 0L
    }

    fun backgroundPermissionsWereAsked() {
        config.lastBackgroundPermissionAskedTimestampMillis = System.currentTimeMillis()
    }

    fun notificationPermissionsWereAsked() {
        config.lastNotificationPermissionAskedTimestampMillis = System.currentTimeMillis()
    }

    fun checkBackgroundLocationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasForegroundLocationPermission =
                ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            if (hasForegroundLocationPermission && shouldAskForBackgroundPermission()) {
                val hasBackgroundLocationPermission = ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasBackgroundLocationPermission) {
                    // handle location update
                } else {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        REQUEST_CODE_BACKGROUND
                    )
                    backgroundPermissionsWereAsked()
                }
            } else if (shouldAskForPermission()) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        if (shouldAskForBackgroundPermission())
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        else null
                    ), REQUEST_CODE_BACKGROUND
                )
                backgroundPermissionsWereAsked()
            }
        }
    }
}