package at.rtr.rmbt.android.viewmodel

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.util.safeOffer
import at.specure.util.permission.PermissionsWatcher
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel

class PermissionsViewModel @Inject constructor(private val permissionsWatcher: PermissionsWatcher, private val appConfig: AppConfig) :
    BaseViewModel() {

    private var currentPage = PermissionsPage.WELCOME

    val permissionsPageChannel = Channel<PermissionsPage>(Channel.CONFLATED)
    val flowCompletedChannel = Channel<Unit>(Channel.CONFLATED)

    val accuracyPermissions = permissionsWatcher.requiredPermissions
        .filter { it == Manifest.permission.READ_PHONE_STATE || it == Manifest.permission.ACCESS_FINE_LOCATION || it == Manifest.permission.ACCESS_FINE_LOCATION }
        .toTypedArray()

    val signalPermissions = mutableListOf<String>().also {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasForegroundLocationPermission =
                ContextCompat.checkSelfPermission(
                    permissionsWatcher.context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            if (hasForegroundLocationPermission) {
                val hasBackgroundLocationPermission = ContextCompat.checkSelfPermission(
                    permissionsWatcher.context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasBackgroundLocationPermission) {
                    it.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            } else {
                it.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                it.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }.toTypedArray()

    fun shouldAskAccuracy() = accuracyPermissions.isNotEmpty()

    fun shouldAskSignal() = signalPermissions.isNotEmpty()

    fun permissionsWereAsked() {
        permissionsWatcher.notifyPermissionsUpdated()
        appConfig.lastPermissionAskedTimestampMillis = System.currentTimeMillis()
    }

    fun setAnalyticsSettings(enabled: Boolean) {
        appConfig.analyticsEnabled = enabled
    }

    fun enablePersistentClientId() {
        appConfig.persistentClientUUIDEnabled = true
    }

    fun moveToNext() {
        val newPage = when (currentPage) {
            PermissionsPage.WELCOME -> PermissionsPage.ACCURACY
            PermissionsPage.ACCURACY -> PermissionsPage.SIGNAL
            PermissionsPage.SIGNAL -> PermissionsPage.ANALYTICS
            PermissionsPage.ANALYTICS -> PermissionsPage.CLIENT_ID
            else -> PermissionsPage.COMPLETED
        }
        currentPage = newPage
        permissionsPageChannel.offer(currentPage)
    }

    fun finish() {
        flowCompletedChannel.safeOffer(Unit)
    }
}

enum class PermissionsPage {
    WELCOME, ACCURACY, SIGNAL, ANALYTICS, CLIENT_ID, COMPLETED
}