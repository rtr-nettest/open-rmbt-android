package at.rtr.rmbt.android.di

import androidx.lifecycle.ViewModelProvider
import at.rtr.rmbt.android.ui.activity.BaseActivity
import at.rtr.rmbt.android.ui.fragment.BaseFragment
import at.rtr.rmbt.android.util.listenNonNull
import at.rtr.rmbt.android.viewmodel.BaseViewModel

inline fun <reified T : BaseViewModel> BaseFragment.viewModelLazy(): Lazy<T> = FragmentViewModelLazy(this, T::class.java)

inline fun <reified T : BaseViewModel> BaseActivity.viewModelLazy(): Lazy<T> = ActivityViewModelLazy(this, T::class.java)

class FragmentViewModelLazy<T : BaseViewModel>(private val fragment: BaseFragment, modelClass: Class<T>) : ViewModelLazy<T>(modelClass) {

    override val viewModelProvider: ViewModelProvider
        get() = ViewModelProvider(fragment, Injector.component.viewModelFactory())

    override fun subscribeOnError(viewModel: BaseViewModel) {
        viewModel.errorLiveData.listenNonNull(fragment) {
            fragment.onHandledException(it)
            viewModel.clearErrorMessages()
        }
        fragment.addViewModel(viewModel)
    }
}

class ActivityViewModelLazy<T : BaseViewModel>(private val activity: BaseActivity, modelClass: Class<T>) : ViewModelLazy<T>(modelClass) {

    override val viewModelProvider: ViewModelProvider
        get() = ViewModelProvider(activity, Injector.component.viewModelFactory())

    override fun subscribeOnError(viewModel: BaseViewModel) {
        viewModel.errorLiveData.listenNonNull(activity) {
            activity.onHandledException(it)
            viewModel.clearErrorMessages()
        }
        activity.addViewModel(viewModel)
    }
}

abstract class ViewModelLazy<T : BaseViewModel>(private val modelClass: Class<T>) : Lazy<T> {

    private lateinit var viewModel: T
    protected abstract val viewModelProvider: ViewModelProvider

    override val value: T
        get() {
            if (!::viewModel.isInitialized) {
                viewModel = viewModelProvider.get(modelClass)
                subscribeOnError(viewModel)
            }
            return viewModel
        }

    override fun isInitialized() = ::viewModel.isInitialized

    abstract fun subscribeOnError(viewModel: BaseViewModel)
}