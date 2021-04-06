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
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.widget.Toolbar
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityDataPrivacyTermsOfUseBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.viewmodel.TermsAcceptanceViewModel

@SuppressLint("SetJavaScriptEnabled")
class DataPrivacyAndTermsOfUseActivity : BaseActivity() {

    private lateinit var binding: ActivityDataPrivacyTermsOfUseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_data_privacy_terms_of_use)

        if (!intent.hasExtra(KEY_DATA_PRIVACY_AND_TERMS_OF_USE_URL) || !intent.hasExtra(KEY_TITLE)) {
            throw IllegalArgumentException("No url or name provided")
        }
        setupToolbar()

        with(binding.webViewDataPrivacyAndTermsOfUse) {
            setInitialScale(1)
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.javaScriptEnabled = true
            webViewClient = MyWebViewClient()

            setOnKeyListener(android.view.View.OnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                    event.action == android.view.KeyEvent.ACTION_UP &&
                    canGoBack()
                ) {
                    goBack()
                    return@OnKeyListener true
                }
                false
            })
            loadUrl(intent.getStringExtra(KEY_DATA_PRIVACY_AND_TERMS_OF_USE_URL))
        }
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

    inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            view?.loadUrl(url)
            return true
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            binding.progressDataPrivacyAndTermsOfUse.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding.progressDataPrivacyAndTermsOfUse.visibility = View.GONE
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            binding.progressDataPrivacyAndTermsOfUse.visibility = View.GONE
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            binding.progressDataPrivacyAndTermsOfUse.visibility = View.GONE
        }
    }

    companion object {

        private const val KEY_DATA_PRIVACY_AND_TERMS_OF_USE_URL: String = "KEY_DATA_PRIVACY_AND_TERMS_OF_USE_URL"
        private const val KEY_TITLE: String = "KEY_TITLE"
        fun start(context: Context, dataPrivacyAndTermsUrl: String, title: String) {

            val intent = Intent(context, DataPrivacyAndTermsOfUseActivity::class.java).apply {
                putExtra(KEY_DATA_PRIVACY_AND_TERMS_OF_USE_URL, dataPrivacyAndTermsUrl)
                putExtra(KEY_TITLE, title)
            }
            context.startActivity(intent)
        }
    }
}