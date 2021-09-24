package at.specure.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.FilterOperatorOptionResponse
import at.rmbt.client.control.FilterPeriodOptionResponse
import at.rmbt.client.control.FilterProviderOptionResponse
import at.rmbt.client.control.FilterStatisticOptionResponse
import at.rmbt.client.control.FilterTechnologyOptionResponse
import at.rmbt.client.control.MapTypeOptionsResponse
import at.rmbt.client.control.data.MapPresentationType
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.util.ActiveFilter
import at.specure.util.FilterValuesStorage

interface MapRepository {

    val active: ActiveFilter
    val storage: FilterValuesStorage

    val subtypesLiveData: FilterOptionLiveData<MapTypeOptionsResponse>
    val statisticalLiveData: FilterOptionLiveData<FilterStatisticOptionResponse>
    val periodLiveData: FilterOptionLiveData<FilterPeriodOptionResponse>
    val operatorLiveData: FilterOptionLiveData<FilterOperatorOptionResponse>
    val providerLiveData: FilterOptionLiveData<FilterProviderOptionResponse>
    val technologyLiveData: FilterOptionLiveData<FilterTechnologyOptionResponse>

    fun loadMarkers(latitude: Double?, longitude: Double?, zoom: Int, loaded: (Boolean) -> Unit)

    fun getMarkers(latitude: Double?, longitude: Double?, zoom: Int): LiveData<List<MarkerMeasurementRecord>>

    fun loadTiles(x: Int, y: Int, zoom: Int, type: MapPresentationType): ByteArray?

    fun loadAutomaticTiles(x: Int, y: Int, zoom: Int): ByteArray?

    fun prepareDetailsLink(openUUID: String): LiveData<String>

    fun obtainFilters(callback: (List<String>) -> Unit)

    fun obtainProviders(callback: (MutableList<String>) -> Unit)

    fun markTypeAsSelected(value: String)

    fun markSubtypeAsSelected(value: String)

    fun markStatisticsAsSelected(value: String)

    fun markPeriodAsSelected(value: String)

    fun markProviderAsSelected(value: String)

    fun markOperatorAsSelected(value: String)

    fun markTechnologyAsSelected(value: String)
}

typealias FilterOptionLiveData<T> = MutableLiveData<Pair<String, List<T>>>