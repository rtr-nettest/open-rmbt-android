package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.data.MapFilterData
import at.rmbt.client.control.data.MapFilterType
import at.rtr.rmbt.android.ui.viewstate.MapFilterViewState
import at.specure.data.repository.MapRepository
import javax.inject.Inject

class MapFiltersViewModel @Inject constructor(private val repository: MapRepository) : BaseViewModel() {

    val state = MapFilterViewState()

    val typesLiveData: MutableLiveData<MapFilterData> = MutableLiveData()

    val statisticsLiveData = repository.statisticalLiveData
    val subtypesLiveData = repository.subtypesLiveData
    val periodLiveData = repository.periodLiveData
    val technologyData = repository.technologyLiveData
    val operatorLiveData = repository.operatorLiveData
    val providerLiveData = repository.providerLiveData

    private val storage = repository.storage

    init {
        addStateSaveHandler(state)

        state.apply {
            statisticsTitle.set(storage.titleStatistics)
            periodTitle.set(storage.titlePeriod)
            operatorTitle.set(storage.titleOperator)
            providerTitle.set(storage.titleProvider)
            technologyTitle.set(storage.titleTechnology)

            technologyVisibility.set(repository.active.type == MapFilterType.MOBILE)
            operatorVisibility.set(repository.active.type == MapFilterType.MOBILE)
            providerVisibility.set(repository.active.type == MapFilterType.WLAN || repository.active.type == MapFilterType.BROWSER)
        }
    }

    fun obtain() {
        repository.obtainFilters {
            typesLiveData.postValue(it)
            updateState()
        }
    }

    fun markTypeAsSelected(value: String) {
        repository.markTypeAsSelected(value)
        updateState()
    }

    fun markSubtypeAsSelected(value: String) {
        repository.markSubtypeAsSelected(value)
        updateState()
    }

    fun markStatisticsAsSelected(value: String) {
        repository.markStatisticsAsSelected(value)
        updateState()
    }

    fun markPeriodAsSelected(value: String) {
        repository.markPeriodAsSelected(value)
        updateState()
    }

    fun markProviderAsSelected(value: String) {
        repository.markProviderAsSelected(value)
        updateState()
    }

    fun markOperatorAsSelected(value: String) {
        repository.markOperatorAsSelected(value)
        updateState()
    }

    fun markTechnologyAsSelected(value: String) {
        repository.markTechnologyAsSelected(value)
        updateState()
    }

    private fun updateState() {
        state.apply {
            type.set(storage.findType(repository.active.type))
            if (repository.active.type == MapFilterType.WLAN || repository.active.type == MapFilterType.BROWSER) {
                provider.set(repository.active.provider)
            }
            if (repository.active.type == MapFilterType.MOBILE) {
                technology.set(repository.active.technology)
                operator.set(repository.active.operator)
            }
            timeRange.set(repository.active.timeRange)
            statistical.set(repository.active.statistical)
            subtype.set(repository.active.subtype)
            displayType.set(
                if (type.get().isNullOrBlank() && subtype.get() == null) ""
                else if (subtype.get() == null) type.get()
                else "${type.get()}, ${subtype.get()!!.title}"
            )
            technologyVisibility.set(repository.active.type == MapFilterType.MOBILE)
            operatorVisibility.set(repository.active.type == MapFilterType.MOBILE)
            providerVisibility.set(repository.active.type == MapFilterType.WLAN || repository.active.type == MapFilterType.BROWSER)
        }
    }
}