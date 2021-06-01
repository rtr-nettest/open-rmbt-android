package at.rtr.rmbt.android.viewmodel

import at.rtr.rmbt.android.config.AppConfig
import at.specure.data.TermsAndConditions
import at.specure.util.permission.PermissionsWatcher
import javax.inject.Inject

class SplashViewModel @Inject constructor(
    private val tac: TermsAndConditions,
    private val appConfig: AppConfig,
    val permissionsWatcher: PermissionsWatcher
) : BaseViewModel() {

    val tacAcceptanceLiveData = tac.tacAcceptanceLiveData

    fun updateTermsAcceptance(accepted: Boolean) {
        tac.tacAccepted = accepted
    }

    fun isTacAccepted(): Boolean {
        return tac.tacAccepted
    }

    fun shouldAskForPermission(): Boolean {
        return (appConfig.lastPermissionAskedTimestampMillis + askPermissionsAgainTimesMillis) < System.currentTimeMillis()
    }
}