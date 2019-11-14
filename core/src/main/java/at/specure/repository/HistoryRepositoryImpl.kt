package at.specure.repository

import androidx.lifecycle.MutableLiveData
import at.specure.database.dao.HistoryDao
import at.specure.database.entity.History

class HistoryRepositoryImpl(private val historyDao: HistoryDao) : HistoryRepository {

    override suspend fun saveHistoryItems(historyItems: List<History>) {
        historyItems.forEach {
            historyDao.insert(it)
        }
    }

    override fun getHistoryItems(): MutableLiveData<List<History>> {
        return historyDao.getHistoryItems()
    }
}