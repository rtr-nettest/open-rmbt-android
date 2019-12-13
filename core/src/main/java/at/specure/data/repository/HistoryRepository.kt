package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rmbt.util.Maybe
import at.specure.data.entity.History

interface HistoryRepository {

    fun getHistory(): LiveData<List<History>>

    fun refreshHistory(callback: (Maybe<Boolean>) -> Unit)
}