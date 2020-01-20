package at.rmbt.client.control

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface MapServerApi {

    @POST
    fun getMarkers(@Url url: String, @Body body: MarkersRequestBody): Call<MarkersResponse>

    @GET
    fun loadTiles(@Url url: String): Call<ResponseBody>
}