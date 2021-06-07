package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.cms.client.CMSApiClient
import at.rtr.rmbt.android.config.CMSEndpointProviderImpl
import kotlinx.coroutines.launch

class StaticPageViewModel : BaseViewModel() {
    private val _endpoints = CMSEndpointProviderImpl()
    private val _contentLiveData = MutableLiveData<String?>()
    private val _api = CMSApiClient(_endpoints, _contentLiveData)

    val contentLiveData: LiveData<String?>
        get() {
            return _contentLiveData
        }

    fun getContent(endpoint: String) = launch {
        when (endpoint) {
            _endpoints.getPrivacyPolicyUrl -> _api.getPrivacyPolicy()
            _endpoints.getTermsOfUseUrl -> _api.getTermsOfUse()
            else -> _api.getAbout()
        }
    }
}