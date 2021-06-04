package at.rmbt.cms.client

import androidx.lifecycle.MutableLiveData

class CMSApiClient(private val _endpointProvider: CMSEndpointProvider) {
    private val _api = CMSApiBuilder(_endpointProvider).build()

    fun getAbout(liveData: MutableLiveData<String>) {
        return _api.getPage(_endpointProvider.getAboutUrl).enqueue(PageRequestCallback(liveData))
    }
    fun getPrivacyPolicy(liveData: MutableLiveData<String>) {
        return _api.getPage(_endpointProvider.getPrivacyPolicyUrl).enqueue(PageRequestCallback(liveData))
    }

    fun getTermsOfUse(liveData: MutableLiveData<String>) {
        return _api.getPage(_endpointProvider.getTermsOfUseUrl).enqueue(PageRequestCallback(liveData))
    }
}