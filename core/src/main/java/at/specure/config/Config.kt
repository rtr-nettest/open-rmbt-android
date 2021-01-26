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
     * Show that QoS tests can be skipped for predefined period
     */
    var skipQoSTestsForPeriod: Boolean

    /**
     * Defines the period for QoS tests should be skipped if @see{skipQoSTestsForPeriod} is set to "true"
     */
    var skipQoSTestsPeriodMinutes: Int

    /**
     * Holds information about last time the QoS test was executed in milliseconds
     */
    var lastQosTestExecutionTimestampMillis: Long

    /**
     * this is variable which we should check before we are trying to start Qos Tests
     */
    var shouldRunQosTest: Boolean

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
     * The number of measurements in the loop
     */
    var loopModeNumberOfTests: Int

    /**
     * Minimum distance to set between 2 loop measurements in meters
     */
    var loopModeMinDistanceMeters: Int

    /**
     * Maximum distance to set between 2 loop measurements in meters
     */
    var loopModeMaxDistanceMeters: Int

    /**
     * Minimum waiting time to set between 2 loop measurements in minutes
     */
    var loopModeMinWaitingTimeMin: Int

    /**
     * Maximum waiting time to set between 2 loop measurements in minutes
     */
    var loopModeMaxWaitingTimeMin: Int

    /**
     * Maximum number of test to execute in loop
     */
    var loopModeMaxTestsNumber: Int

    /**
     * Minimum number of test to execute in loop
     */
    var loopModeMinTestsNumber: Int

    /**
     * Duration of the signal measurement activity time after user enable it, in minutes
     */
    var signalMeasurementDurationMin: Int

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
     * End of the url for getting qos test result details from the control server, example "ControlServer/qostestresult"
     */
    var controlServerQosTestResultDetailsEndpoint: String

    /**
     * Url for checking captive portal
     */
    var captivePortalWalledGardenUrl: String

    /**
     * WebPage address to be redirected user after click on the contact item in settings
     */
    var aboutWebPageUrl: String

    /**
     * Email address to write on from contact item in settings
     */
    var aboutEmailAddress: String

    /**
     * Url to public github repository with app code
     */
    var aboutGithubRepositoryUrl: String

    /**
     * Counter of tests performed by user
     */
    var testCounter: Int

    /**
     * Status of the previous measurement test for the user, example "ERROR"
     */
    var previousTestStatus: String?

    /**
     * Client uses RMBTHttp if true, default should be true
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

    /**
     * Link suffix to obtain tiles for map screen, example "RMBTMapServer/tiles/{type}/{zoom}/{x}/{y}.png?map_options=all/download&statistical_method=0.5&period=180
     */
    var mapMarkerShowDetailsRoute: String

    /**
     * Endpoint to get sync code for current device
     */
    var getSyncCodeRoute: String

    /**
     * Endpoint to sync two devices
     */
    var syncDevicesRoute: String

    /**
     * Endpoint to get news
     */
    var controlServerNewsEndpoint: String

    /**
     * End of url to obtain map filters data, example "RMBTMapServer/tiles/info
     */
    var mapFilterInfoEndpoint: String

    /**
     * url of data privacy and terms of use
     */
    var dataPrivacyAndTermsUrl: String

    /**
     * Default (local stored) url to open terms of use for acceptance
     */
    var termsAcceptanceDefaultUrl: String

    /**
     * Is Override Map Server turned on
     */
    var mapServerOverrideEnabled: Boolean

    /**
     * Map server host, example "myhost.com"
     */
    var mapServerHost: String

    /**
     * Port that should be used for map server client
     */
    var mapServerPort: Int

    /**
     * Allows to use "https://" when enabled otherwise "http://" should be used
     */
    var mapServerUseSSL: Boolean

    /**
     * User can define tag for measurements, this tag is sent with results to control server, if it is null or empty, nothing is sent
     */
    var measurementTag: String?

    /**
     * End of url to perform signal measurement request
     */
    var signalRequestRoute: String

    /**
     * End of url to send signal measurement results
     */
    var signalResultRoute: String

    /**
     * Secret code to enable developer mode
     */
    var secretCodeDeveloperModeOn: String

    /**
     * Secret code to disable developer mode
     */
    var secretCodeDeveloperModeOff: String

    /**
     * Secret code to disable all special modes
     */
    var secretCodeAllModesOff: String

    /**
     * Allows to simulate 5G network in the developer mode
     */
    var developer5GSimulationEnabled: Boolean

    /**
     * This flag makes simulate 5G network visible in the developer mode or not
     */
    val developer5GSimulationAvailable: Boolean

    /**
     * Timestamp of last asked permissions System.currentTimeMillis()
     */
    val lastPermissionAskedTimestampMillis: Long

    /**
     * Timestamp of last asked for background location permission System.currentTimeMillis()
     */
    val lastBackgroundPermissionAskedTimestampMillis: Long

    /**
     * The URL of updated Server
     */
    var newServerURL: String
}