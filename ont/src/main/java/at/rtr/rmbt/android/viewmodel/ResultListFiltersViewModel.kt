package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.Transformations
import at.rtr.rmbt.android.ui.viewstate.ResultListFiltersViewState
import at.rtr.rmbt.android.util.combineWith
import at.specure.data.repository.HistoryRepository
import javax.inject.Inject

class ResultListFiltersViewModel @Inject constructor(private val repository: HistoryRepository) : BaseViewModel() {

    val state = ResultListFiltersViewState()

    init {
        addStateSaveHandler(state)
    }

    val activeNetworksLiveData = Transformations.map(repository.activeNetworksLiveData) {
//        state.run {
//            activeNetworks = it
//            val displayString = displayStringSet(it, defaultNetwors)
//            networks.set(displayString)
//        }
        //       return@map it
    }
    val activeDevicesLiveData = Transformations.map(repository.activeDevicesLiveData) {
//        state.run {
//            activeDevices = it
//            val displayString = displayStringSet(it, defaultDevices)
//            devices.set(displayString)
//        }
//        return@map it
    }

    val networksLiveData = repository.networksLiveData.combineWith(repository.activeNetworksLiveData) { networks, activeNetworks ->
        val data = mutableListOf<FilterOption>()
        networks?.forEach {
            data.add(FilterOption(it, activeNetworks?.contains(it) == true))
        }
        return@combineWith data

//        state.defaultNetwors = it
        //       it
    }
    val devicesLiveData = repository.devicesLiveData.combineWith(repository.activeDevicesLiveData) { devices, activeDevices ->
        val data = mutableListOf<FilterOption>()
        devices?.forEach {
            data.add(FilterOption(it, activeDevices?.contains(it) == true))
        }
        return@combineWith data
//        state.defaultDevices = it
    }

//    val activeNetworksLiveData = repository.activeNetworksLiveData
//    val activeDevicesLiveData = repository.activeDevicesLiveData

//    val networksLiveData = repository.networksLiveData
//    val devicesLiveData = repository.devicesLiveData

    fun updateNetworkFilters(selected: Set<String>) {
        repository.saveFiltersNetwork(selected)
    }

    fun updateDeviceFilters(selected: Set<String>) {
        repository.saveFiltersDevices(selected)
    }

    fun displayStringSet(data: Set<String>?, defaultData: Set<String>? = null): String {
        return when {
            data == null || data.isEmpty() -> displayStringSet(defaultData)
            data.size == 1 -> data.first()
            else -> {
                buildString {
                    data.forEach {
                        if (it != data.first()) {
                            append(", ")
                        }
                        append(it)
                    }
                }
            }
        }
    }

    class FilterOption(val option: String, val selected: Boolean)
}