package at.rmbt.cms.client

interface CMSEndpointProvider {

    val hostname: String

    val getAboutUrl: String

    val getPrivacyPolicyUrl: String

    val getTermsOfUseUrl: String

    /**
     * Value for X-Nettest-Client header, do not include header if empty
     */
    val getNettestHeaderValue: String
}