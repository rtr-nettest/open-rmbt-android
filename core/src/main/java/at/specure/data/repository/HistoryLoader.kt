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
import javax.inject.Inject

private const val LIMIT = 100

class HistoryLoader @Inject constructor(
    db: CoreDatabase,
    private val historyRepository: HistoryRepository
) :
    PagedList.BoundaryCallback<HistoryContainer>() {

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
        val source = historyDao.getHistorySource()
        val config = PagedList.Config.Builder()
            .setPageSize(LIMIT)
            .build()
        LivePagedListBuilder(source, config)
            .setBoundaryCallback(this)
            .build()
    }

    val source: DataSource.Factory<Int, HistoryContainer>
        get() = historyDao.getHistorySource()

    override fun onZeroItemsLoaded() {
        super.onZeroItemsLoaded()
        loadItems()
    }

    override fun onItemAtEndLoaded(itemAtEnd: HistoryContainer) {
        super.onItemAtEndLoaded(itemAtEnd)
        loadItems()
    }

    private fun loadItems() = io {
        if (!isLoading) {
            isLoading = true

            val count = historyDao.getItemsCount()
            val result = historyRepository.loadHistoryItems(count, LIMIT)

            result.onFailure {
                errorChannel?.send(it)
            }

            isLoading = false
        }
    }

    fun clear() = io {
        historyDao.clear()
    }

    fun refresh() = io {
        isLoading = true
        historyRepository.loadHistoryItems(0, LIMIT).onFailure {
            errorChannel?.send(it)
        }
        isLoading = false
    }
}