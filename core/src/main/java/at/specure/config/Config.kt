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

/**
 * An interface that contains all config values need by core module and other modules that depends of it
 */
interface Config {

    /**
     * Shows that NDT is enabled
     */
    var NDTEnabled: Boolean

    /**
     * Show that QoS tests can be skipped
     */
    var skipQoSTests: Boolean

    /**
     * User can menage location settings from the settings menu by opening application or system settings
     */
    var canManageLocationSettings: Boolean

    /**
     * Loop mode is enabled
     */
    var loopModeEnabled: Boolean

    /**
     * Time waiting while loop mode is enabled [loopModeEnabled] in minutes
     */
    var loopModeWaitingTimeMin: Int

    /**
     * Distance triggering in loop mode while [loopModeEnabled] in meters
     */
    var loopModeDistanceMeters: Int

    /**
     * Shows an expert menu to the user if enabled in settings screen
     */
    var expertModeEnabled: Boolean

    /**
     * Allows to use IPv4 protocol for requests
     */
    var expertModeUseIpV4Only: Boolean

    /**
     * Allows to use "https://" when enabled otherwise "http://" should be used
     */
    var controlServerUseSSL: Boolean

    /**
     * Port that should be used for control server client
     */
    var controlServerPort: Int

    /**
     * Control server host, example "myhost.com"
     */
    var controlServerHost: String

    /**
     * Url to the host for IPv4 test, example "v4.myhost.com"
     */
    var controlServerCheckPrivateIPv4Host: String

    /**
     * Url to the host for IPv6 test, example "v6.myhost.com"
     */
    var controlServerCheckPrivateIPv6Host: String

    /**
     * Link to check public IPv4 address, example "v4.myhost.com/ControlServer/V2/ip
     */
    var controlServerCheckPublicIPv4Url: String

    /**
     * Link to check public IPv6 address, example "v6.myhost.com/ControlServer/V2/ip
     */
    var controlServerCheckPublicIPv6Url: String

    /**
     * Route to the control server, example for value "RMBTControlServer",
     * "myhost.com/RMBTControlServer/endpoint" will be used for requests to ControlServer
     */
    var controlServerRoute: String

    /**
     * End of the url for getting settings from the control server, example "settings"
     */
    var controlServerSettingsEndpoint: String

    /**
     * End of the url for getting new test settings from the control server, example "testRequest"
     */
    var controlServerRequestTestEndpoint: String

    /**
     * End of the url for sending test results to the control server, example "result"
     */
    var controlServerSendResultEndpoint: String

    /**
     * End of the url for sending QoS test results to the control server, example "resultQoS"
     */
    var controlServerSendQoSResultEndpoint: String

    /**
     * End of the url for receiving history items from the server, example "history"
     */
    var controlServerHistoryEndpoint: String

    /**
     * End of the url for getting basic results from the control server, example "ControlServer/testresult"
     */
    var controlServerResultsBasicPath: String

    /**
     * End of the url for getting opendata results from the control server, example "ControlServer/opendatas"
     */
    var controlServerResultsOpenDataPath: String

    /**
     * End of the url for getting test result details from the control server, example "ControlServer/testresultdetail"
     */
    var controlServerTestResultDetailsEndpoint: String

    /**
     * Counter of tests performed by user
     */
    var testCounter: Int

    /**
     * Status of the previous measurement test for the user, example "ERROR"
     */
    var previousTestStatus: String?

    /**
     * Client uses RMBTHttp if true, default should be false
     */
    var capabilitiesRmbtHttp: Boolean

    /**
     * if true the third state (=INFO) is supported, default should be false
     */
    var capabilitiesQosSupportsInfo: Boolean

    /**
     * number of intervals(classes) to classify measured values, default 4
     */
    var capabilitiesClassificationCount: Int

    /**
     * User is able to select servers
     */
    var userServerSelectionEnabled: Boolean

    /**
     * Is Developer Mode turned on
     */
    var developerModeIsEnabled: Boolean

    /**
     * Is Developer Mode is available to be turned on or not
     */
    var developerModeIsAvailable: Boolean

    /**
     * Is Override Control Server turned on
     */
    var controlServerOverrideEnabled: Boolean

    /**
     * Is SSL connection should be used for QoS Tests
     */
    var qosSSL: Boolean

    /**
     * Route to the map server, example for value "RMBTMapServer",
     * "myhost.com/RMBTMapServer/endpoint" will be used for requests to MapServer
     */
    var mapServerRoute: String

    /**
     * End of url to obtain markers, example "MapServer/V2/tiles/markers"
     */
    var mapMarkersEndpoint: String

    /**
     * End of url to obtain tiles for map screen, example "RMBTMapServer/tiles/{type}/{zoom}/{x}/{y}.png?map_options=all/download&statistical_method=0.5&period=180
     */
    var mapTilesEndpoint: String
}