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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityStaticPageBinding
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.StaticPageViewModel
import at.specure.util.MarkwonBuilder
import io.noties.markwon.Markwon

@SuppressLint("SetJavaScriptEnabled")
class StaticPageActivity : BaseActivity() {

    private lateinit var binding: ActivityStaticPageBinding
    private val viewModel: StaticPageViewModel by viewModels()
    private lateinit var markwon: Markwon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_static_page)
        markwon = MarkwonBuilder.build(applicationContext)

        if (!intent.hasExtra(KEY_DATA_PRIVACY_AND_TERMS_OF_USE_URL) || !intent.hasExtra(KEY_TITLE)) {
            throw IllegalArgumentException("No url or name provided")
        }
        setupToolbar()

        viewModel.contentLiveData.listen(this) { pageContent ->
            binding.progressDataPrivacyAndTermsOfUse.visibility = View.GONE
            markwon.setMarkdown(binding.textViewDataPrivacyAndTermsOfUse, pageContent)
        }

        viewModel.getContent(applicationContext, intent.getStringExtra(KEY_DATA_PRIVACY_AND_TERMS_OF_USE_URL)!!)
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationIcon(R.drawable.ic_back)
        binding.tvToolbarTitle.text = intent.getStringExtra(KEY_TITLE)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    companion object {

        private const val KEY_DATA_PRIVACY_AND_TERMS_OF_USE_URL: String = "KEY_DATA_PRIVACY_AND_TERMS_OF_USE_URL"
        private const val KEY_TITLE: String = "KEY_TITLE"
        fun start(context: Context, dataPrivacyAndTermsUrl: String, title: String) {

            val intent = Intent(context, StaticPageActivity::class.java).apply {
                putExtra(KEY_DATA_PRIVACY_AND_TERMS_OF_USE_URL, dataPrivacyAndTermsUrl)
                putExtra(KEY_TITLE, title)
            }
            context.startActivity(intent)
        }
    }
}