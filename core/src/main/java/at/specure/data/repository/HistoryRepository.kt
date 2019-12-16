package at.specure.data.repository

import androidx.paging.DataSource
import androidx.paging.PagedList
import at.rmbt.util.Maybe
import at.rmbt.util.exception.HandledException
import at.specure.data.entity.History

interface HistoryRepository {

    fun getHistorySource(): DataSource.Factory<Int, History>

    fun clearHistory()

    fun boundaryCallback(
        limit: Int,
        onLoadingCallback: ((Boolean) -> Unit),
        onErrorCallback: ((HandledException) -> Unit)
    ): PagedList.BoundaryCallback<History>
}