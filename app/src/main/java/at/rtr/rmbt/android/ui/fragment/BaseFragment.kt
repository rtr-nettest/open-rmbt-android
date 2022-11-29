package at.rtr.rmbt.android.ui.fragment

import android.annotation.TargetApi
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import at.rmbt.util.exception.HandledException
import at.rmbt.util.exception.NoConnectionException
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.viewmodel.BaseViewModel
import at.rtr.rmbt.android.viewmodel.LocationViewModel
import timber.log.Timber

private const val DIALOG_DEFAULT_OK = -1

abstract class BaseFragment : Fragment() {

    private val viewModels = mutableListOf<BaseViewModel>()
    private lateinit var fragmentBinding: ViewDataBinding

    val locationViewModel: LocationViewModel by viewModelLazy()

    abstract val layoutResId: Int

    protected inline fun <reified T : ViewDataBinding> bindingLazy(): BindingLazy<T> = BindingLazy()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        if (savedInstanceState != null && viewModels.isNotEmpty()) {
            viewModels.forEach { it.onRestoreState(savedInstanceState) }
        }

        return if (layoutResId == 0) {
            throw IllegalArgumentException("Please setup layoutResId")
        } else {
            fragmentBinding = DataBindingUtil.inflate(inflater, layoutResId, container, false)
            fragmentBinding.root
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModels.forEach { it.onSaveState(outState) }
    }

    open fun onHandledException(exception: HandledException) {
        val message = if (exception is NoConnectionException) {
            getString(R.string.error_no_connection)
        } else {
            exception.getText(requireContext())
        }

        SimpleDialog.Builder()
            .messageText(message)
            .positiveText(android.R.string.ok)
            .cancelable(false)
            .show(parentFragmentManager, DIALOG_DEFAULT_OK)
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
        val uiMode = context?.resources?.configuration?.uiMode
        if (uiMode != null && (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION) {
            isAndroidTV = true
        }
        Timber.i("Is Android TV: $isAndroidTV")
        return isAndroidTV
    }

    @Suppress("UNCHECKED_CAST")
    protected inner class BindingLazy<out T : ViewDataBinding> : Lazy<T> {

        override val value: T
            get() = fragmentBinding as T

        override fun isInitialized() = ::fragmentBinding.isInitialized
    }
}