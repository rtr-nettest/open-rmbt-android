package at.specure.repository

import androidx.lifecycle.MutableLiveData
import at.specure.database.entity.History

interface HistoryRepository {

    fun getHistoryItems(): MutableLiveData<List<History>>

    suspend fun saveHistoryItems(historyItems: List<History>)
}