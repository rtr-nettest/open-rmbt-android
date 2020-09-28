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
import android.preference.PreferenceManager
import androidx.lifecycle.LiveData
import at.specure.util.StringPreferenceLiveData
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_CLIENT_UUID = "uuid"

@Singleton
class ClientUUIDLegacy @Inject constructor(context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val liveData: LiveData<String?>
        get() {
            return StringPreferenceLiveData(preferences, KEY_CLIENT_UUID, null)
                .also {
                    it.postValue(preferences.getString(KEY_CLIENT_UUID, null))
                }
        }

    var value: String?
        get() = _value
        set(a) {
            _value = a
            preferences.edit().putString(KEY_CLIENT_UUID, _value).apply()
        }

    private var _value: String? = null

    init {
        _value = preferences.getString(KEY_CLIENT_UUID, null)
    }
}