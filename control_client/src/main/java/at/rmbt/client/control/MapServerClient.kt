package at.rmbt.client.control

import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.util.Maybe
import okhttp3.ResponseBody
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import androidx.core.net.toUri

class MapServerClient @Inject constructor(
    private val endpointProvider: MapEndpointProvider,
    private val api: MapServerApi
) {

    fun getMarkers(body: MarkersRequestBody): Maybe<MarkersResponse> {
        return api.getMarkers(endpointProvider.getMapMarkersUrl, body).exec()
    }

    fun loadTiles(x: Int, y: Int, zoom: Int, type: MapPresentationType, filters: Map<String, String> = HashMap()): Response<ResponseBody>? {
        return try {
            val url = String.format(endpointProvider.getMapTilesUrl, type.value, zoom, x, y)
            val uriBuilder = url.toUri().buildUpon()
            for (entry in filters.entries) {
                uriBuilder.appendQueryParameter(entry.key, entry.value)
            }
            api.loadTiles(uriBuilder.build().toString()).execute()
        } catch(e: Exception) {
            Timber.e("Map tiles loading exception ${e.localizedMessage}")
            null
        }
    }

    fun prepareDetailsLink(openUUID: String) =
        MutableLiveData<String>().apply { postValue(String.format(endpointProvider.mapMarkerShowDetailsUrl, openUUID)) }

    fun obtainMapFiltersInfo(body: FilterLanguageRequestBody): Maybe<MapFilterResponse> = api.getFilters(endpointProvider.mapFilterInfoUrl, body).exec()

    fun obtainNationalTable(): Maybe<NationalTableResponse> = api.loadOperators(endpointProvider.nettestHeader, endpointProvider.getNationalTableUrl).exec()
}