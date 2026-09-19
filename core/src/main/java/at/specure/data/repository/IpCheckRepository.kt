package at.specure.data.repository

import android.net.Network
import at.rmbt.client.control.IpInfoResponse
import at.rmbt.util.Maybe

interface IpCheckRepository {

    fun getPublicIpV4Address(network: Network): Maybe<IpInfoResponse>

    fun getPublicIpV6Address(network: Network): Maybe<IpInfoResponse>

    /** Public IPv4 over [network] with an explicit request timeout (ms). */
    fun getPublicIpV4Address(network: Network, timeoutMs: Int): Maybe<IpInfoResponse>

    /** Public IPv6 over [network] with an explicit request timeout (ms). */
    fun getPublicIpV6Address(network: Network, timeoutMs: Int): Maybe<IpInfoResponse>

    fun getPrivateIpV4Address(): Maybe<IpInfoResponse>

    fun getPrivateIpV6Address(): Maybe<IpInfoResponse>
}