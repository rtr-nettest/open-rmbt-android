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

package at.specure.config

import at.rmbt.client.control.ControlEndpointProvider
import at.specure.data.ControlServerSettings
import timber.log.Timber

class ControlServerProviderImpl(
    private val config: Config,
    private val controlServerSettings: ControlServerSettings,
) : ControlEndpointProvider {

    private val protocol = if (config.controlServerUseSSL) "https://" else "http://"

    private val routePath = if (route.isEmpty()) "" else "/$route"

    override val host: String
        get() = protocol + config.controlServerHost

    override val statisticsHost: String
        get() = controlServerSettings.statisticsMasterServerUrl!!

    override val checkSettingsUrl: String
        get() = "$host$routePath/${config.controlServerSettingsEndpoint}"

    override val testRequestUrl: String
        get() = "$host$routePath/${config.controlServerRequestTestEndpoint}"

    override val sendTestResultsUrl: String
        get() = "$host$routePath/${config.controlServerSendResultEndpoint}"

    override val sendQoSTestResultsUrl: String
        get() = "$host$routePath/${config.controlServerSendQoSResultEndpoint}"

    override val getHistoryUrl: String
        get() = "$host$routePath/${config.controlServerHistoryEndpoint}"

    override val route: String
        get() = config.controlServerRoute

    override val getTestResultsBasicUrl: String
        get() = "$host$routePath/${config.controlServerResultsBasicPath}"

    override val getTestResultsOpenDataUrl: String
        get() {
            val statisticHost = "$statisticsHost/${config.controlServerResultsOpenDataPath}"
            return statisticHost
        }

    override val getTestResultsDetailsUrl: String
        get() = "$host$routePath/${config.controlServerTestResultDetailsEndpoint}"

    override val getQosResultDetailsUrl: String
        get() = "$host$routePath/${config.controlServerQosTestResultDetailsEndpoint}"

    override val getSyncCodeUrl: String
        get() = "$host$routePath/${config.getSyncCodeRoute}"

    override val syncDevicesUrl: String
        get() = "$host$routePath/${config.syncDevicesRoute}"

    override val signalRequestUrl: String
        get() = "$host$routePath/${config.signalRequestRoute}"

    override val signalResultUrl: String
        get() = "$host$routePath/${config.signalResultRoute}"

    override val coverageRequestUrl: String
        get() = "$host$routePath/${config.coverageRequestRoute}"

    override val coverageResultUrl: String
        get() = "$host$routePath/${config.coverageResultRoute}"

    override val getNewsUrl: String
        get() = "$host$routePath/${config.controlServerNewsEndpoint}"

    override val getNettestHeaderValue: String
        get() = config.headerValue
}