package at.specure.data.repository

import androidx.paging.DataSource
import androidx.paging.PagedList
import at.rmbt.util.exception.HandledException
import at.specure.data.entity.History
import at.specure.data.entity.HistoryContainer
import at.specure.util.StringSetPreferenceLiveData

interface HistoryRepository {

    val activeNetworksLiveData: StringSetPreferenceLiveData
    val activeDevicesLiveData: StringSetPreferenceLiveData

    val networksLiveData: StringSetPreferenceLiveData
    val devicesLiveData: StringSetPreferenceLiveData

    val appliedFiltersLiveData: StringSetPreferenceLiveData

    fun getHistorySource(): DataSource.Factory<Int, HistoryContainer>

    fun boundaryCallback(
        limit: Int,
        onLoadingCallback: ((Boolean) -> Unit),
        onErrorCallback: ((HandledException) -> Unit)
    ): PagedList.BoundaryCallback<HistoryContainer>

    fun refreshHistory(limit: Int, onLoadingCallback: (Boolean) -> Unit, onErrorCallback: (HandledException) -> Unit)

    fun clearHistory()

    fun saveFiltersNetwork(selected: Set<String>)

    fun saveFiltersDevices(selected: Set<String>)

    fun removeFromFilters(value: String)

    fun getActiveNetworks(): Set<String>?

    fun getActiveDevices(): Set<String>?

    fun getNetworks(): Set<String>?

    fun getDevices(): Set<String>?
}