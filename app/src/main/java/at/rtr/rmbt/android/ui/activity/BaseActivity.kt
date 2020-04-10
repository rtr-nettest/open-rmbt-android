package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import at.rmbt.util.exception.HandledException
import at.rmbt.util.exception.NoConnectionException
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.util.hasLocationPermissions
import at.rtr.rmbt.android.viewmodel.BaseViewModel
import at.rtr.rmbt.android.viewmodel.LocationViewModel

private const val DIALOG_DEFAULT_OK = -1

abstract class BaseActivity : AppCompatActivity() {

    private val viewModels = mutableListOf<BaseViewModel>()

    private val locationViewModel: LocationViewModel by viewModelLazy()

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewModels.forEach { it.onRestoreState(savedInstanceState) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModels.forEach { it.onSaveState(outState) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissions.hasLocationPermissions()) {
            locationViewModel.updateLocationPermissions()
        }
    }

    /**
     * Binds layout to [ViewDataBinding]
     */
    fun <T : ViewDataBinding> bindContentView(@LayoutRes layoutRes: Int): T =
        DataBindingUtil.setContentView(this, layoutRes)

    open fun onHandledException(exception: HandledException) {
        val message = if (exception is NoConnectionException) {
            getString(R.string.error_no_connection)
        } else {
            exception.getText(this)
        }

        SimpleDialog.Builder()
            .messageText(message)
            .positiveText(android.R.string.ok)
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
            when ((getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.orientation) {
                Surface.ROTATION_0 -> v.setPadding(0, 0, 0, v.paddingBottom + insets.systemWindowInsetBottom)
                Surface.ROTATION_90 -> v.setPadding(0, 0, v.paddingRight + insets.systemWindowInsetRight, 0)
                Surface.ROTATION_180 -> v.setPadding(0, v.paddingTop + insets.systemWindowInsetTop, 0, 0)
                Surface.ROTATION_270 -> v.setPadding(v.paddingLeft + insets.systemWindowInsetLeft, 0, 0, 0)
            }
            window.decorView.setBackgroundColor(Color.BLACK)
            insets
        }
    }
}