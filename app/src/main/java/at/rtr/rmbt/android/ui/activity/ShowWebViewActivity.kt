package at.rtr.rmbt.android.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityShowWebviewBinding

class ShowWebViewActivity : BaseActivity() {

    private lateinit var binding: ActivityShowWebviewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_show_webview)

        val url = intent.getStringExtra(KEY_URL) ?: throw IllegalArgumentException("Nothing to show without url")
        binding.content.loadUrl(url)
        binding.content.settings.javaScriptEnabled = true
        binding.content.webViewClient = DetailsViewClient()
    }

    inner class DetailsViewClient : WebViewClient() {

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding.progress.visibility = View.GONE
        }
    }

    companion object {
        const val KEY_URL = "url"

        fun start(context: Context, url: String) = context.startActivity(Intent(context, ShowWebViewActivity::class.java).putExtra(KEY_URL, url))
    }
}