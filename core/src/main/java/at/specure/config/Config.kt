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
     * End of the url for getting settings from the control server, example "ControlServer/settings"
     */
    var controlServerSettingsPath: String

    /**
     * End of the url for getting new test settings from the control server, example "ControlServer/testRequest"
     */
    var controlServerRequestTestPath: String

    /**
     * Counter of tests performed by user
     */
    var testCounter: Int

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
}