package at.rmbt.cms.client

import android.content.Context
import androidx.lifecycle.MutableLiveData

class CMSApiClient(
    private val _endpointProvider: CMSEndpointProvider,
    private val _liveData: MutableLiveData<String>
    ) {
    private val _api = CMSApiBuilder(_endpointProvider).build()

    fun getAbout(context: Context) {
        return _api.getPage(_endpointProvider.getAboutUrl).enqueue(PageRequestCallback(context, _liveData))
    }
    fun getPrivacyPolicy(context: Context) {
        return _api.getPage(_endpointProvider.getPrivacyPolicyUrl).enqueue(PageRequestCallback(context, _liveData))
    }

    fun getTermsOfUse(context: Context) {
        return _api.getPage(_endpointProvider.getTermsOfUseUrl).enqueue(PageRequestCallback(context, _liveData))
    }
}