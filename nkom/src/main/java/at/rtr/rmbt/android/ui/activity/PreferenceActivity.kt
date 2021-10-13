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

package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityPreferenceBinding
import at.rtr.rmbt.android.ui.fragment.SettingsFragment

class PreferenceActivity : BaseActivity() {

    private lateinit var binding: ActivityPreferenceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_preference)

        setTransparentStatusBar()

        val permissionsOnly = intent?.getBooleanExtra(KEY_PERMISSIONS_ONLY, false) ?: false

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_content, SettingsFragment.newInstance(permissionsOnly))
                .commitNow()
        }

        binding.tvToolbarTitle.text = getText(
            if (permissionsOnly) {
                R.string.permissions
            } else {
                R.string.options
            }
        )

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    companion object {
        private const val KEY_PERMISSIONS_ONLY = "key_permissions_only"

        fun getIntent(context: Context, permissionsOnly: Boolean): Intent {
            return Intent(context, PreferenceActivity::class.java).apply { putExtra(KEY_PERMISSIONS_ONLY, permissionsOnly) }
        }
    }
}