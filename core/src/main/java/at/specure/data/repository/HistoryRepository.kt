package at.specure.data.repository

import at.rmbt.util.Maybe
import at.specure.data.entity.History
import at.specure.util.StringSetPreferenceLiveData

interface HistoryRepository {

    val activeNetworksLiveData: StringSetPreferenceLiveData
    val activeDevicesLiveData: StringSetPreferenceLiveData

    val networksLiveData: StringSetPreferenceLiveData
    val devicesLiveData: StringSetPreferenceLiveData

    val appliedFiltersLiveData: StringSetPreferenceLiveData

    fun saveFiltersNetwork(selected: Set<String>)

    fun saveFiltersDevices(selected: Set<String>)

    fun removeFromFilters(value: String)

    fun getActiveNetworks(): Set<String>?

    fun getActiveDevices(): Set<String>?

    fun getNetworks(): Set<String>?

    fun getDevices(): Set<String>?

    fun loadHistoryItems(offset: Int, limit: Int): Maybe<List<History>>

    fun cleanHistory()
}