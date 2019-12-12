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

    override val host: String
        get() = protocol + config.controlServerHost

    override val checkPrivateIPv4Host: String
        get() = config.controlServerCheckPrivateIPv4Host

    override val checkPrivateIPv6Host: String
        get() = config.controlServerCheckPrivateIPv6Host

    override val checkPublicIPv4Url: String
        get() = protocol + config.controlServerCheckPublicIPv4Url

    override val checkPublicIPv6Url: String
        get() = protocol + config.controlServerCheckPublicIPv6Url

    override val checkSettingsUrl: String
        get() = host + "/" + config.controlServerSettingsPath

    override val testRequestUrl: String
        get() = host + "/" + config.controlServerRequestTestPath

    override val sendTestResultsUrl: String
        get() = host + "/" + config.controlServerSendResultPath

    override val getHistoryUrl: String
        get() = host + "/" + config.controlServerHistoryPath
    override val port: Int
        get() = config.controlServerPort
}