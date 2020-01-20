package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
import android.os.PersistableBundle
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityShowWebviewBinding


class ShowWebViewActivity : BaseActivity() {

    private lateinit var binding: ActivityShowWebviewBinding

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        binding = bindContentView(R.layout.activity_show_webview)

        val url = intent.getStringExtra(KEY_URL) ?: throw IllegalArgumentException("Nothing to show without url")
        binding.content.loadUrl(url)
        binding.content.webViewClient = DetailsViewClient()
        binding.content.setBackgroundColor(Color.YELLOW)
    }

    inner class DetailsViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            view?.loadUrl(url)
            return true
        }

        override fun onReceivedSslError(
            view: WebView?, handler: SslErrorHandler,
            error: SslError?
        ) {
            super.onReceivedSslError(view, handler, error)
            handler.proceed()
        }

    }

    companion object {
        const val KEY_URL = "url"

        fun start(context: Context, url: String) = context.startActivity(Intent(context, ShowWebViewActivity::class.java).putExtra(KEY_URL, url))
    }
}

