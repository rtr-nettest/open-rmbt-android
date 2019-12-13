package at.rmbt.client.control

/**
 * An interface that required by [IpClient] and provides information about routes of control server for ip checks
 */
interface IpEndpointProvider {

    /**
     * Port that should be used for control server client
     */
    val port: Int

    /**
     * Url to the host for IPv4 test, example "v4.myhost.com"
     */
    val checkPrivateIPv4Host: String

    /**
     * Url to the host for IPv6 test, example "v6.myhost.com"
     */
    val checkPrivateIPv6Host: String

    /**
     * Link to check public IPv4 address, example "v4.myhost.com/ControlServer/V2/ip
     */
    val checkPublicIPv4Url: String

    /**
     * Link to check public IPv6 address, example "v6.myhost.com/ControlServer/V2/ip
     */
    val checkPublicIPv6Url: String
}