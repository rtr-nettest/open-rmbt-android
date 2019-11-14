/*
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.specure.data

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_CONTROL_SERVER_URL = "CONTROL_SERVER_URL"
private const val KEY_CONTROL_V4_SERVER_URL = "CONTROL_V4_SERVER_URL"
private const val KEY_CONTROL_V6_SERVER_URL = "CONTROL_V6_SERVER_URL"
private const val KEY_CONTROL_IPV4_CHECK_URL = "CONTROL_IPV4_CHECK_URL"
private const val KEY_CONTROL_IPV6_CHECK_URL = "CONTROL_IPV6_CHECK_URL"
private const val KEY_OPEN_DATA_PREFIX = "OPEN_DATA_PREFIX"
private const val KEY_STATISTIC_URL = "STATISTIC_URL"
private const val KEY_SHARE_URL = "SHARE_URL"
private const val KEY_CONTROL_SERVER_VERSION = "KEY_CONTROL_SERVER_VERSION"

@Singleton
class ControlServerSettings @Inject constructor(context: Context) {

    private val preferences = context.getSharedPreferences("server_settings.pref", Context.MODE_PRIVATE)

    var controlServerUrl: String? = null
        set(value) {
            preferences.edit().putString(KEY_CONTROL_SERVER_URL, value).apply()
        }

    var controlServerV4Url: String? = null
        set(value) {
            preferences.edit().putString(KEY_CONTROL_V4_SERVER_URL, value).apply()
        }

    var controlServerV6Url: String? = null
        set(value) {
            preferences.edit().putString(KEY_CONTROL_V6_SERVER_URL, value).apply()
        }

    var ipV4CheckUrl: String? = null
        set(value) {
            preferences.edit().putString(KEY_CONTROL_IPV4_CHECK_URL, value).apply()
        }

    var ipV6CheckUrl: String? = null
        set(value) {
            preferences.edit().putString(KEY_CONTROL_IPV6_CHECK_URL, value).apply()
        }

    var openDataPrefix: String? = null
        set(value) {
            preferences.edit().putString(KEY_OPEN_DATA_PREFIX, value).apply()
        }

    var statisticsUrl: String? = null
        set(value) {
            preferences.edit().putString(KEY_STATISTIC_URL, value).apply()
        }

    var shareUrl: String? = null
        set(value) {
            preferences.edit().putString(KEY_SHARE_URL, value).apply()
        }

    var controlServerVersion: String? = null
        set(value) {
            preferences.edit().putString(KEY_CONTROL_SERVER_VERSION, value).apply()
        }

    init {
        controlServerUrl = preferences.getString(KEY_CONTROL_SERVER_URL, null)
        controlServerV4Url = preferences.getString(KEY_CONTROL_V4_SERVER_URL, null)
        controlServerV6Url = preferences.getString(KEY_CONTROL_V6_SERVER_URL, null)
        ipV4CheckUrl = preferences.getString(KEY_CONTROL_IPV4_CHECK_URL, null)
        ipV6CheckUrl = preferences.getString(KEY_CONTROL_IPV6_CHECK_URL, null)
        openDataPrefix = preferences.getString(KEY_OPEN_DATA_PREFIX, null)
        statisticsUrl = preferences.getString(KEY_STATISTIC_URL, null)
        shareUrl = preferences.getString(KEY_SHARE_URL, null)
        controlServerVersion = preferences.getString(KEY_CONTROL_SERVER_VERSION, null)
    }
}