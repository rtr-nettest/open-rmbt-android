package at.rtr.rmbt.android.viewmodel

import at.rtr.rmbt.android.ui.viewstate.HistoryFiltersViewState
import at.specure.data.repository.HistoryRepository
import javax.inject.Inject

class HistoryFiltersViewModel @Inject constructor(private val repository: HistoryRepository) : BaseViewModel() {

    val state = HistoryFiltersViewState()

    init {
        addStateSaveHandler(state)
    }

    val activeNetworksLiveData = repository.activeNetworksLiveData
    val activeDevicesLiveData = repository.activeDevicesLiveData

    val networksLiveData = repository.networksLiveData
    val devicesLiveData = repository.devicesLiveData

    fun updateNetworkFilters(selected: Set<String>) {
        repository.saveFiltersNetwork(selected)
    }

    fun updateDeviceFilters(selected: Set<String>) {
        repository.saveFiltersDevices(selected)
    }

    fun displayStringSet(data: Set<String>?, defaultData: Set<String>? = null): String {
        return when {
            data == null || data.isEmpty() -> {
                if (!defaultData.isNullOrEmpty()) {
                    displayStringSet(defaultData)
                } else {
                    ""
                }
            }
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
}