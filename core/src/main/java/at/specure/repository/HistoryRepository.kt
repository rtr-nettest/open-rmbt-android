package at.specure.repository

import at.specure.database.entity.History

interface HistoryRepository {

    fun getHistoryItems(): List<History>

    suspend fun saveHistoryItems(historyItems: List<History>)
}