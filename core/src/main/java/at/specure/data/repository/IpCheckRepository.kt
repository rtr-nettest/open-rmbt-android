package at.specure.data.repository

import at.rmbt.client.control.IpInfoResponse
import at.rmbt.util.Maybe

interface IpCheckRepository {

    fun getPublicIpV4Address(): Maybe<IpInfoResponse>

    fun getPublicIpV6Address(): Maybe<IpInfoResponse>

    fun getPrivateIpV4Address(): Maybe<IpInfoResponse>

    fun getPrivateIpV6Address(): Maybe<IpInfoResponse>
}