package at.specure.data.repository

import at.specure.data.entity.History

interface HistoryRepository {

    fun getHistoryItems(): List<History>

    suspend fun saveHistoryItems(historyItems: List<History>)
}