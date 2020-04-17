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

package at.rtr.rmbt.android.config

import android.content.Context
import at.rtr.rmbt.android.BuildConfig
import at.rtr.rmbt.android.util.ConfigValue
import at.specure.config.Config
import at.specure.data.ControlServerSettings
import javax.inject.Inject

private const val FILENAME = "config.pref"

private const val KEY_TEST_COUNTER = "KEY_TEST_COUNTER"
private const val KEY_PREVIOUS_TEST_STATUS = "PREVIOUS_TEST_STATUS"
private const val KEY_MEASUREMENT_TAG = "MEASUREMENT_TAG"
private const val KEY_LAST_QOS_TEST_PERFORMED_TIMESTAMP_MILLIS = "LAST_QOS_TEST_PERFORMED_TIMESTAMP_MILLIS"

class AppConfig @Inject constructor(context: Context, private val serverSettings: ControlServerSettings) : Config {

    private val preferences = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE)

    private fun getInt(configValue: ConfigValue, serverValue: Int? = null): Int {
        return preferences.getInt(configValue.name, serverValue ?: configValue.value.toInt())
    }

    private fun setInt(configValue: ConfigValue, value: Int) {
        preferences.edit()
            .putInt(configValue.name, value)
            .apply()
    }

    private fun getString(configValue: ConfigValue, serverValue: String? = null): String {
        return preferences.getString(configValue.name, serverValue ?: configValue.value)!!
    }

    private fun setString(configValue: ConfigValue, value: String) {
        preferences.edit()
            .putString(configValue.name, value)
            .apply()
    }

    private fun getBoolean(configValue: ConfigValue, serverValue: Boolean? = null): Boolean {
        return preferences.getBoolean(configValue.name, serverValue ?: configValue.value.toBoolean())
    }

    private fun setBoolean(configValue: ConfigValue, value: Boolean) {
        preferences.edit()
            .putBoolean(configValue.name, value)
            .apply()
    }

    override var NDTEnabled: Boolean
        get() = getBoolean(BuildConfig.NDT_ENABLED)
        set(value) = setBoolean(BuildConfig.NDT_ENABLED, value)

    override var skipQoSTests: Boolean
        get() = getBoolean(BuildConfig.SKIP_QOS_TESTS)
        set(value) {
            setBoolean(BuildConfig.SKIP_QOS_TESTS, value)
            lastQosTestExecutionTimestampMillis = 0
            skipQoSTestsForPeriod = false
        }

    override var skipQoSTestsForPeriod: Boolean
        get() = getBoolean(BuildConfig.SKIP_QOS_TESTS_FOR_PERIOD)
        set(value) = setBoolean(BuildConfig.SKIP_QOS_TESTS_FOR_PERIOD, value)

    override var skipQoSTestsPeriodMinutes: Int
        get() = getInt(BuildConfig.SKIP_QOS_TESTS_PERIOD_MIN)
        set(value) = setInt(BuildConfig.SKIP_QOS_TESTS_PERIOD_MIN, value)

    override var lastQosTestExecutionTimestampMillis: Long
        get() = preferences.getLong(KEY_LAST_QOS_TEST_PERFORMED_TIMESTAMP_MILLIS, 0)
        set(value) = preferences.edit()
            .putLong(KEY_LAST_QOS_TEST_PERFORMED_TIMESTAMP_MILLIS, value)
            .apply()

    override var shouldRunQosTest: Boolean
        get() = (!skipQoSTests && (!skipQoSTestsForPeriod || (skipQoSTestsForPeriod && (System.currentTimeMillis() - lastQosTestExecutionTimestampMillis >= skipQoSTestsPeriodMinutes * 60 * 1000))))
        set(value) {}

    override var canManageLocationSettings: Boolean
        get() = getBoolean(BuildConfig.CAN_MANAGE_LOCATION_SETTINGS)
        set(value) = setBoolean(BuildConfig.CAN_MANAGE_LOCATION_SETTINGS, value)

    override var loopModeEnabled: Boolean
        get() = getBoolean(BuildConfig.LOOP_MODE_ENABLED)
        set(value) = setBoolean(BuildConfig.LOOP_MODE_ENABLED, value)

    override var loopModeWaitingTimeMin: Int
        get() = getInt(BuildConfig.LOOP_MODE_WAITING_TIME_MIN)
        set(value) = setInt(BuildConfig.LOOP_MODE_WAITING_TIME_MIN, value)

    override var loopModeDistanceMeters: Int
        get() = getInt(BuildConfig.LOOP_MODE_DISTANCE_METERS)
        set(value) = setInt(BuildConfig.LOOP_MODE_DISTANCE_METERS, value)

    override var loopModeNumberOfTests: Int
        get() = getInt(BuildConfig.LOOP_MODE_NUMBER_OF_TESTS)
        set(value) = setInt(BuildConfig.LOOP_MODE_NUMBER_OF_TESTS, value)

    override var loopModeMinDistanceMeters: Int
        get() = getInt(BuildConfig.LOOP_MODE_MIN_DISTANCE_METERS)
        set(value) {
            // this value cannot be changed
        }

    override var loopModeMaxDistanceMeters: Int
        get() = getInt(BuildConfig.LOOP_MODE_MAX_DISTANCE_METERS)
        set(value) {
            // this value cannot be changed
        }

    override var loopModeMinWaitingTimeMin: Int
        get() = getInt(BuildConfig.LOOP_MODE_MIN_WAITING_TIME_MIN)
        set(value) {
            // this value cannot be changed
        }

    override var loopModeMaxWaitingTimeMin: Int
        get() = getInt(BuildConfig.LOOP_MODE_MAX_WAITING_TIME_MIN)
        set(value) {
            // this value cannot be changed
        }

    override var loopModeMaxTestsNumber: Int
        get() = getInt(BuildConfig.LOOP_MODE_MAX_NUMBER_OF_TESTS)
        set(value) {
            // this value cannot be changed
        }

    override var loopModeMinTestsNumber: Int
        get() = getInt(BuildConfig.LOOP_MODE_MIN_NUMBER_OF_TESTS)
        set(value) {
            // this value cannot be changed
        }

    override var expertModeEnabled: Boolean
        get() = getBoolean(BuildConfig.EXPERT_MODE_ENABLED)
        set(value) = setBoolean(BuildConfig.EXPERT_MODE_ENABLED, value)

    override var expertModeUseIpV4Only: Boolean
        get() = getBoolean(BuildConfig.EXPERT_MODE_IPV4_ONLY)
        set(value) = setBoolean(BuildConfig.EXPERT_MODE_IPV4_ONLY, value)

    override var controlServerUseSSL: Boolean
        get() = getBoolean(BuildConfig.CONTROL_SERVER_USE_SSL)
        set(value) = setBoolean(BuildConfig.CONTROL_SERVER_USE_SSL, value)

    override var controlServerPort: Int
        get() = getInt(BuildConfig.CONTROL_SERVER_PORT)
        set(value) = setInt(BuildConfig.CONTROL_SERVER_PORT, value)

    override var controlServerHost: String
        get() {
            return if (expertModeEnabled && expertModeUseIpV4Only && serverSettings.controlServerV4Url != null) {
                serverSettings.controlServerV4Url!!
            } else {
                getString(BuildConfig.CONTROL_SERVER_HOST)
            }
        }
        set(value) = setString(BuildConfig.CONTROL_SERVER_HOST, value)

    override var measurementTag: String?
        get() {
            return if (developerModeIsEnabled && developerModeIsAvailable) {
                preferences.getString(KEY_MEASUREMENT_TAG, null)
            } else {
                return null
            }
        }
        set(value) = preferences.edit()
            .putString(KEY_MEASUREMENT_TAG, value)
            .apply()

    override var controlServerCheckPrivateIPv4Host: String
        get() = getString(BuildConfig.CONTROL_SERVER_CHECK_PRIVATE_IPV4_HOST, serverSettings.controlServerV4Url)
        set(value) = setString(BuildConfig.CONTROL_SERVER_CHECK_PRIVATE_IPV4_HOST, value)

    override var controlServerCheckPrivateIPv6Host: String
        get() = getString(BuildConfig.CONTROL_SERVER_CHECK_PRIVATE_IPV6_HOST, serverSettings.controlServerV6Url)
        set(value) = setString(BuildConfig.CONTROL_SERVER_CHECK_PRIVATE_IPV6_HOST, value)

    override var controlServerCheckPublicIPv4Url: String
        get() = getString(BuildConfig.CONTROL_SERVER_CHECK_PUBLIC_IPV4_URL, serverSettings.ipV4CheckUrl)
        set(value) = setString(BuildConfig.CONTROL_SERVER_CHECK_PUBLIC_IPV4_URL, value)

    override var controlServerCheckPublicIPv6Url: String
        get() = getString(BuildConfig.CONTROL_SERVER_CHECK_PUBLIC_IPV6_URL, serverSettings.ipV6CheckUrl)
        set(value) = setString(BuildConfig.CONTROL_SERVER_CHECK_PUBLIC_IPV6_URL, value)

    override var controlServerRoute: String
        get() = getString(BuildConfig.CONTROL_SERVER_ROUTE)
        set(value) = setString(BuildConfig.CONTROL_SERVER_ROUTE, value)

    override var controlServerSettingsEndpoint: String
        get() = getString(BuildConfig.CONTROL_SERVER_SETTINGS_ENDPOINT)
        set(value) = setString(BuildConfig.CONTROL_SERVER_SETTINGS_ENDPOINT, value)

    override var controlServerRequestTestEndpoint: String
        get() = getString(BuildConfig.CONTROL_SERVER_TEST_REQUEST_ENDPOINT)
        set(value) = setString(BuildConfig.CONTROL_SERVER_TEST_REQUEST_ENDPOINT, value)

    override var controlServerSendResultEndpoint: String
        get() = getString(BuildConfig.CONTROL_SERVER_SEND_RESULT_ENDPOINT)
        set(value) = setString(BuildConfig.CONTROL_SERVER_SEND_RESULT_ENDPOINT, value)

    override var controlServerSendQoSResultEndpoint: String
        get() = getString(BuildConfig.CONTROL_SERVER_SEND_QOS_RESULT_ENDPOINT)
        set(value) = setString(BuildConfig.CONTROL_SERVER_SEND_QOS_RESULT_ENDPOINT, value)

    override var controlServerHistoryEndpoint: String
        get() = getString(BuildConfig.CONTROL_SERVER_HISTORY_ENDPOINT)
        set(value) = setString(BuildConfig.CONTROL_SERVER_HISTORY_ENDPOINT, value)

    override var controlServerResultsBasicPath: String
        get() = getString(BuildConfig.CONTROL_SERVER_GET_BASIC_RESULT_PATH)
        set(value) = setString(BuildConfig.CONTROL_SERVER_GET_BASIC_RESULT_PATH, value)

    override var controlServerResultsOpenDataPath: String
        get() = getString(BuildConfig.CONTROL_SERVER_GET_OPENDATA_RESULT_PATH)
        set(value) = setString(BuildConfig.CONTROL_SERVER_GET_OPENDATA_RESULT_PATH, value)

    override var controlServerTestResultDetailsEndpoint: String
        get() = getString(BuildConfig.CONTROL_SERVER_TEST_RESULT_DETAILS_ENDPOINT)
        set(value) = setString(BuildConfig.CONTROL_SERVER_TEST_RESULT_DETAILS_ENDPOINT, value)

    override var controlServerQosTestResultDetailsEndpoint: String
        get() = getString(BuildConfig.CONTROL_SERVER_GET_QOS_TEST_RESULT_ENDPOINT)
        set(value) = setString(BuildConfig.CONTROL_SERVER_GET_QOS_TEST_RESULT_ENDPOINT, value)

    override var controlServerNewsEndpoint: String
        get() = getString(BuildConfig.CONTROL_SERVER_GET_NEWS_ENDPOINT)
        set(value) = setString(BuildConfig.CONTROL_SERVER_GET_NEWS_ENDPOINT, value)

    override var testCounter: Int
        get() = preferences.getInt(KEY_TEST_COUNTER, 0)
        set(value) = preferences.edit()
            .putInt(KEY_TEST_COUNTER, value)
            .apply()

    override var previousTestStatus: String? // can be null before first test
        get() = preferences.getString(KEY_PREVIOUS_TEST_STATUS, null)
        set(value) = preferences.edit()
            .putString(KEY_PREVIOUS_TEST_STATUS, value)
            .apply()

    override var capabilitiesRmbtHttp: Boolean
        get() = getBoolean(BuildConfig.CAPABILITIES_RMBT_HTTP)
        set(value) = setBoolean(BuildConfig.CAPABILITIES_RMBT_HTTP, value)

    override var captivePortalWalledGardenUrl: String
        get() = getString(BuildConfig.CAPTIVE_PORTAL_WALLED_GARDEN_URL)
        set(value) = setString(BuildConfig.CAPTIVE_PORTAL_WALLED_GARDEN_URL, value)

    override var aboutWebPageUrl: String
        get() = getString(BuildConfig.WEBSITE_URL)
        set(value) = setString(BuildConfig.WEBSITE_URL, value)

    override var aboutEmailAddress: String
        get() = getString(BuildConfig.EMAIL_ADDRESS)
        set(value) = setString(BuildConfig.EMAIL_ADDRESS, value)

    override var aboutGithubRepositoryUrl: String
        get() = getString(BuildConfig.SOURCE_CODE_URL)
        set(value) = setString(BuildConfig.SOURCE_CODE_URL, value)

    override var capabilitiesQosSupportsInfo: Boolean
        get() = getBoolean(BuildConfig.CAPABILITIES_QOS_SUPPORTS_INFO)
        set(value) = setBoolean(BuildConfig.CAPABILITIES_QOS_SUPPORTS_INFO, value)

    override var capabilitiesClassificationCount: Int
        get() = getInt(BuildConfig.CAPABILITIES_CLASSIFICATION_COUNT)
        set(value) = setInt(BuildConfig.CAPABILITIES_CLASSIFICATION_COUNT, value)

    override var userServerSelectionEnabled: Boolean
        get() = getBoolean(BuildConfig.USER_SERVER_SELECTION_ENABLED)
        set(value) = setBoolean(BuildConfig.USER_SERVER_SELECTION_ENABLED, value)

    override var developerModeIsEnabled: Boolean
        get() = getBoolean(BuildConfig.DEVELOPER_MODE_IS_ENABLED)
        set(value) = setBoolean(BuildConfig.DEVELOPER_MODE_IS_ENABLED, value)

    override var developerModeIsAvailable: Boolean
        get() = getBoolean(BuildConfig.DEVELOPER_MODE_IS_AVAILABLE)
        set(value) {
            // this value cannot be changed
        }

    override var controlServerOverrideEnabled: Boolean
        get() = getBoolean(BuildConfig.IS_CONTROL_SERVER_OVERRIDE_ENABLED)
        set(value) = setBoolean(BuildConfig.IS_CONTROL_SERVER_OVERRIDE_ENABLED, value)

    override var qosSSL: Boolean
        get() = getBoolean(BuildConfig.QOS_SSL)
        set(value) = setBoolean(BuildConfig.QOS_SSL, value)

    override var mapServerRoute: String
        get() = getString(BuildConfig.MAP_SERVER_ROUTE)
        set(value) = setString(BuildConfig.MAP_SERVER_ROUTE, value)

    override var mapMarkersEndpoint: String
        get() = getString(BuildConfig.MAP_MARKERS_ENDPOINT)
        set(value) = setString(BuildConfig.MAP_MARKERS_ENDPOINT, value)

    override var mapTilesEndpoint: String
        get() = getString(BuildConfig.MAP_TILES_ENDPOINT)
        set(value) = setString(BuildConfig.MAP_TILES_ENDPOINT, value)

    override var mapMarkerShowDetailsRoute: String
        get() = getString(BuildConfig.MAP_MARKER_SHOW_DETAILS_ROUTE)
        set(value) = setString(BuildConfig.MAP_MARKER_SHOW_DETAILS_ROUTE, value)

    override var getSyncCodeRoute: String
        get() = getString(BuildConfig.CONTROL_SERVER_GET_SYNC_CODE_ROUTE)
        set(value) = setString(BuildConfig.CONTROL_SERVER_GET_SYNC_CODE_ROUTE, value)

    override var syncDevicesRoute: String
        get() = getString(BuildConfig.CONTROL_SERVER_SYNC_DEVICES_ROUTE)
        set(value) = setString(BuildConfig.CONTROL_SERVER_SYNC_DEVICES_ROUTE, value)

    override var mapFilterInfoEndpoint: String
        get() = getString(BuildConfig.MAP_FILTERS_ENDPOINT)
        set(value) = setString(BuildConfig.MAP_FILTERS_ENDPOINT, value)

    override var dataPrivacyAndTermsUrl: String
        get() = getString(BuildConfig.DATA_PRIVACY_AND_TERMS_URL)
        set(value) = setString(BuildConfig.DATA_PRIVACY_AND_TERMS_URL, value)

    override var signalRequestRoute: String
        get() = getString(BuildConfig.CONTROL_SERVER_SIGNAL_REQUEST_ROUTE)
        set(value) = setString(BuildConfig.CONTROL_SERVER_SIGNAL_REQUEST_ROUTE, value)

    override var signalResultRoute: String
        get() = getString(BuildConfig.CONTROL_SERVER_SIGNAL_RESULT_ROUTE)
        set(value) = setString(BuildConfig.CONTROL_SERVER_SIGNAL_RESULT_ROUTE, value)

    override var secretCodeDeveloperModeOn: String
        get() = getString(BuildConfig.DEVELOPER_ACTIVATE_CODE)
        set(value) {
            // this value cannot be changed
        }

    override var secretCodeDeveloperModeOff: String
        get() = getString(BuildConfig.DEVELOPER_DEACTIVATE_CODE)
        set(value) {
            // this value cannot be changed
        }

    override var secretCodeAllModesOff: String
        get() = getString(BuildConfig.ALL_DEACTIVATE_CODE)
        set(value) {
            // this value cannot be changed
        }

    override var termsAcceptanceDefaultUrl: String
        get() = getString(BuildConfig.TERMS_FOR_ACCEPTANCE_URL)
        set(value) = setString(BuildConfig.TERMS_FOR_ACCEPTANCE_URL, value)

    override var mapServerOverrideEnabled: Boolean
        get() = getBoolean(BuildConfig.IS_MAP_SERVER_OVERRIDE_ENABLED)
        set(value) = setBoolean(BuildConfig.IS_MAP_SERVER_OVERRIDE_ENABLED, value)

    override var mapServerHost: String
        get() = getString(BuildConfig.MAP_SERVER_HOST)
        set(value) = setString(BuildConfig.MAP_SERVER_HOST, value)

    override var mapServerPort: Int
        get() = getInt(BuildConfig.MAP_SERVER_PORT)
        set(value) = setInt(BuildConfig.MAP_SERVER_PORT, value)

    override var mapServerUseSSL: Boolean
        get() = getBoolean(BuildConfig.MAP_SERVER_USE_SSL)
        set(value) = setBoolean(BuildConfig.MAP_SERVER_USE_SSL, value)

    override var developer5GSimulationEnabled: Boolean
        get() = getBoolean(BuildConfig.DEV_SIMULATE_5G_NETWORK)
        set(value) = setBoolean(BuildConfig.DEV_SIMULATE_5G_NETWORK, value)

    override val developer5GSimulationAvailable: Boolean
        get() = getBoolean(BuildConfig.DEVELOPER_MODE_IS_AVAILABLE)
}