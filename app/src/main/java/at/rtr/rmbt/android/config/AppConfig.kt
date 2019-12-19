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
        set(value) = setBoolean(BuildConfig.SKIP_QOS_TESTS, value)

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
        get() = getString(BuildConfig.CONTROL_SERVER_HOST)
        set(value) = setString(BuildConfig.CONTROL_SERVER_HOST, value)

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
}