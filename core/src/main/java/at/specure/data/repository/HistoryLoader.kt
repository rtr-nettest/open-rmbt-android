package at.specure.data.repository

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.specure.data.CoreDatabase
import at.specure.data.entity.HistoryContainer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

private const val LIMIT = 100

class HistoryLoader @Inject constructor(
    db: CoreDatabase,
    private val historyRepository: HistoryRepository
) :
    PagedList.BoundaryCallback<HistoryContainer>() {

    var latestLoadedPage = 0
    var isLoadingChannel: Channel<Boolean>? = null
    var errorChannel: Channel<HandledException>? = null

    private val historyDao = db.historyDao()

    // Re-entrancy guard shared by refresh() and on-demand paging.
    private var isLoading = false

    // Set once the server reports no more pages, so we stop requesting past the end.
    @Volatile
    private var endReached = false

    // Drives the pull-to-refresh spinner. Only refresh() reports loading here so the
    // spinner clears once the first (visible) page is ready, instead of staying up while
    // the boundary callback silently pages the rest of the history in the background.
    private fun reportRefreshing(loading: Boolean) {
        runBlocking {
            isLoadingChannel?.send(loading)
        }
    }

    val historyLiveData: LiveData<PagedList<HistoryContainer>> by lazy {

        val networks = historyRepository.networksLiveData.value?.toList() ?: emptyList()
        val devices = historyRepository.devicesLiveData.value?.toList() ?: emptyList()

        val source = historyDao.getHistorySource(
            networks = networks,
            ignoreNetworkTypes = networks.isEmpty(),
            devices = devices,
            ignoreDevices = devices.isEmpty()
        )
        val config = PagedList.Config.Builder()
            .setPageSize(LIMIT)
            .setPrefetchDistance(LIMIT / 2) // load only near visible end
            .setInitialLoadSizeHint(LIMIT)  // do NOT load 3 pages at start
            .setEnablePlaceholders(false)
            .build()
        LivePagedListBuilder(source, config)
            .setBoundaryCallback(this)
            .build()
    }

    val source: DataSource.Factory<Int, HistoryContainer>
        get() {
            val networks = historyRepository.networksLiveData.value?.toList() ?: emptyList()
            val devices = historyRepository.devicesLiveData.value?.toList() ?: emptyList()

            val source = historyDao.getHistorySource(
                networks = networks,
                ignoreNetworkTypes = networks.isEmpty(),
                devices = devices,
                ignoreDevices = devices.isEmpty()
            )
            return source
        }

    // NOTE: BoundaryCallback hooks are intentionally inert. Inserting each fetched page
    // invalidates the Room DataSource, which rebuilds the PagedList and would re-fire these
    // callbacks, causing the whole history to be pulled at once. Pagination is instead
    // driven on demand: refresh() loads the first page, and the UI requests further pages
    // via loadNextPage() as the user scrolls (see HistoryFragment).
    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
    }

    override fun onItemAtEndLoaded(itemAtEnd: HistoryContainer) {
        super.onItemAtEndLoaded(itemAtEnd)
    }

    /**
     * Loads the next page of history from the server on demand (e.g. when the user scrolls
     * near the end of the list). No-op while a load is in progress or once the end is reached.
     */
    @Synchronized
    fun loadNextPage() = io {
        if (!isLoading && !endReached) {
            isLoading = true
            val offset = latestLoadedPage * LIMIT
            val result = historyRepository.loadHistoryItems(offset, LIMIT)
            result.onFailure {
                errorChannel?.send(it)
            }
            result.onSuccess {
                if (it.isNullOrEmpty()) {
                    endReached = true
                } else {
                    latestLoadedPage++
                    if (it.size < LIMIT) endReached = true
                }
            }
            isLoading = false
        }
    }

    fun clear() = io {
        historyDao.clear()
    }

    fun refresh() = io {
        isLoading = true
        reportRefreshing(true)
        endReached = false
        val result = historyRepository.loadHistoryItems(0, LIMIT, false)
        result.onFailure {
            errorChannel?.send(it)
        }
        // Page 0 is now loaded, so advance the cursor. Otherwise the first on-demand
        // loadNextPage() would re-fetch offset 0 (a duplicate) before continuing.
        result.onSuccess {
            if (it.isNullOrEmpty()) {
                latestLoadedPage = 0
                endReached = true
            } else {
                latestLoadedPage = 1
                if (it.size < LIMIT) endReached = true
            }
        }
        reportRefreshing(false)
        isLoading = false
    }
}