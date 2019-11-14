package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.ui.viewstate.HistoryViewState
import javax.inject.Inject

class HistoryViewModel @Inject constructor() : BaseViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is history Fragment"
    }

    val text: LiveData<String> = _text

    val state = HistoryViewState()

    init {
        addStateSaveHandler(state)
    }
}