package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.viewmodel.BaseViewModel

private const val DIALOG_DEFAULT_OK = -1

abstract class BaseFragment : Fragment() {

    private val viewModels = mutableListOf<BaseViewModel>()
    private lateinit var fragmentBinding: ViewDataBinding

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
        SimpleDialog.Builder()
            .messageText(exception.getText(requireContext()))
            .positiveText(R.string.button_close)
            .cancelable(false)
            .show(parentFragmentManager, DIALOG_DEFAULT_OK)
    }

    /**
     * Used by ViewModelLazy to allow state saving
     */
    fun addViewModel(viewModel: BaseViewModel) {
        viewModels.add(viewModel)
    }

    protected fun updateTransparentStatusBarHeight(stubView: View) {
        val heightDimensionId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (heightDimensionId > 0) {
            stubView.layoutParams.height = resources.getDimensionPixelSize(heightDimensionId)
            stubView.requestLayout()
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected inner class BindingLazy<out T : ViewDataBinding> : Lazy<T> {

        override val value: T
            get() = fragmentBinding as T

        override fun isInitialized() = ::fragmentBinding.isInitialized
    }
}