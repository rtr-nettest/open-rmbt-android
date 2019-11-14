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

private const val KEY_TAC_URL = "KEY_TAC_URL"
private const val KEY_NDT_TERMS_URL = "KEY_NDT_TERMS_URL"
private const val KEY_TAC_VERSION = "KEY_TAC_VERSION"

class TermsAndConditions @Inject constructor(context: Context) {

    private val preferences = context.getSharedPreferences("terms_and_conditions.pref", Context.MODE_PRIVATE)

    var tacUrl: String? = null
        set(value) {
            preferences.edit().putString(KEY_TAC_URL, value).apply()
        }

    var ndtTermsUrl: String? = null
        set(value) {
            preferences.edit().putString(KEY_NDT_TERMS_URL, value).apply()
        }

    var tacVersion: Long?
        get() {
            val value = preferences.getLong(KEY_TAC_VERSION, -1)
            return if (value == -1L) null
            else value
        }
        set(value) {
            preferences.edit().putLong(KEY_TAC_VERSION, value ?: -1).apply()
        }

    init {
        tacUrl = preferences.getString(KEY_TAC_URL, null)
        ndtTermsUrl = preferences.getString(KEY_NDT_TERMS_URL, null)
        tacVersion = preferences.getLong(KEY_TAC_VERSION, -1)
    }
}