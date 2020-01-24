package at.rtr.rmbt.android.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentStatisticsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.viewmodel.StatisticsViewModel

@SuppressLint("SetJavaScriptEnabled")
class StatisticsFragment : BaseFragment() {

    private val statisticsViewModel: StatisticsViewModel by viewModelLazy()
    private val binding: FragmentStatisticsBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_statistics

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.window?.changeStatusBarColor(ToolbarTheme.WHITE)

        binding.state = statisticsViewModel.state

        with(binding.webViewStatistics) {
            setInitialScale(1)
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.javaScriptEnabled = true
            webViewClient = MyWebViewClient(statisticsViewModel)
        }
    }

    private class MyWebViewClient(private val statisticsViewModel: StatisticsViewModel) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            view?.loadUrl(url)
            return true
        }
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            statisticsViewModel.state.isLoading.set(true)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            statisticsViewModel.state.isLoading.set(false)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            statisticsViewModel.state.isLoading.set(false)
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            statisticsViewModel.state.isLoading.set(false)
        }
    }
}