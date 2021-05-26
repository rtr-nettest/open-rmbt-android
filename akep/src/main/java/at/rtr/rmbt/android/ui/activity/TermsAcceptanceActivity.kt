package at.rtr.rmbt.android.ui.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityTermsAcceptanceBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.TermsAcceptanceViewModel
import at.specure.worker.WorkLauncher

class TermsAcceptanceActivity : BaseActivity() {

    private lateinit var binding: ActivityTermsAcceptanceBinding
    private val viewModel: TermsAcceptanceViewModel by viewModelLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_terms_acceptance)
        window?.changeStatusBarColor(ToolbarTheme.WHITE)

        binding.content.webViewClient = TermsClient()
        viewModel.tacContentLiveData.listen(this) {
            it?.let {
                binding.content.loadDataWithBaseURL(null, it, "text/html", "utf-8", null)
            }
        }

        binding.buttonToBottom.setOnClickListener {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }

        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {
            val scrollBounds = Rect()
            binding.scrollView.getHitRect(scrollBounds)
            if (binding.accept.getLocalVisibleRect(scrollBounds)) {
                // imageView is within the visible window
                binding.buttonToBottom.hide()
            } else {
                // imageView is not within the visible window
                binding.buttonToBottom.show()
            }
        }

        binding.accept.setOnClickListener {
            if (binding.checkbox.isChecked) {
                viewModel.updateTermsAcceptance(true)
                WorkLauncher.enqueueSettingsRequest(this)
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                SimpleDialog.Builder()
                    .messageText(R.string.text_terms_agree_empty)
                    .positiveText(android.R.string.ok)
                    .cancelable(true)
                    .show(supportFragmentManager, CODE_DIALOG)
            }
        }

        binding.decline.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            viewModel.updateTermsAcceptance(false)
            finish()
        }

        binding.checkbox.isFocusable = true
        binding.checkbox.isFocusableInTouchMode = false
        binding.accept.isFocusable = true
        binding.accept.isFocusableInTouchMode = false
        binding.decline.isFocusable = true
        binding.decline.isFocusableInTouchMode = false

        viewModel.getTac()
    }

    override fun onResume() {
        super.onResume()
        binding.checkbox.requestFocus()
    }

    override fun onBackPressed() {}

    inner class TermsClient : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (!checkIsTelevision()) {
                binding.buttonToBottom.show()
            }
            binding.scrollView.visibility = View.VISIBLE
            binding.checkbox.requestFocus()
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            // always open links in new intent on terms/conditions/privacy
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                startActivity(this)
            }

            return true
        }
    }

    companion object {

        private const val CODE_DIALOG = 12

        fun start(activity: Activity, code: Int) = activity.startActivityForResult(Intent(activity, TermsAcceptanceActivity::class.java), code)
    }
}
