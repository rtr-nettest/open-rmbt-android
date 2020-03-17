package at.specure.data.repository

import android.net.Network
import at.rmbt.client.control.IpInfoResponse
import at.rmbt.util.Maybe

interface IpCheckRepository {

    fun getPublicIpV4Address(network: Network): Maybe<IpInfoResponse>

    fun getPublicIpV6Address(network: Network): Maybe<IpInfoResponse>

    fun getPrivateIpV4Address(): Maybe<IpInfoResponse>

    fun getPrivateIpV6Address(): Maybe<IpInfoResponse>
}