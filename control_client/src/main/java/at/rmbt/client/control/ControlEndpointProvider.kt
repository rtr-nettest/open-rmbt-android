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

/**
 * An interface that required by [ControlServerClient] and provides information about routes of control server
 */
interface ControlEndpointProvider {

    /**
     * Control server host, example "myhost.com"
     */
    val host: String

    /**
     * Route to the control server, example for value "RMBTControlServer",
     * "myhost.com/RMBTControlServer/endpoint" will be used for requests to ControlServer
     */
    val route: String

    /**
     * Link suffix to check settings, example "ControlServer/V2/settings
     */
    val checkSettingsUrl: String

    /**
     * Link suffix to check basic test settings, example "ControlServer/V2/testRequest
     */
    val testRequestUrl: String

    /**
     * Url to send test results
     */
    val sendTestResultsUrl: String

    /**
     * Url to get history records from server
     */
    val getHistoryUrl: String

    /**
     * Url to get basic test results
     */
    val getTestResultsBasicUrl: String

    /**
     * Url to get basic test results
     */
    val getTestResultsOpenDataUrl: String

    /**
     * Port that should be used for control server client
     */
    val port: Int
}