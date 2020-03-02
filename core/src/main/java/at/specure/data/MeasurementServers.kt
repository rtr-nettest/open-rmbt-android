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
import at.rmbt.client.control.Server
import com.google.gson.Gson
import timber.log.Timber
import javax.inject.Inject

private const val KEY_SELECTED_SERVER = "KEY_SELECTED_SERVER"
private const val KEY_MEASUREMENT_SERVERS = "KEY_MEASUREMENT_SERVERS"

class MeasurementServers @Inject constructor(context: Context) {

    private val preferences = context.getSharedPreferences("measurement_servers.pref", Context.MODE_PRIVATE)
    private val gson = Gson()

    var selectedMeasurementServer: Server?
        get() {
            val serverUUID = preferences.getString(KEY_SELECTED_SERVER, null)
            Timber.d("Servers uuid get selected: $serverUUID")
            if (serverUUID == null) {
                return null
            } else {
                measurementServers?.forEach { x ->
                    if (x.uuid.equals(serverUUID)) {
                        return x
                    }
                }
                return null
            }
        }
        set(value) {
            Timber.d("Server uuid selected: ${value?.uuid}")
            preferences.edit().putString(KEY_SELECTED_SERVER, value?.uuid).apply()
        }

    var measurementServers: List<Server>?
        get() {
            val servers = preferences.getString(KEY_MEASUREMENT_SERVERS, null)
            Timber.d("Servers loaded: $servers")
            return gson.fromJson<List<Server>>(servers, ArrayList::class.java)
        }
        set(value) {
            val servers = gson.toJson(value)
            Timber.d("Servers saved: $servers")
            preferences.edit().putString(KEY_MEASUREMENT_SERVERS, servers).apply()
        }
}