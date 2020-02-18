package at.rtr.rmbt.android.viewmodel

import at.specure.data.TermsAndConditions
import javax.inject.Inject

class SplashViewModel @Inject constructor(private val tac: TermsAndConditions) : BaseViewModel() {

    val tacAcceptanceLiveData = tac.tacAcceptanceLiveData

    fun updateTermsAcceptance(accepted: Boolean) {
        tac.tacAccepted = accepted
    }
}