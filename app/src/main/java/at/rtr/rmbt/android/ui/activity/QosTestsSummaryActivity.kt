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
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityQosTestsSummaryBinding
import at.rtr.rmbt.android.ui.fragment.QosTestsSummaryFragment
import at.specure.data.entity.QosCategoryRecord
import at.specure.result.QoSCategory

class QosTestsSummaryActivity : BaseActivity() {

    private lateinit var binding: ActivityQosTestsSummaryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_qos_tests_summary)

        intent.extras?.getString(KEY_TEST_UUID) ?: throw IllegalStateException("Please pass test UUID")
        intent.extras?.getString(KEY_QOS_CATEGORY_DESCRIPTION) ?: throw IllegalStateException("Please pass category description")

        val testUUID = intent.extras?.getString(KEY_TEST_UUID)
        val category = intent.extras?.getSerializable(KEY_QOS_CATEGORY) as QoSCategory
        val categoryDescription = intent.extras?.getString(KEY_QOS_CATEGORY_DESCRIPTION)
        val categoryName = intent.extras?.getString(KEY_CATEGORY_NAME)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_content, QosTestsSummaryFragment.newInstance(testUUID, categoryDescription, category, categoryName))
                .commitNow()
        }

        setupToolbar(categoryName)
    }

    private fun setupToolbar(toolbarTitle: String?) {
        binding.title.text = toolbarTitle
        binding.title.contentDescription = "${ContextCompat.getString(this.applicationContext, R.string.title)} $toolbarTitle"
        binding.buttonBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            binding.buttonBack.requestFocus()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {

        private const val KEY_TEST_UUID: String = "KEY_TEST_UUID"
        private const val KEY_QOS_CATEGORY: String = "KEY_QOS_CATEGORY"
        private const val KEY_QOS_CATEGORY_DESCRIPTION: String = "KEY_QOS_CATEGORY_DESCRIPTION"
        private const val KEY_CATEGORY_NAME: String = "KEY_CATEGORY_NAME"

        fun start(context: Context, qosCategoryRecord: QosCategoryRecord) {

            Intent(context, QosTestsSummaryActivity::class.java).apply {
                putExtra(KEY_TEST_UUID, qosCategoryRecord.testUUID)
                putExtra(KEY_QOS_CATEGORY_DESCRIPTION, qosCategoryRecord.categoryDescription)
                putExtra(KEY_QOS_CATEGORY, qosCategoryRecord.category)
                putExtra(KEY_CATEGORY_NAME, qosCategoryRecord.categoryName)
                context.startActivity(this)
            }
        }
    }
}