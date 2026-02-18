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
    private var isLoading = false
        set(value) {
            field = value
            runBlocking {
                isLoadingChannel?.send(value)
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

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        loadItems()
    }

    override fun onItemAtEndLoaded(itemAtEnd: HistoryContainer) {
        super.onItemAtEndLoaded(itemAtEnd)
        loadItems()
    }

    @Synchronized
    private fun loadItems() = io {
        if (!isLoading) {
            isLoading = true

            val count = historyDao.getItemsCount()
            Timber.d("HistoryItemsCount: $count")
            if ((count % LIMIT == 0) && (count / LIMIT != latestLoadedPage)) {
                val result = historyRepository.loadHistoryItems(count, LIMIT)
                result.onFailure {
                    errorChannel?.send(it)
                }
                result.onSuccess {
                    latestLoadedPage = count / LIMIT
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
        historyRepository.loadHistoryItems(0, LIMIT, false).onFailure {
            errorChannel?.send(it)
        }
        isLoading = false
    }
}