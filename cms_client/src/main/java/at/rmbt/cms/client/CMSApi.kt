package at.rmbt.cms.client

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface CMSApi {
    @GET("pages?_limit=1")
    fun getPage(@Query("menu_item.route") route: String): Call<PageResponse>
}