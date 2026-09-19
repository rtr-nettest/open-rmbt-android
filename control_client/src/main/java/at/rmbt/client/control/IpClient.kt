package at.rmbt.client.control

import android.net.ConnectivityManager
import android.net.Network
import at.rmbt.util.Maybe
import at.rmbt.util.exception.HandledException
import at.rmbt.util.exception.NoConnectionException
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.nio.charset.Charset
import javax.inject.Inject

private const val SOCKET_TIME_OUT_MS = 5000
private const val CONNECTION_TIME_OUT_MS = 10000
private const val READ_TIME_OUT_MS = 8000

class IpClient @Inject constructor(
    private val endpoint: IpEndpointProvider,
    private val api: IpApi,
    private val connectivityManager: ConnectivityManager
) {

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
            if (ex is CancellationException) {
                throw ex
            }
            Maybe(HandledException.from(ex))
        }
    }

    fun getPublicIpV4Address(body: IpRequestBody, network: Network): Maybe<IpInfoResponse> {
        return try {
            val connection = network.openConnection(URL(endpoint.checkPublicIPv4Url)) as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doInput = true
            connection.doOutput = true
            connection.connectTimeout = CONNECTION_TIME_OUT_MS
            connection.readTimeout = READ_TIME_OUT_MS
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")

            val gson = Gson()
            val input = gson.toJson(body)
            val writer = connection.outputStream.writer(Charset.forName("UTF-8"))
            writer.write(input)
            writer.flush()
            writer.close()

            val statusCode = connection.responseCode
            Timber.d("IPv4 status code: $statusCode")
            val output = connection.inputStream.bufferedReader().readText()
            val response = gson.fromJson(output, IpInfoResponse::class.java)
            Maybe(response)
        } catch (ex: Exception) {
            if (ex is CancellationException) {
                throw ex
            }
            Maybe<IpInfoResponse>(NoConnectionException())
        }
    }

    fun getPublicIpV6Address(body: IpRequestBody, network: Network): Maybe<IpInfoResponse> {
        return api.ipCheck(endpoint.checkPublicIPv6Url, body).exec(true)
    }

    /**
     * Network-bound public-IP lookups with an explicit (short) timeout, used by the coverage
     * measurement to poll the current public IP without long blocking. Both IPv4 and IPv6 go through
     * the given [network] (so they reflect the active/VPN route), and a blocked family simply fails
     * within [timeoutMs] (the caller ignores a failure - it is not treated as an IP change).
     */
    fun getPublicIpV4Address(body: IpRequestBody, network: Network, timeoutMs: Int): Maybe<IpInfoResponse> =
        getPublicIpAddressViaNetwork(endpoint.checkPublicIPv4Url, body, network, timeoutMs)

    fun getPublicIpV6Address(body: IpRequestBody, network: Network, timeoutMs: Int): Maybe<IpInfoResponse> =
        getPublicIpAddressViaNetwork(endpoint.checkPublicIPv6Url, body, network, timeoutMs)

    private fun getPublicIpAddressViaNetwork(
        url: String,
        body: IpRequestBody,
        network: Network,
        timeoutMs: Int
    ): Maybe<IpInfoResponse> {
        return try {
            val connection = network.openConnection(URL(url)) as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doInput = true
            connection.doOutput = true
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")

            val gson = Gson()
            connection.outputStream.writer(Charset.forName("UTF-8")).use { it.write(gson.toJson(body)) }
            val statusCode = connection.responseCode
            Timber.d("IP status code ($url): $statusCode")
            val output = connection.inputStream.bufferedReader().readText()
            Maybe(gson.fromJson(output, IpInfoResponse::class.java))
        } catch (ex: Exception) {
            if (ex is CancellationException) {
                throw ex
            }
            Maybe<IpInfoResponse>(NoConnectionException())
        }
    }
}