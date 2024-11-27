package at.rtr.rmbt.android.viewmodel

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.rmbt.util.Maybe
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.ui.viewstate.ViewState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import java.util.concurrent.TimeUnit

open class BaseViewModel : ViewModel(), CoroutineScope {

    private val viewStates = mutableSetOf<ViewState>()
    private val _errorLiveData = MutableLiveData<HandledException?>()
    val askPermissionsAgainTimesMillis = TimeUnit.DAYS.toMillis(1)

    val errorLiveData: LiveData<HandledException?>
        get() = _errorLiveData

    fun clearErrorMessages() {
        _errorLiveData.postValue(null)
    }

    /**
     * Sends an error to default error-handling LiveData
     */
    protected fun postError(error: HandledException) = _errorLiveData.postValue(error)

    /**
     * Send result to given LiveData on success otherwise sends an error to default error-handling LiveData
     */
    protected fun <T> Maybe<T>.postTo(liveData: MutableLiveData<T>) {
        onSuccess { liveData.postValue(it) }
        onFailure { postError(it) }
    }

    /**
     * Should be called in init{} block of ViewModel to add [ViewState] to save/restore states
     */
    fun addStateSaveHandler(vararg viewState: ViewState) {
        viewStates.addAll(viewState)
    }

    /**
     * Should be called to restore states from bundle
     */
    open fun onRestoreState(bundle: Bundle?) {
        viewStates.forEach { it.onRestoreState(bundle) }
    }

    /**
     * Should be called to save states to bundle
     */
    open fun onSaveState(bundle: Bundle?) {
        viewStates.forEach { it.onSaveState(bundle) }
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { context, e ->
        if (e is HandledException) {
            postError(e)
        } else {
            Timber.e("My viewModel coroutine named: ${context[CoroutineName]} has crashed with: ${e.message}")
            throw e
        }
    }

    final override val coroutineContext = viewModelScope.coroutineContext + coroutineExceptionHandler
}