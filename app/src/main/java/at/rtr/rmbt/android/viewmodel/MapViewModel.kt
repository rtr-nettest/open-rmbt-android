package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.ui.viewstate.MapViewState
import javax.inject.Inject

class MapViewModel @Inject constructor() : BaseViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is map Fragment"
    }

    val text: LiveData<String> = _text

    val state = MapViewState()

    init {
        addStateSaveHandler(state)
    }
}