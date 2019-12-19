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

class ControlServerProviderImpl(private val config: Config) : ControlEndpointProvider {

    private val protocol = if (config.controlServerUseSSL) "https://" else "http://"

    private val routePath = if (route.isEmpty()) "" else "/$route"

    override val host: String
        get() = protocol + config.controlServerHost

    override val checkSettingsUrl: String
        get() = "$host$routePath/${config.controlServerSettingsEndpoint}"

    override val testRequestUrl: String
        get() = "$host$routePath/${config.controlServerRequestTestEndpoint}"

    override val sendTestResultsUrl: String
        get() = "$host$routePath/${config.controlServerSendResultEndpoint}"

    override val getHistoryUrl: String
        get() = "$host$routePath/${config.controlServerHistoryEndpoint}"

    override val route: String
        get() = config.controlServerRoute

    override val getTestResultsBasicUrl: String
        get() = "$host$routePath/${config.controlServerResultsBasicPath}"

    override val getTestResultsOpenDataUrl: String
        get() = "$host$routePath/${config.controlServerResultsOpenDataPath}"

    override val port: Int
        get() = config.controlServerPort
}