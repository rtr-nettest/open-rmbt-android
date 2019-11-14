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
import at.specure.util.StringSetPreferenceLiveData
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_HISTORY_FILTER_DEVICES_OPTIONS = "KEY_HISTORY_FILTER_DEVICES_OPTIONS"
private const val KEY_HISTORY_FILTER_NETWORKS_OPTIONS = "KEY_HISTORY_FILTER_NETWORKS_OPTIONS"
private const val KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS = "KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS"
private const val KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS = "KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS"

@Singleton
class HistoryFilterOptions @Inject constructor(context: Context) {

    private val preferences = context.getSharedPreferences("history_filter_settings.pref", Context.MODE_PRIVATE)

    val filterActiveDevicesLiveData: LiveData<Set<String>?>
        get() {
            return StringSetPreferenceLiveData(preferences, KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS, null)
                .also {
                    it.postValue(preferences.getStringSet(KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS, null))
                }
        }

    var valueActiveFilterDevices: Set<String>?
        get() = _valueActiveFilterDevices
        set(value) {
            _valueActiveFilterDevices = value
            preferences.edit().putStringSet(KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS, _valueActiveFilterDevices).apply()
        }

    private var _valueActiveFilterDevices: Set<String>? = null

    val filterOptionsDevicesLiveData: LiveData<Set<String>?>
        get() {
            return StringSetPreferenceLiveData(preferences, KEY_HISTORY_FILTER_DEVICES_OPTIONS, null)
                .also {
                    it.postValue(preferences.getStringSet(KEY_HISTORY_FILTER_DEVICES_OPTIONS, null))
                }
        }

    var valueOptionsFilterDevices: Set<String>?
        get() = _valueOptionsFilterDevices
        set(value) {
            _valueOptionsFilterDevices = value
            preferences.edit().putStringSet(KEY_HISTORY_FILTER_DEVICES_OPTIONS, _valueOptionsFilterDevices).apply()
        }

    private var _valueOptionsFilterDevices: Set<String>? = null

    val filterActiveNetworksLiveData: LiveData<Set<String>?>
        get() {
            return StringSetPreferenceLiveData(preferences, KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS, null)
                .also {
                    it.postValue(preferences.getStringSet(KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS, null))
                }
        }

    var valueActiveFilterNetworks: Set<String>?
        get() = _valueActiveFilterNetworks
        set(value) {
            _valueActiveFilterNetworks = value
            preferences.edit().putStringSet(KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS, _valueActiveFilterNetworks).apply()
        }

    private var _valueActiveFilterNetworks: Set<String>? = null

    val filterOptionsNetworksLiveData: LiveData<Set<String>?>
        get() {
            return StringSetPreferenceLiveData(preferences, KEY_HISTORY_FILTER_NETWORKS_OPTIONS, null)
                .also {
                    it.postValue(preferences.getStringSet(KEY_HISTORY_FILTER_NETWORKS_OPTIONS, null))
                }
        }

    var valueOptionsFilterNetworks: Set<String>?
        get() = _valueOptionsFilterNetworks
        set(value) {
            _valueOptionsFilterNetworks = value
            preferences.edit().putStringSet(KEY_HISTORY_FILTER_NETWORKS_OPTIONS, _valueOptionsFilterNetworks).apply()
        }

    private var _valueOptionsFilterNetworks: Set<String>? = null

    init {
        _valueActiveFilterDevices = preferences.getStringSet(KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS, null)
        _valueActiveFilterNetworks = preferences.getStringSet(KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS, null)
        _valueOptionsFilterDevices = preferences.getStringSet(KEY_HISTORY_FILTER_DEVICES_OPTIONS, null)
        _valueOptionsFilterNetworks = preferences.getStringSet(KEY_HISTORY_FILTER_NETWORKS_OPTIONS, null)
    }
}
