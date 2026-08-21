package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.ui.viewstate.HistoryViewState
import at.specure.data.entity.HistoryContainer
import at.specure.data.repository.HistoryLoadState
import at.specure.data.repository.HistoryLoader
import at.specure.data.repository.HistoryRepository
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository,
    private val loader: HistoryLoader,
) : BaseViewModel() {

    private val _isLoadingLiveData = MutableLiveData<Boolean>()
    private val _initialLoadingLiveData = MutableLiveData<Boolean>()
    private val _loadFailedLiveData = MutableLiveData<Boolean>()
    private var _historyLiveData: LiveData<PagedList<HistoryContainer>> = MutableLiveData<PagedList<HistoryContainer>>()

    val state = HistoryViewState()

    val isLoadingLiveData: LiveData<Boolean>
        get() = _isLoadingLiveData

    /** Immediate (non-debounced) initial-load state, used to hide the placeholder text while loading. */
    val initialLoadingLiveData: LiveData<Boolean>
        get() = _initialLoadingLiveData

    /** True when the initial load failed, used to show the "loading failed" placeholder. */
    val loadFailedLiveData: LiveData<Boolean>
        get() = _loadFailedLiveData

    val historyLiveData: LiveData<PagedList<HistoryContainer>>
        get() {
            _historyLiveData = loader.historyLiveData
            return _historyLiveData
        }

    init {
        addStateSaveHandler(state)

        val isLoadingChannel = Channel<Boolean>()
        launch(CoroutineName("HistoryViewModelInit1")) {
            isLoadingChannel.consumeAsFlow()
                .debounce(200)
                .collect {
                    _isLoadingLiveData.postValue(it)
                }
        }

        val errorChannel = Channel<HandledException>()
        launch(CoroutineName("HistoryViewModelInit2")) {
            errorChannel.consumeAsFlow()
                .collect {
                    postError(it)
                }
        }

        val loadStateChannel = Channel<HistoryLoadState>()
        launch(CoroutineName("HistoryViewModelInit3")) {
            loadStateChannel.consumeAsFlow()
                .collect { state ->
                    _initialLoadingLiveData.postValue(state == HistoryLoadState.LOADING)
                    _loadFailedLiveData.postValue(state == HistoryLoadState.FAILED)
                }
        }

        loader.isLoadingChannel = isLoadingChannel
        loader.errorChannel = errorChannel
        loader.loadStateChannel = loadStateChannel
    }

    val activeFiltersLiveData = repository.appliedFiltersLiveData

    init {
        addStateSaveHandler(state)
    }

    fun refreshHistory() {
        loader.latestLoadedPage = 0
        repository.cleanHistory()
        loader.refresh()

    }

    /**
     * Shows cached history instantly when the screen is (re)opened and only loads from the
     * network when nothing is cached. Use this instead of [refreshHistory] on screen open to
     * avoid reloading everything each time History is reopened.
     */
    fun loadHistoryIfNeeded() {
        loader.loadInitialIfNeeded()
    }

    /**
     * Marks the cached history as invalid so it is fully reloaded next time History is opened.
     * Used after a device sync (which can change the history on the backend).
     */
    fun invalidateHistoryCache() {
        repository.historyCacheInvalidated = true
    }

    /**
     * Loads the next page of history on demand, e.g. when the user scrolls near the end of
     * the list (lazy loading).
     */
    fun loadNextPage() {
        loader.loadNextPage()
    }

    fun removeFromFilters(value: String) {
        repository.removeFromFilters(value)
        refreshHistory()
    }
}