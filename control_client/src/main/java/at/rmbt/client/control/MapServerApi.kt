package at.rmbt.client.control

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface MapServerApi {

    @POST
    fun getMarkers(@Url url: String, @Body body: MarkersRequestBody): Call<MarkersResponse>

    @GET
    @Headers("Accept: image/png")
    fun loadTiles(@Url url: String): Call<ResponseBody>

    @POST
    fun getFilters(@Url url: String, @Body body: FilterLanguageRequestBody): Call<MapFilterResponse>

    @GET
    fun loadOperators(@Header("X-Nettest-Client") nettestHeader: String?, @Url url: String): Call<NationalTableResponse>
}