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
import androidx.core.content.edit
import at.specure.config.Config
import at.specure.core.R
import at.specure.util.BooleanPreferenceLiveData
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_TAC_URL = "KEY_TAC_URL"
private const val KEY_NDT_TERMS_URL = "KEY_NDT_TERMS_URL"
private const val KEY_TAC_VERSION = "KEY_TAC_VERSION"
private const val KEY_TAC_IS_ACCEPTED = "KEY_TAC_ACCEPTED"
private const val KEY_ACCEPTED_TAC_VERSION = "KEY_ACCEPTED_TAC_VERSION"

@Singleton
class TermsAndConditions @Inject constructor(context: Context, config: Config) {

    private val preferences = context.getSharedPreferences("terms_and_conditions.pref", Context.MODE_PRIVATE)

    var tacUrl: String?
        get() = preferences.getString(KEY_TAC_URL, null)
        set(value) {
            preferences.edit { putString(KEY_TAC_URL, value) }
        }

    var ndtTermsUrl: String?
        get() = preferences.getString(KEY_NDT_TERMS_URL, null)
        set(value) {
            preferences.edit { putString(KEY_NDT_TERMS_URL, value) }
        }

    var tacVersion: Int?
        get() {
            val value = preferences.getInt(KEY_TAC_VERSION, -1)
            return if (value == -1) null
            else value
        }
        set(value) {
            preferences.edit { putInt(KEY_TAC_VERSION, value ?: -1) }
        }

    var localVersion: InputStream = context.resources.openRawResource(R.raw.terms)

    var tacAccepted: Boolean
        get() = preferences.getBoolean(KEY_TAC_IS_ACCEPTED, false)
        set(value) = preferences.edit { putBoolean(KEY_TAC_IS_ACCEPTED, value) }

    /**
     * The terms version the user actually accepted (null if never accepted). The re-prompt decision
     * compares this against the server's current version, so the terms are only shown again when the
     * server has a strictly newer version - not on a language/URL change.
     */
    var acceptedTacVersion: Int?
        get() {
            val value = preferences.getInt(KEY_ACCEPTED_TAC_VERSION, -1)
            return if (value == -1) null else value
        }
        set(value) {
            preferences.edit { putInt(KEY_ACCEPTED_TAC_VERSION, value ?: -1) }
        }

    /** Version to record as accepted when there is no server version yet (offline acceptance). */
    val bundledTermsVersion: Int = config.bundledTermsVersion

    val tacAcceptanceLiveData = BooleanPreferenceLiveData(preferences, KEY_TAC_IS_ACCEPTED, false)
}