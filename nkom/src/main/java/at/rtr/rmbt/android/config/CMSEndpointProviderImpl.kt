package at.rtr.rmbt.android.config

import at.rmbt.cms.client.CMSEndpointProvider
import at.rtr.rmbt.android.BuildConfig

class CMSEndpointProviderImpl : CMSEndpointProvider {
    override val baseUrl: String
        get() = BuildConfig.CMS_URL.value
    override val getAboutUrl: String
        get() = "om"
    override val getPrivacyPolicyUrl: String
        get() = "personvernerklaering"
    override val getTermsOfUseUrl: String
        get() = "brukervilkar"
    override val getNettestHeaderValue: String
        get() = BuildConfig.REQUEST_HEADER_VALUE.value
}