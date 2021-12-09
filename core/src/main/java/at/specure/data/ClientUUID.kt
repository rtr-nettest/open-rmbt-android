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
import androidx.lifecycle.LiveData
import at.specure.config.Config
import at.specure.util.StringPreferenceLiveData
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_CLIENT_UUID = "KEY_CLIENT_UUID"

@Singleton
class ClientUUID @Inject constructor(context: Context, val config: Config) {

    private val preferences = context.getSharedPreferences("client_uuid.pref", Context.MODE_PRIVATE)

    val liveData: LiveData<String?>
        get() {
            return if (config.expertModeEnabled && config.controlServerOverrideEnabled) {
                StringPreferenceLiveData(preferences, KEY_CLIENT_UUID + config.controlServerHost, null)
                    .also {
                        it.postValue(preferences.getString(KEY_CLIENT_UUID + config.controlServerHost, null))
                    }
            } else {
                StringPreferenceLiveData(preferences, KEY_CLIENT_UUID, null)
                    .also {
                        it.postValue(preferences.getString(KEY_CLIENT_UUID, null))
                    }
            }
        }

    var value: String?
        get() {
            return if (config.expertModeEnabled && config.controlServerOverrideEnabled) {
                preferences.getString(KEY_CLIENT_UUID + config.controlServerHost , _value)
            } else {
                _value
            }
        }
        set(a) {
            _value = a
            if (config.expertModeEnabled && config.controlServerOverrideEnabled) {
                preferences.edit().putString(KEY_CLIENT_UUID + config.controlServerHost, _value).apply()
            } else {
                preferences.edit().putString(KEY_CLIENT_UUID, _value).apply()
            }


        }

    private var _value: String? = null

    init {
        _value = preferences.getString(KEY_CLIENT_UUID, null)
    }
}