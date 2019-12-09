package at.specure.data.repository

import at.specure.data.dao.HistoryDao
import at.specure.data.entity.History

class HistoryRepositoryImpl(private val historyDao: HistoryDao) : HistoryRepository {

    override suspend fun saveHistoryItems(historyItems: List<History>) {
        historyItems.forEach {
            historyDao.insert(it)
        }
    }

    override fun getHistoryItems(): List<History> {
        return historyDao.get()
    }
}