package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.util.io
import at.rtr.rmbt.android.ui.viewstate.SimpleResultsListViewState
import at.specure.data.entity.History
import at.specure.data.repository.HistoryRepository
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class SimpleResultsListViewModel @Inject constructor(private val repository: HistoryRepository) : BaseViewModel() {

    private val _isLoadingLiveData = MutableLiveData<Boolean>()
    private val _loopHistoryLiveData = MutableLiveData<List<History>>()

    val state = SimpleResultsListViewState()

    val isLoadingLiveData: LiveData<Boolean>
        get() = _isLoadingLiveData

    val historyLiveData: LiveData<List<History>>
        get() = _loopHistoryLiveData

    init {
        addStateSaveHandler(state)
    }

    fun refreshHistory(loopUUID: String) = io {
        _isLoadingLiveData.postValue(true)
        repository.loadLoopHistoryItems(loopUUID).collect {
            _loopHistoryLiveData.postValue(it)
            _isLoadingLiveData.postValue(false)
        }
    }
}