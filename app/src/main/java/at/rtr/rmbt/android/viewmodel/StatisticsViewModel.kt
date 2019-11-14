package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.ui.viewstate.StatisticsViewState
import javax.inject.Inject

class StatisticsViewModel @Inject constructor() : BaseViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is statistics Fragment"
    }

    val text: LiveData<String> = _text

    val state = StatisticsViewState()

    init {
        addStateSaveHandler(state)
    }
}