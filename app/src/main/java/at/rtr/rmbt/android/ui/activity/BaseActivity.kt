package at.rtr.rmbt.android.ui.activity

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.ui.dialog.Dialogs
import at.rtr.rmbt.android.util.getStringTitle
import at.rtr.rmbt.android.viewmodel.BaseViewModel

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
        Dialogs.show(
            context = this,
            title = exception.getStringTitle(this),
            message = exception.getText(this)
        )
    }

    /**
     * Used by ViewModelLazy to allow state saving
     */
    fun addViewModel(viewModel: BaseViewModel) {
        viewModels.add(viewModel)
    }
}