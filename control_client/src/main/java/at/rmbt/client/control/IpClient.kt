package at.rmbt.client.control

import android.net.ConnectivityManager
import android.net.Network
import at.rmbt.util.Maybe
import at.rmbt.util.exception.HandledException
import at.rmbt.util.exception.NoConnectionException
import com.google.gson.Gson
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
            Maybe<IpInfoResponse>(NoConnectionException())
        }
    }

    fun getPublicIpV6Address(body: IpRequestBody, network: Network): Maybe<IpInfoResponse> {
        return api.ipCheck(endpoint.checkPublicIPv6Url, body).exec(true)
    }
}