package at.rtr.rmbt.android.viewmodel

import at.rtr.rmbt.android.ui.viewstate.ResultListFiltersViewState
import at.rtr.rmbt.android.util.combineWith
import at.specure.data.repository.HistoryRepository
import javax.inject.Inject

class ResultListFiltersViewModel @Inject constructor(private val repository: HistoryRepository) : BaseViewModel() {

    val state = ResultListFiltersViewState()

    init {
        addStateSaveHandler(state)
    }

    val networksLiveData = repository.networksLiveData.combineWith(repository.activeNetworksLiveData) { networks, activeNetworks ->
        val data = mutableListOf<FilterOption>()
        networks?.forEach {
            data.add(FilterOption(it, activeNetworks?.contains(it) == true))
        }
        return@combineWith data
    }

    val devicesLiveData = repository.devicesLiveData.combineWith(repository.activeDevicesLiveData) { devices, activeDevices ->
        val data = mutableListOf<FilterOption>()
        devices?.forEach {
            data.add(FilterOption(it, activeDevices?.contains(it) == true))
        }
        return@combineWith data
    }

    fun updateNetworkFilters(selected: Set<String>) {
        repository.saveFiltersNetwork(selected)
    }

    fun updateDeviceFilters(selected: Set<String>) {
        repository.saveFiltersDevices(selected)
    }

    class FilterOption(val option: String, val selected: Boolean)
}