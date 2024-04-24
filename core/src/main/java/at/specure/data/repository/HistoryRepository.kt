package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rmbt.util.Maybe
import at.specure.data.HistoryLoopMedian
import at.specure.data.entity.History
import at.specure.util.StringSetPreferenceLiveData
import kotlinx.coroutines.flow.Flow

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

    fun getLoadedHistoryItems(limit: Int): LiveData<List<History>?>

    fun loadHistoryItems(offset: Int, limit: Int): Maybe<List<History?>?>

    fun loadHistoryItems(offset: Int, limit: Int, ignoreFilters: Boolean): Maybe<List<History?>?>

    fun loadLoopHistoryItems(loopUuid: String): Flow<List<History>?>

    fun loadLoopMedianValues(loopUuid: String): Flow<HistoryLoopMedian?>

    fun getLoopMedianValues(loopUuid: String): LiveData<HistoryLoopMedian?>

    fun getLoopHistoryItems(loopUuid: String): LiveData<List<History>?>

    fun cleanHistory()
}