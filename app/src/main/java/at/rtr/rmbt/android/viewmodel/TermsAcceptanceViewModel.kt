package at.rtr.rmbt.android.viewmodel

import at.specure.data.TermsAndConditions
import javax.inject.Inject

class TermsAcceptanceViewModel @Inject constructor(tac: TermsAndConditions) : BaseViewModel() {

    val tacUrlLiveData = tac.tacUrlLiveData
}