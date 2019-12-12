package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.ui.viewstate.HistoryViewState
import at.rtr.rmbt.android.util.liveDataOf
import at.specure.data.entity.History
import at.specure.data.repository.HistoryRepository
import javax.inject.Inject

class HistoryViewModel @Inject constructor(private val repository: HistoryRepository) : BaseViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is history Fragment"
    }

    val text: LiveData<String> = _text

    val state = HistoryViewState()

    val historyLiveData: LiveData<List<History>>
        get() = repository.getHistory()

    init {
        addStateSaveHandler(state)
    }

    fun refreshHistory() = liveDataOf<Boolean> { liveData ->
        repository.refreshHistory {
            liveData.postValue(it.ok)
            it.onFailure { error ->
                postError(error)
            }
        }
    }
}