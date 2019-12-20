package at.rmbt.client.control

import at.rmbt.util.Maybe
import at.rmbt.util.exception.HandledException
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

private const val SOCKET_TIME_OUT_MS = 5000

class IpClient @Inject constructor(private val endpoint: IpEndpointProvider, private val api: IpApi) {

    fun getPrivateIpV4Address() = getPrivateIpAddress(InetSocketAddress(endpoint.checkPrivateIPv4Host, endpoint.port), IpProtocol.V4)

    fun getPrivateIpV6Address() = getPrivateIpAddress(InetSocketAddress(endpoint.checkPrivateIPv6Host, endpoint.port), IpProtocol.V6)

    private fun getPrivateIpAddress(address: InetSocketAddress, protocol: IpProtocol): Maybe<IpInfoResponse> {
        return try {
            val socket = Socket()
            socket.connect(address, SOCKET_TIME_OUT_MS)
            val privateIp = socket.localAddress
            socket.close()
            Maybe(IpInfoResponse(protocol.intValue, privateIp.hostAddress))
        } catch (ex: Exception) {
            Timber.w("Failed to get ip address: ${ex.message}")
            Maybe(HandledException.from(ex))
        }
    }

    fun getPublicIpV4Address(body: IpRequestBody): Maybe<IpInfoResponse> {
        return api.ipCheck(endpoint.checkPublicIPv4Url, body).exec(true)
    }

    fun getPublicIpV6Address(body: IpRequestBody): Maybe<IpInfoResponse> {
        return api.ipCheck(endpoint.checkPublicIPv6Url, body).exec(true)
    }
}