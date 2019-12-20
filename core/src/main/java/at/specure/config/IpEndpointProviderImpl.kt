package at.specure.config

import at.rmbt.client.control.IpEndpointProvider

class IpEndpointProviderImpl(private val config: Config) : IpEndpointProvider {

    private val protocol = if (config.controlServerUseSSL) "https://" else "http://"

    override val port: Int
        get() = config.controlServerPort

    override val checkPrivateIPv4Host: String
        get() = config.controlServerCheckPrivateIPv4Host

    override val checkPrivateIPv6Host: String
        get() = config.controlServerCheckPrivateIPv6Host

    override val checkPublicIPv4Url: String
        get() = protocol + config.controlServerCheckPublicIPv4Url

    override val checkPublicIPv6Url: String
        get() = protocol + config.controlServerCheckPublicIPv6Url
}