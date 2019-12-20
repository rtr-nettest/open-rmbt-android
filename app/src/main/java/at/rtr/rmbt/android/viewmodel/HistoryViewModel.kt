package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import at.rtr.rmbt.android.ui.viewstate.HistoryViewState
import at.rtr.rmbt.android.util.map
import at.specure.data.entity.History
import at.specure.data.repository.HistoryRepository
import javax.inject.Inject

private const val PAGE_SIZE = 25

class HistoryViewModel @Inject constructor(private val repository: HistoryRepository) : BaseViewModel() {

    private val boundaryCallback = repository.boundaryCallback(
        limit = PAGE_SIZE,
        onLoadingCallback = { _isLoadingLiveData.postValue(it) },
        onErrorCallback = { postError(it) }
    )

    private val _isLoadingLiveData = MutableLiveData<Boolean>()

    val isLoadingLiveData: LiveData<Boolean>
        get() = _isLoadingLiveData

    val state = HistoryViewState()

    val historyLiveData: LiveData<PagedList<History>> by lazy {
        val source = repository.getHistorySource()
        val config = PagedList.Config.Builder()
            .setPageSize(PAGE_SIZE)
            .build()
        LivePagedListBuilder(source, config)
            .setBoundaryCallback(boundaryCallback)
            .build()
    }

    val isHistoryEmpty: LiveData<Boolean> = historyLiveData.map {
        it.isEmpty()
    }

    init {
        addStateSaveHandler(state)
    }

    fun clearHistory() = repository.clearHistory()

    fun refreshHistory() = repository.refreshHistory(
        limit = PAGE_SIZE,
        onLoadingCallback = { _isLoadingLiveData.postValue(it) },
        onErrorCallback = { postError(it) }
    )
}