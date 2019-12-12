/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.rmbt.client.control

import at.rmbt.util.Maybe
import at.rmbt.util.exception.HandledException
import timber.log.Timber
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

private const val SOCKET_TIME_OUT_MS = 5000

class ControlServerClient @Inject constructor(private val endpointProvider: ControlEndpointProvider, private val api: ControlServerApi) {

    fun getPrivateIpV4Address() = getPrivateIpAddress(InetSocketAddress(endpointProvider.checkPrivateIPv4Host, endpointProvider.port), IpProtocol.V4)

    fun getPrivateIpV6Address() = getPrivateIpAddress(InetSocketAddress(endpointProvider.checkPrivateIPv6Host, endpointProvider.port), IpProtocol.V6)

    private fun getPrivateIpAddress(address: InetSocketAddress, protocol: IpProtocol): Maybe<IpInfoResponse> {
        return try {
            val socket = Socket()
            socket.connect(address, SOCKET_TIME_OUT_MS)
            val privateIp = socket.localAddress
            socket.close()
            Maybe(IpInfoResponse(protocol.intValue, privateIp.hostAddress))
        } catch (ex: Exception) {
            Timber.w(ex)
            Maybe(HandledException.from(ex))
        }
    }

    fun getPublicIpV4Address(body: IpRequestBody): Maybe<IpInfoResponse> {
        return api.ipCheck(endpointProvider.checkPublicIPv4Url, body).exec()
    }

    fun getPublicIpV6Address(body: IpRequestBody): Maybe<IpInfoResponse> {
        return api.ipCheck(endpointProvider.checkPublicIPv6Url, body).exec()
    }

    fun getSettings(body: SettingsRequestBody): Maybe<SettingsResponse> {
        return api.settingsCheck(endpointProvider.checkSettingsUrl, body).exec()
    }

    fun getTestSettings(body: TestRequestRequestBody): Maybe<TestRequestResponse> {
        return api.testRequest(endpointProvider.testRequestUrl, body).exec()
    }

    fun sendTestResults(body: TestResultBody): Maybe<BaseResponse> {
        return api.sendTestResult(endpointProvider.sendTestResultsUrl, body).exec()
    }

    fun getHistroty(): Maybe<BaseResponse> {
        return api.getHistory(endpointProvider.getHistoryUrl).exec()
    }
}