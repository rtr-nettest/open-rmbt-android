package at.rtr.rmbt.android.config

import at.rmbt.cms.client.CMSEndpointProvider
import at.rtr.rmbt.android.BuildConfig

class CMSEndpointProviderImpl : CMSEndpointProvider {
    override val baseUrl: String
        get() = BuildConfig.CMS_URL.value
    override val getAboutUrl: String
        get() = "about"
    override val getPrivacyPolicyUrl: String
        get() = "privacy-policy"
    override val getTermsOfUseUrl: String
        get() = "terms-of-use"
    override val getNettestHeaderValue: String
        get() = BuildConfig.REQUEST_HEADER_VALUE.value
}