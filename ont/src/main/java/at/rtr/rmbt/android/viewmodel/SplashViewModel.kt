package at.rtr.rmbt.android.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.config.AppConfig
import at.specure.data.TermsAndConditions
import at.specure.util.permission.PermissionsWatcher
import javax.inject.Inject

class SplashViewModel @Inject constructor(
    private val context: Context,
    private val tac: TermsAndConditions,
    private val appConfig: AppConfig,
    private val permissionsWatcher: PermissionsWatcher
) : BaseViewModel() {

    val tacAcceptanceLiveData = tac.tacAcceptanceLiveData

    fun updateTermsAcceptance(accepted: Boolean) {
        tac.tacAccepted = accepted
    }

    fun isTacAccepted(): Boolean {
        return tac.tacAccepted
    }

    fun shouldAskForPermission(): Boolean {
        return (appConfig.lastPermissionAskedTimestampMillis + askPermissionsAgainTimesMillis) < System.currentTimeMillis() &&
                permissionsWatcher.requiredPermissions.any {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_DENIED
                }
    }
}