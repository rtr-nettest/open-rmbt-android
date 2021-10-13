package at.rtr.rmbt.android.config

import at.rmbt.cms.client.CMSEndpointProvider
import at.rtr.rmbt.android.BuildConfig

class CMSEndpointProviderImpl : CMSEndpointProvider {
    override val baseUrl: String
        get() = BuildConfig.CMS_URL.value
    override val getAboutUrl: String
        get() = "rreth"
    override val getPrivacyPolicyUrl: String
        get() = "politika-e-privatesise"
    override val getTermsOfUseUrl: String
        get() = "kushtet-e-sherbimit"
    override val getNettestHeaderValue: String
        get() = BuildConfig.REQUEST_HEADER_VALUE.value
}