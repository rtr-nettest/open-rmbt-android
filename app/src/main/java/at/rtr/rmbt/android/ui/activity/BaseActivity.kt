package at.rtr.rmbt.android.ui.activity

import android.annotation.TargetApi
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import at.rmbt.util.exception.HandledException
import at.rmbt.util.exception.NoConnectionException
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.viewmodel.BaseViewModel
import timber.log.Timber

private const val DIALOG_DEFAULT_OK = -1

abstract class BaseActivity : AppCompatActivity() {

    private val viewModels = mutableListOf<BaseViewModel>()

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModels.forEach { it.onRestoreState(savedInstanceState) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModels.forEach { it.onSaveState(outState) }
    }

    /**
     * Binds layout to [ViewDataBinding]
     */
    fun <T : ViewDataBinding> bindContentView(@LayoutRes layoutRes: Int): T =
        DataBindingUtil.setContentView(this, layoutRes)

    open fun onHandledException(exception: HandledException?) {
        exception?.let { handledException ->
            val message = if (handledException is NoConnectionException) {
                getString(R.string.error_no_connection)
            } else {
                handledException.getText(this)
            }

            SimpleDialog.Builder()
                .messageText(message)
                .positiveText(android.R.string.ok)
                .cancelable(false)
                .show(supportFragmentManager, DIALOG_DEFAULT_OK)
        }
    }

    /**
     * Used by ViewModelLazy to allow state saving
     */
    fun addViewModel(viewModel: BaseViewModel) {
        viewModels.add(viewModel)
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    protected fun checkIsTelevision(): Boolean {
        var isAndroidTV = false
        val uiMode = this.resources?.configuration?.uiMode
        if (uiMode != null && (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION) {
            isAndroidTV = true
        }
        Timber.i("Is Android TV: $isAndroidTV")
        return isAndroidTV
    }

    private fun setWindowFlag(bits: Int, on: Boolean) {
        val winParams = window.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        window.attributes = winParams
    }

    protected fun setTransparentStatusBar() {
        setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        setWindowFlag(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS, true)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}