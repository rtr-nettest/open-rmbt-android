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

private const val KEY_NEWS_LAST_UID = "KEY_NEWS_LAST_UID"

@Singleton
class NewsSettings @Inject constructor(context: Context) {

    private val preferences = context.getSharedPreferences("news.pref", Context.MODE_PRIVATE)

    var lastNewsUID: Long? = null
        set(value) {
            value?.let { uid ->
                if ((uid != -1L) && (field == null || field ?: 0 < uid)) {
                    preferences.edit().putLong(KEY_NEWS_LAST_UID, uid).apply()
                    field = value
                }
            }
        }

    init {
        lastNewsUID = preferences.getLong(KEY_NEWS_LAST_UID, -1)
    }
}