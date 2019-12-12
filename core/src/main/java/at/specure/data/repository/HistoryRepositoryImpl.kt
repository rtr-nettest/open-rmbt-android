package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rmbt.client.control.ControlServerClient
import at.rmbt.util.Maybe
import at.rmbt.util.io
import at.specure.data.dao.HistoryDao
import at.specure.data.entity.History
import timber.log.Timber

class HistoryRepositoryImpl(private val historyDao: HistoryDao, private val client: ControlServerClient) : HistoryRepository {

    override fun getHistory(): LiveData<List<History>> = historyDao.get()

    override fun refreshHistory(callback: (Maybe<Boolean>) -> Unit) = io {
        val response = client.getHistroty()
        Timber.d("History received")
    }
}