package at.rtr.rmbt.android.config

import at.rmbt.cms.client.CMSEndpointProvider

class CMSEndpointProviderImpl : CMSEndpointProvider {
    override val hostname: String
        get() = "https://portal-api.nettest.org"
    override val getPrivacyPolicyUrl: String
        get() = "politika-e-privatesise"
    override val getTermsOfUseUrl: String
        get() = "kushtet-e-sherbimit"
    override val getNettestHeaderValue: String
        get() = "al"
}