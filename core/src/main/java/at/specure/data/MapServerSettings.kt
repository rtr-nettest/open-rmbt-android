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

private const val KEY_MAP_SERVER_HOST = "MAP_SERVER_HOST"
private const val KEY_MAP_SERVER_PORT = "MAP_SERVER_PORT"
private const val KEY_MAP_SERVER_USE_SSL = "MAP_SERVER_USE_SSL"
private const val KEY_MAP_SERVER_URL = "MAP_SERVER_URL"

@Singleton
class MapServerSettings @Inject constructor(context: Context) {

    private val preferences = context.getSharedPreferences("map_server_settings.pref", Context.MODE_PRIVATE)

    /**
     * if this is empty, you must build map server url from fields bellow
     */
    var mapServerUrl: String? = null
        set(value) {
            preferences.edit().putString(KEY_MAP_SERVER_URL, value).apply()
        }

    var mapServerHost: String? = null
        set(value) {
            preferences.edit().putString(KEY_MAP_SERVER_HOST, value).apply()
        }

    var mapServerPort: Int?
        get() {
            val value = preferences.getInt(KEY_MAP_SERVER_PORT, -1)
            return if (value == -1) null
            else value
        }
        set(value) {
            preferences.edit().putInt(KEY_MAP_SERVER_PORT, value ?: -1).apply()
        }

    var mapServerUseSsl: Boolean?
        get() {
            val value = preferences.getInt(KEY_MAP_SERVER_USE_SSL, -1)
            return when {
                (value == 0) -> false
                (value == 1) -> true
                else -> null
            }
        }
        set(value) {
            preferences.edit().putBoolean(KEY_MAP_SERVER_USE_SSL, value ?: false).apply()
        }

    init {
        mapServerUrl = preferences.getString(KEY_MAP_SERVER_URL, null)
        mapServerHost = preferences.getString(KEY_MAP_SERVER_HOST, null)
    }
}