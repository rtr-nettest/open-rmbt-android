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
import at.specure.util.StringSetPreferenceLiveData
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_HISTORY_FILTER_DEVICES_OPTIONS = "KEY_HISTORY_FILTER_DEVICES_OPTIONS"
private const val KEY_HISTORY_FILTER_NETWORKS_OPTIONS = "KEY_HISTORY_FILTER_NETWORKS_OPTIONS"
private const val KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS = "KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS"
private const val KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS = "KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS"
private const val KEY_HISTORY_FILTERS_APPLIED_OPTIONS = "KEY_HISTORY_FILTERS_APPLIED_OPTIONS"

@Singleton
class HistoryFilterOptions @Inject constructor(context: Context) {

    private val preferences = context.getSharedPreferences("history_filter_settings.pref", Context.MODE_PRIVATE)

    var networks: Set<String>?
        get() = preferences.getStringSet(KEY_HISTORY_FILTER_NETWORKS_OPTIONS, null)
        set(value) = preferences.edit().putStringSet(KEY_HISTORY_FILTER_NETWORKS_OPTIONS, value).apply()

    var activeNetworks: Set<String>?
        get() = preferences.getStringSet(KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS, null)
        set(value) = preferences.edit().putStringSet(KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS, value).apply()

    var devices: Set<String>?
        get() = preferences.getStringSet(KEY_HISTORY_FILTER_DEVICES_OPTIONS, null)
        set(value) = preferences.edit().putStringSet(KEY_HISTORY_FILTER_DEVICES_OPTIONS, value).apply()

    var activeDevices: Set<String>?
        get() = preferences.getStringSet(KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS, null)
        set(value) = preferences.edit().putStringSet(KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS, value).apply()

    var appliedFilters: Set<String>?
        get() = preferences.getStringSet(KEY_HISTORY_FILTERS_APPLIED_OPTIONS, null)
        set(value) = preferences.edit().putStringSet(KEY_HISTORY_FILTERS_APPLIED_OPTIONS, value).apply()

    val activeNetworksLiveData = StringSetPreferenceLiveData(preferences, KEY_HISTORY_FILTER_ACTIVE_NETWORKS_OPTIONS, activeNetworks)
    val activeDevicesLiveData = StringSetPreferenceLiveData(preferences, KEY_HISTORY_FILTER_ACTIVE_DEVICES_OPTIONS, activeDevices)

    val networksLiveData = StringSetPreferenceLiveData(preferences, KEY_HISTORY_FILTER_NETWORKS_OPTIONS, networks)
    val devicesLiveData = StringSetPreferenceLiveData(preferences, KEY_HISTORY_FILTER_DEVICES_OPTIONS, devices)

    val appliedFiltersLiveData = StringSetPreferenceLiveData(
        preferences,
        KEY_HISTORY_FILTERS_APPLIED_OPTIONS,
        mutableSetOf<String>()
            .apply {
                activeDevices?.let { addAll(it) }
                activeNetworks?.let { addAll(it) }
            }
    )
}
