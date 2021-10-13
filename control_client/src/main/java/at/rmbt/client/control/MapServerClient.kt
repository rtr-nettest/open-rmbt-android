package at.rmbt.client.control

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.util.Maybe
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class MapServerClient @Inject constructor(
    private val endpointProvider: MapEndpointProvider,
    private val api: MapServerApi
) {

    fun getMarkers(body: MarkersRequestBody): Maybe<MarkersResponse> {
        return api.getMarkers(endpointProvider.getMapMarkersUrl, body).exec()
    }

    fun loadTiles(x: Int, y: Int, zoom: Int, type: MapPresentationType, filters: Map<String, String> = HashMap()): Response<ResponseBody> {
        val url = String.format(endpointProvider.getMapTilesUrl, type.value, zoom, x, y)
        val uriBuilder = Uri.parse(url).buildUpon()
        for (entry in filters.entries) {
            uriBuilder.appendQueryParameter(entry.key, entry.value)
        }
        return api.loadTiles(uriBuilder.build().toString()).execute()
    }

    fun prepareDetailsLink(openUUID: String) =
        MutableLiveData<String>().apply { postValue(String.format(endpointProvider.mapMarkerShowDetailsUrl, openUUID)) }

    fun obtainMapFiltersInfo(body: FilterLanguageRequestBody): Maybe<MapFilterResponse> = api.getFilters(endpointProvider.mapFilterInfoUrl, body).exec()

    fun obtainNationalTable(): Maybe<NationalTableResponse> = api.loadOperators(endpointProvider.nettestHeader, endpointProvider.getNationalTableUrl).exec()
}