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
import androidx.appcompat.widget.Toolbar
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityTestResultDetailBinding
import at.rtr.rmbt.android.ui.fragment.TestResultDetailFragment

class TestResultDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityTestResultDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_test_result_detail)
        setupToolbar()

        val testUUID = intent.extras?.getString(KEY_TEST_UUID)
        if (testUUID == null) {
            throw IllegalArgumentException("Please pass test UUID")
        } else {
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_content, TestResultDetailFragment.newInstance(testUUID))
                    .commitNow()
            }
        }
    }
    private fun setupToolbar() {
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        binding.tvToolbarTitle.text = getString(R.string.result_test_details)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    companion object {

        private const val KEY_TEST_UUID: String = "KEY_TEST_UUID"

        fun start(context: Context, testUUID: String) {
            val intent = Intent(context, TestResultDetailActivity::class.java)
            intent.putExtra(KEY_TEST_UUID, testUUID)
            context.startActivity(intent)
        }
    }
}