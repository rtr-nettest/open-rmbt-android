package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.cms.client.CMSApiClient
import at.rtr.rmbt.android.config.CMSEndpointProviderImpl
import at.specure.data.TermsAndConditions
import kotlinx.coroutines.launch
import javax.inject.Inject

class TermsAcceptanceViewModel @Inject constructor(private val tac: TermsAndConditions) :
    BaseViewModel() {

    private val _tacContentLiveData = MutableLiveData<String?>()
    private val _api = CMSApiClient(CMSEndpointProviderImpl(), _tacContentLiveData)

    val tacContentLiveData: LiveData<String?>
        get() {
            return _tacContentLiveData
        }

    fun getTac() = launch {
        _api.getTermsOfUse()
    }

    fun updateTermsAcceptance(accepted: Boolean) {
        tac.tacAccepted = accepted
    }
}