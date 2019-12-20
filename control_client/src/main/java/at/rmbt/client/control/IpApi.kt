package at.rmbt.client.control

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface IpApi {

    /**
     * Private IPv4 or IPv6 information check url
     */
    @POST
    fun ipCheck(@Url url: String, @Body body: IpRequestBody): Call<IpInfoResponse>
}