package at.rmbt.cms.client

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CMSApiBuilder(val provider: CMSEndpointProvider) {
    private val client = OkHttpClient.Builder()
        .addInterceptor(CMSRequestInterceptor(provider))
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(provider.hostname)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    fun build(): CMSApi {
        return retrofit.create(CMSApi::class.java)
    }
}