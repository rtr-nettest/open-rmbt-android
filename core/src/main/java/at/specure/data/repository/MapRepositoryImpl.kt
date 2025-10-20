package at.specure.data.repository

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.Coordinates
import at.rmbt.client.control.Filter
import at.rmbt.client.control.FilterLanguageRequestBody
import at.rmbt.client.control.FilterOperatorOptionResponse
import at.rmbt.client.control.FilterPeriodOptionResponse
import at.rmbt.client.control.FilterProviderOptionResponse
import at.rmbt.client.control.FilterStatisticOptionResponse
import at.rmbt.client.control.FilterTechnologyOptionResponse
import at.rmbt.client.control.MapOptions
import at.rmbt.client.control.MapFilterItemV2
import at.rmbt.client.control.FilterBaseOptionResponseV2
import at.rmbt.client.control.MapFilterTypeClass
import at.rmbt.client.control.MapServerClient
import at.rmbt.client.control.MapTypeOptionsResponse
import at.rmbt.client.control.MarkersRequestBody
import at.rmbt.client.control.ProviderStatistics
import at.rmbt.client.control.data.MapFilterData
import at.rmbt.client.control.data.MapFilterType
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.ControlServerSettings
import at.specure.data.CoreDatabase
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.data.toModelList
import at.specure.util.ActiveFilter
import at.specure.util.FilterValuesStorage
import at.specure.util.getCurrentLatestFinishedMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class MapRepositoryImpl @Inject constructor(
    private val client: MapServerClient,
    private val db: CoreDatabase,
    override val storage: FilterValuesStorage,
    override val active: ActiveFilter,
    private val controlServerSettings: ControlServerSettings,
    private val config: Config
) : MapRepository {

    override val subtypesLiveData: FilterOptionLiveData<MapTypeOptionsResponse> = MutableLiveData()
    override val statisticalLiveData: FilterOptionLiveData<FilterStatisticOptionResponse> = MutableLiveData()
    override val periodLiveData: FilterOptionLiveData<FilterPeriodOptionResponse> = MutableLiveData()
    override val operatorLiveData: FilterOptionLiveData<FilterOperatorOptionResponse> = MutableLiveData()
    override val providerLiveData: FilterOptionLiveData<FilterProviderOptionResponse> = MutableLiveData()
    override val technologyLiveData: FilterOptionLiveData<FilterTechnologyOptionResponse> = MutableLiveData()

    override fun loadMarkers(latitude: Double?, longitude: Double?, zoom: Int, loaded: (Boolean) -> Unit) = io {
        val coordinates = Coordinates(latitude, longitude, zoom)

        val operator = if (active.type != MapFilterType.MOBILE || active.operator.operator.isBlank()) null else active.operator.operator
        val technology = if (active.type != MapFilterType.MOBILE || active.technology.technology.isBlank()) null else active.technology.technology
        val provider = if (active.type == MapFilterType.MOBILE || active.provider.provider.isBlank()) null else active.provider.provider

        val body = MarkersRequestBody(
            language = Locale.getDefault().language,
            coordinates = coordinates,
            filter = Filter(operator, active.timeRange.period.toString(), provider, active.statistical.statisticalMethod.toString(), technology),
            options = MapOptions(active.subtype.mapOptions)
        )
        val result = client.getMarkers(body)
        result.onSuccess {
            val clearCount = db.mapDao().clear()
            if (clearCount == 0) {
                Timber.e("DB: failed to clear map markers")
            }
            db.mapDao().insert(it.toModelList())
        }
        loaded.invoke(result.ok)
    }

    override fun getMarkers(latitude: Double?, longitude: Double?, zoom: Int): LiveData<List<MarkerMeasurementRecord>> = db.mapDao().get()

    override fun loadTiles(x: Int, y: Int, zoom: Int, type: MapPresentationType): ByteArray? = runBlocking(Dispatchers.IO) {
        val result = client.loadTiles(x, y, zoom, type, prepareFilters())
        if (result?.isSuccessful == true) {
            with(result.body()) {
                this?.let {
                    return@runBlocking bytes()
                }
            }
        }
        return@runBlocking byteArrayOf()
    }

    override fun loadAutomaticTiles(x: Int, y: Int, zoom: Int): ByteArray? {
        val heatmapBytes = loadTiles(x, y, zoom, MapPresentationType.AUTOMATIC)
        val pointsBytes = loadTiles(x, y, zoom, MapPresentationType.POINTS)

        val heatmapBitmap = BitmapFactory.decodeByteArray(heatmapBytes, 0, heatmapBytes?.size ?: 0)
        val pointsBitmap = BitmapFactory.decodeByteArray(pointsBytes, 0, pointsBytes?.size ?: 0)

        val stream = ByteArrayOutputStream()

        heatmapBitmap.config?.let {
            val result = createBitmap(heatmapBitmap.width, heatmapBitmap.height, it)
            val canvas = Canvas(result)
            canvas.drawBitmap(heatmapBitmap, 0f, 0f, null)
            canvas.drawBitmap(pointsBitmap, 0f, 0f, null)
            result.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        return stream.toByteArray()
    }

    override fun prepareDetailsLink(openUUID: String): LiveData<String> {
        return if (controlServerSettings.openDataPrefix.isNullOrEmpty()) {
            client.prepareDetailsLink(openUUID)
        } else {
            MutableLiveData<String>().apply { postValue(controlServerSettings.openDataPrefix + openUUID + "#noMMenu") }
        }
    }

    override fun obtainFilters(callback: (MapFilterData) -> Unit) = io {

        if (config.headerValue.isNullOrEmpty()) {

            val result = client.obtainMapFiltersInfo(FilterLanguageRequestBody(Locale.getDefault().language))

            result.onSuccess { mapFilterResponse ->
                mapFilterResponse.filters?.let {
                    val types = LinkedHashMap<String, MapFilterType>()
                    val subTypes: MutableMap<MapFilterType, List<MapTypeOptionsResponse>> = hashMapOf()

                    val mapTypes = mapFilterResponse.filters?.firstOrNull { item -> item.icon == MapFilterTypeClass.MAP_TYPE.serverValue }

                    mapTypes?.options?.forEach { mapTypeOptions ->
                        val filterType =
                            mapTypeOptions.options?.get(0)?.params?.mapOptions?.split('/')?.get(0) ?: "all"
                        filterType?.let {
                            MapFilterType.fromFilterString(it)?.let { filterType ->
                                types[mapTypeOptions.title] = filterType
                                mapTypeOptions.options?.let { mapTypesOptions ->
                                    subTypes[filterType] = mapTypesOptions.toV1MapOptions()
                                }
                            }
                        }
                    }

                    val otherFilters = mapFilterResponse.filters?.filter { item -> item.icon != MapFilterTypeClass.MAP_TYPE.serverValue }
                    var statistics: Map<MapFilterType, List<FilterStatisticOptionResponse>> = hashMapOf<MapFilterType, List<FilterStatisticOptionResponse>>()
                    var period: Map<MapFilterType, List<FilterPeriodOptionResponse>> = hashMapOf<MapFilterType, List<FilterPeriodOptionResponse>>()
                    var technology: Map<MapFilterType, List<FilterTechnologyOptionResponse>> = hashMapOf<MapFilterType, List<FilterTechnologyOptionResponse>>()
                    var operator: Map<MapFilterType, List<FilterOperatorOptionResponse>> = hashMapOf<MapFilterType, List<FilterOperatorOptionResponse>>()
                    var provider: Map<MapFilterType, List<FilterProviderOptionResponse>> = hashMapOf<MapFilterType, List<FilterProviderOptionResponse>>()

                    otherFilters?.forEach { filterItem ->
                        MapFilterTypeClass.fromServerString(filterItem.icon ?: MapFilterTypeClass.MAP_FILTER_STATISTIC.serverValue)?.let { filterType ->
                            when (filterType) {
                                MapFilterTypeClass.MAP_FILTER_TECHNOLOGY -> {
                                    storage.titleTechnology = filterItem.title
                                    technology = filterItem.toTechnologyMap()
                                }
                                MapFilterTypeClass.MAP_FILTER_CARRIER -> {
                                    if (filterItem.dependsOn?.mapTypeIsMobile == true) {
                                        storage.titleOperator = filterItem.title
                                        operator = filterItem.toOperatorMap()
                                    } else {
                                        storage.titleProvider = filterItem.title
                                        provider = filterItem.toProviderMap()
                                    }
                                }
                                MapFilterTypeClass.MAP_FILTER_PERIOD -> {
                                    storage.titlePeriod = filterItem.title
                                    period = filterItem.toPeriodMap()
                                }
                                MapFilterTypeClass.MAP_FILTER_STATISTIC -> {
                                    storage.titleStatistics = filterItem.title
                                    statistics = filterItem.toStatisticMap()
                                }
                                else -> {
                                    // do nothing
                                }
                            }
                        }
                    }

                    storage.apply {
                        init(types, subTypes, statistics, period, technology, operator, provider, Calendar.getInstance().getCurrentLatestFinishedMonth())
                    }
                    markTypeAsSelected(storage.findType(active.type))

                    callback.invoke(
                        MapFilterData(
                            filterData = types.keys.toMutableList(),
                        )
                    )
                }
            }

            result.onFailure {
                callback.invoke(
                    MapFilterData(
                        filterData = mutableListOf(),
                        exception = it
                    )
                )
                Timber.d("Loading map filters failed $it")
            }
        } else {
            val types = LinkedHashMap<String, MapFilterType>()
            types[MapFilterType.MOBILE.name] = MapFilterType.MOBILE
            types[MapFilterType.WLAN.name] = MapFilterType.WLAN
            types[MapFilterType.BROWSER.name] = MapFilterType.BROWSER
            types[MapFilterType.ALL.name] = MapFilterType.ALL

            val subTypes = createCleanFilterMap<MapTypeOptionsResponse>()

            val technologyList = listOf(
                FilterTechnologyOptionResponse("ALL").apply {
                    title = "ALL"
                    default = true
                },
                FilterTechnologyOptionResponse("2G").apply {
                    title = "2G"
                },
                FilterTechnologyOptionResponse("3G").apply {
                    title = "3G"
                },
                FilterTechnologyOptionResponse("4G").apply {
                    title = "4G"
                },
                FilterTechnologyOptionResponse("5G").apply {
                    title = "5G"
                }
            )

            val technology = createCleanFilterMap<FilterTechnologyOptionResponse>()
            technology[MapFilterType.MOBILE] = technologyList
            technology[MapFilterType.WLAN] = emptyList()
            technology[MapFilterType.BROWSER] = technologyList
            technology[MapFilterType.ALL] = technologyList

//            val result = client.obtainNationalTable()
//
//            result.onSuccess {
//
//                val providerFilterOptions = it.providerStats.toProviderNameList()
//
//                val providers = createCleanFilterMap<FilterProviderOptionResponse>()
//                providers[MapFilterType.MOBILE] = providerFilterOptions
//                providers[MapFilterType.WLAN] = providerFilterOptions
//                providers[MapFilterType.BROWSER] = providerFilterOptions
//                providers[MapFilterType.ALL] = providerFilterOptions
//
//                storage.apply {
//                    init(types,
//                        subTypes,
//                        createCleanFilterMap<FilterStatisticOptionResponse>(),
//                        createCleanFilterMap<FilterPeriodOptionResponse>(),
//                        technology,
//                        createCleanFilterMap<FilterOperatorOptionResponse>(),
//                        providers,
//                    )
//                }
//                markTypeAsSelected(storage.findType(active.type))
//                callback.invoke(types.keys.toMutableList())
//            }
//            result.onFailure {
            storage.apply {
                init(
                    types,
                    subTypes,
                    createCleanFilterMap<FilterStatisticOptionResponse>(),
                    createCleanFilterMap<FilterPeriodOptionResponse>(),
                    technology,
                    createCleanFilterMap<FilterOperatorOptionResponse>(),
                    createCleanFilterMap<FilterProviderOptionResponse>(),
                    Calendar.getInstance().getCurrentLatestFinishedMonth()
                )
            }
            markTypeAsSelected(storage.findType(active.type))
            callback.invoke(
                MapFilterData(
                    filterData = types.keys.toMutableList()
                )
            )
//            }
        }
        // todo: add error handling
    }

    override fun obtainProviders(callback: (MutableList<String>) -> Unit) = io {
        var providers: MutableList<String> = arrayListOf()
        client.obtainNationalTable().onSuccess {
            providers = it.providerStats!!.map { provider -> provider.providerName ?: "" }.toMutableList()
        }
        callback(providers)
    }

    private fun <T> createCleanFilterMap(): MutableMap<MapFilterType, List<T>> {
        val subTypes = mutableMapOf<MapFilterType, List<T>>()
        subTypes[MapFilterType.MOBILE] = mutableListOf<T>()
        subTypes[MapFilterType.WLAN] = mutableListOf<T>()
        subTypes[MapFilterType.BROWSER] = mutableListOf<T>()
        subTypes[MapFilterType.ALL] = mutableListOf<T>()
        return subTypes
    }

    override fun markTypeAsSelected(value: String) {
        (storage.findType(value) ?: MapFilterType.MOBILE).let {
            active.type = it

            storage.run {
                findStatisticalList(it)?.let { statisticalLiveData.postValue(titleStatistics to it) }
                findSubtypesList(it)?.let { subtypesLiveData.postValue(value to it) }
                findPeriodsList(it)?.let { periodLiveData.postValue(titlePeriod to it) }
                findOperatorList(it)?.let { operatorLiveData.postValue(titleOperator to it) }
                findProviderList(it)?.let { providerLiveData.postValue(titleProvider to it) }
                findTechnologyList(it)?.let { technologyLiveData.postValue(titleTechnology to it) }
            }
        }
    }

    override fun markSubtypeAsSelected(value: String) {
        active.subtype = storage.findSubtype(value, active.type)
    }

    override fun markStatisticsAsSelected(value: String) {
        active.statistical = storage.findStatistical(value, active.type)
    }

    override fun markPeriodAsSelected(value: String) {
        active.timeRange = storage.findPeriod(value, active.type)
    }

    override fun markProviderAsSelected(value: String) {
        active.provider = storage.findProvider(value, active.type)
    }

    override fun markOperatorAsSelected(value: String) {
        active.operator = storage.findOperator(value, active.type)
    }

    override fun markTechnologyAsSelected(value: String) {
        active.technology = storage.findTechnology(value, active.type)
    }

    override fun setTimeSelected(year: Int, month: Int) {
        val selectedTime = Pair<Int, Int>(month, year)
        active.time = selectedTime
    }

    override fun getTimeSelected(): Pair<Int, Int> {
        return active.time
    }

    private fun prepareFilters(): Map<String, String> {
        val filters = hashMapOf<String, String>()
        filters["map_options"] = active.subtype.mapOptions
        filters["statistical_method"] = active.statistical?.statisticalMethod.toString()
        filters["period"] = active.timeRange?.period.toString()
        if (active.type == MapFilterType.MOBILE) {
            active.technology?.technology?.let { filters.put("technology", it) }
            active.operator?.operator?.let { filters.put("operator", it) }
        }
        if (active.type == MapFilterType.BROWSER || active.type == MapFilterType.WLAN) {
            active.provider?.provider?.let { filters.put("provider", it) }
        }
        return filters
    }

    private fun List<ProviderStatistics>?.toProviderNameList(): List<FilterProviderOptionResponse> {
        var providerList = mutableListOf<FilterProviderOptionResponse>()
        this?.forEach {
            it.providerName?.let { providerName ->
                providerList.add(FilterProviderOptionResponse(providerName))
            }
        }
        return providerList
    }
}

private fun MapFilterItemV2.toTechnologyMap(): Map<MapFilterType, List<FilterTechnologyOptionResponse>> {
    val technologyMap = hashMapOf<MapFilterType, List<FilterTechnologyOptionResponse>>()
    val optionList = mutableListOf<FilterTechnologyOptionResponse>()

    this.options.mapTo(optionList, {
        val technologyOptions = FilterTechnologyOptionResponse(technology = it.params?.technology ?: "")
        technologyOptions.apply {
            title = it.title
            summary = it.summary ?: ""
            default = it.default
        }
    })

    MapFilterType.values().forEach {
        technologyMap[it] = optionList
    }
    return technologyMap
}

private fun MapFilterItemV2.toOperatorMap(): Map<MapFilterType, List<FilterOperatorOptionResponse>> {
    val operatorMap = hashMapOf<MapFilterType, List<FilterOperatorOptionResponse>>()
    val optionList = mutableListOf<FilterOperatorOptionResponse>()

    this.options.mapTo(optionList, {
        val operatorOptions = FilterOperatorOptionResponse(operator = it.params?.operator ?: "")
        operatorOptions.apply {
            title = it.title
            summary = it.summary ?: ""
            default = it.default
        }
    })

    MapFilterType.values().forEach {
        operatorMap[it] = optionList
    }
    return operatorMap
}

private fun MapFilterItemV2.toProviderMap(): Map<MapFilterType, List<FilterProviderOptionResponse>> {
    val providerMap = hashMapOf<MapFilterType, List<FilterProviderOptionResponse>>()
    val optionList = mutableListOf<FilterProviderOptionResponse>()

    this.options.mapTo(optionList, {
        val providerOptions = FilterProviderOptionResponse(provider = it.params?.provider ?: "")
        providerOptions.apply {
            title = it.title
            summary = it.summary ?: ""
            default = it.default
        }
    })

    MapFilterType.values().forEach {
        providerMap[it] = optionList
    }
    return providerMap
}

private fun MapFilterItemV2.toPeriodMap(): Map<MapFilterType, List<FilterPeriodOptionResponse>> {
    val periodMap = hashMapOf<MapFilterType, List<FilterPeriodOptionResponse>>()
    val optionList = mutableListOf<FilterPeriodOptionResponse>()

    this.options.mapTo(optionList, { optionItem ->
        val providerOptions = FilterPeriodOptionResponse(period = optionItem.params?.period ?: 1)
        providerOptions.apply {
            title = optionItem.title
            summary = optionItem.summary ?: ""
            default = optionItem.default
        }
    })

    MapFilterType.values().forEach {
        periodMap[it] = optionList
    }
    return periodMap
}

private fun MapFilterItemV2.toStatisticMap(): Map<MapFilterType, List<FilterStatisticOptionResponse>> {
    val statisticMap = hashMapOf<MapFilterType, List<FilterStatisticOptionResponse>>()
    val optionList = mutableListOf<FilterStatisticOptionResponse>()

    this.options.mapTo(optionList, { optionItem ->
        val providerOptions = FilterStatisticOptionResponse(statisticalMethod =  optionItem.params?.statisticalMethod ?: 0.0)
        providerOptions.apply {
            title = optionItem.title
            summary = optionItem.summary ?: ""
            default = optionItem.default
        }
    })

    MapFilterType.values().forEach {
        statisticMap[it] = optionList
    }
    return statisticMap
}

private fun List<FilterBaseOptionResponseV2>.toV1MapOptions(): List<MapTypeOptionsResponse> {
    val optionList = mutableListOf<MapTypeOptionsResponse>()
    this.mapTo(optionList, {
        MapTypeOptionsResponse(
            mapOptions = it.params?.mapOptions ?: "",
            summary = it.summary  ?: "",
            title = it.title
        )
    })
    return optionList
}
