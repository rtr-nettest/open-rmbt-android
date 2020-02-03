package at.specure.data.repository

import android.graphics.Bitmap
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
import at.rmbt.client.control.MapServerClient
import at.rmbt.client.control.MapTypeOptionsResponse
import at.rmbt.client.control.MarkersRequestBody
import at.rmbt.client.control.data.MapFilterType
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.util.io
import at.specure.data.CoreDatabase
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.data.getTypeTitle
import at.specure.data.toMap
import at.specure.data.toModelList
import at.specure.data.toSubtypesMap
import at.specure.util.ActiveFilter
import at.specure.util.FilterValuesStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.LinkedHashMap

class MapRepositoryImpl @Inject constructor(
    private val client: MapServerClient,
    private val db: CoreDatabase,
    override val storage: FilterValuesStorage,
    override val active: ActiveFilter
) : MapRepository {

    override val subtypesLiveData: FilterOptionLiveData<MapTypeOptionsResponse> = MutableLiveData()
    override val statisticalLiveData: FilterOptionLiveData<FilterStatisticOptionResponse> = MutableLiveData()
    override val periodLiveData: FilterOptionLiveData<FilterPeriodOptionResponse> = MutableLiveData()
    override val operatorLiveData: FilterOptionLiveData<FilterOperatorOptionResponse> = MutableLiveData()
    override val providerLiveData: FilterOptionLiveData<FilterProviderOptionResponse> = MutableLiveData()
    override val technologyLiveData: FilterOptionLiveData<FilterTechnologyOptionResponse> = MutableLiveData()

    override fun loadMarkers(latitude: Double?, longitude: Double?, zoom: Int, loaded: (Boolean) -> Unit) = io {
        val coordinates = Coordinates(latitude, longitude, zoom)

        val operator = if (active.operator?.operator.isNullOrBlank()) null else active.operator?.operator
        val technology = if (active.technology?.technology.isNullOrBlank()) null else active.technology?.technology
        val provider = if (active.provider?.provider.isNullOrBlank()) null else active.provider?.provider

        val body = MarkersRequestBody(
            language = Locale.getDefault().language,
            coordinates = coordinates,
            filter = Filter(operator, active.timeRange?.period.toString(), provider, active.statistical?.statisticalMethod.toString(), technology),
            options = MapOptions(active.subtype.mapOptions)
        )
        val result = client.getMarkers(body)
        result.onSuccess {
            db.mapDao().clear()
            db.mapDao().insert(it.toModelList())
        }
        loaded.invoke(result.ok)
    }

    override fun getMarkers(latitude: Double?, longitude: Double?, zoom: Int): LiveData<List<MarkerMeasurementRecord>> = db.mapDao().get()

    override fun loadTiles(x: Int, y: Int, zoom: Int, type: MapPresentationType): ByteArray? = runBlocking(Dispatchers.IO) {
        val result = client.loadTiles(x, y, zoom, type, prepareFilters())
        if (result.isSuccessful) {
            with(result.body()) {
                this?.let {
                    return@runBlocking bytes()
                }
            }
        }
        return@runBlocking null
    }

    override fun loadAutomaticTiles(x: Int, y: Int, zoom: Int): ByteArray? {
        val heatmapBytes = loadTiles(x, y, zoom, MapPresentationType.AUTOMATIC)
        val pointsBytes = loadTiles(x, y, zoom, MapPresentationType.POINTS)

        val heatmapBitmap = BitmapFactory.decodeByteArray(heatmapBytes, 0, heatmapBytes?.size ?: 0)
        val pointsBitmap = BitmapFactory.decodeByteArray(pointsBytes, 0, pointsBytes?.size ?: 0)

        val result = Bitmap.createBitmap(heatmapBitmap.width, heatmapBitmap.height, heatmapBitmap.config)
        val canvas = Canvas(result)
        canvas.drawBitmap(heatmapBitmap, 0f, 0f, null)
        canvas.drawBitmap(pointsBitmap, 0f, 0f, null)

        val stream = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    override fun prepareDetailsLink(openUUID: String) = client.prepareDetailsLink(openUUID)

    override fun obtainFilters(callback: (List<String>) -> Unit) = io {
        val result = client.obtainMapFiltersInfo(FilterLanguageRequestBody(Locale.getDefault().language))

        result.onSuccess {
            val types = LinkedHashMap<String, MapFilterType>()
            with(it.filter.mapTypes) {
                types[get(0).title] = MapFilterType.MOBILE
                types[get(1).title] = MapFilterType.WLAN
                types[get(2).title] = MapFilterType.BROWSER
                types[get(3).title] = MapFilterType.ALL
            }

            val subTypes = it.filter.toSubtypesMap(types) as MutableMap<MapFilterType, List<MapTypeOptionsResponse>>
            with(it.filter.mapFilters) {
                storage.apply {
                    init(types, subTypes, toMap(), toMap(), toMap(), toMap(), toMap())
                    titleStatistics = getTypeTitle<FilterStatisticOptionResponse>()
                    titlePeriod = getTypeTitle<FilterPeriodOptionResponse>()
                    titleOperator = getTypeTitle<FilterOperatorOptionResponse>()
                    titleProvider = getTypeTitle<FilterProviderOptionResponse>()
                    titleTechnology = getTypeTitle<FilterTechnologyOptionResponse>()
                }
                markTypeAsSelected(storage.findType(active.type))
            }
            callback.invoke(types.keys.toMutableList())
        }
        // todo: add error handling
    }

    override fun markTypeAsSelected(value: String) {
        (storage.findType(value) ?: MapFilterType.MOBILE).let {
            active.type = it

            storage.apply {
                findStatisticalList(it)?.let { statisticalLiveData.postValue(titleStatistics to it) }
                findSubtypesList(it)?.let { subtypesLiveData.postValue(titleSubtype to it) }
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

    private fun prepareFilters(): Map<String, String> {
        val filters = hashMapOf<String, String>()
        active.subtype?.mapOptions?.let { filters.put("map_options", it) }
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
}