package at.rmbt.cms.client

import androidx.lifecycle.MutableLiveData

class CMSApiClient(
    private val _endpointProvider: CMSEndpointProvider,
    private val _liveData: MutableLiveData<String?>
    ) {
    private val _api = CMSApiBuilder(_endpointProvider).build()

    fun getAbout() {
        return _api.getPage(_endpointProvider.getAboutUrl).enqueue(PageRequestCallback(_liveData))
    }
    fun getPrivacyPolicy() {
        return _api.getPage(_endpointProvider.getPrivacyPolicyUrl).enqueue(PageRequestCallback(_liveData))
    }

    fun getTermsOfUse() {
        return _api.getPage(_endpointProvider.getTermsOfUseUrl).enqueue(PageRequestCallback(_liveData))
    }
}