package at.rtr.rmbt.android.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.viewmodel.BaseViewModel

private const val DIALOG_DEFAULT_OK = -1

abstract class BaseActivity : AppCompatActivity() {

    private val viewModels = mutableListOf<BaseViewModel>()

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
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

    open fun onHandledException(exception: HandledException) {
        SimpleDialog.Builder()
            .messageText(exception.getText(this))
            .positiveText(R.string.button_close)
            .cancelable(false)
            .show(supportFragmentManager, DIALOG_DEFAULT_OK)
    }

    /**
     * Used by ViewModelLazy to allow state saving
     */
    fun addViewModel(viewModel: BaseViewModel) {
        viewModels.add(viewModel)
    }

    protected fun setTransparentStatusBar() {
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            v.setPadding(0, 0, 0, v.paddingBottom + insets.systemWindowInsetBottom)
            window.decorView.setBackgroundColor(Color.BLACK)
            insets
        }
    }
}