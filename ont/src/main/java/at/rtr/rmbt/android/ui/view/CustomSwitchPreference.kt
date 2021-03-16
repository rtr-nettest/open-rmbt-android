/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import at.rtr.rmbt.android.R
import com.suke.widget.SwitchButton

class CustomSwitchPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwitchPreference(
    context, attrs
) {

    init {
        widgetLayoutResource = R.layout.switch_widget
    }

    /**
     * This function used for set value, and add event listener
     */
    private fun bindSwitch(viewHolder: PreferenceViewHolder) {
        val view = viewHolder.findViewById(R.id.compound_button)

        if (view is SwitchButton) {
            view.isChecked = getPersistedBoolean(false)
            view.setOnCheckedChangeListener { _, isChecked ->
                if (getPersistedBoolean(false) != isChecked) {
                    if (onPreferenceChangeListener != null)
                        onPreferenceChangeListener.onPreferenceChange(
                            this@CustomSwitchPreference,
                            isChecked
                        )
                    persistBoolean(isChecked)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        holder?.let { bindSwitch(it) }
    }
}