/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.rmbt.client.control

import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val CONNECTION_TIMEOUT_SEC = 30L
private const val READ_TIMEOUT_SEC = 10L

/**
 * Temporary module for providing Retrofit instance
 */
@Module
class ControlServerModule {

    @Provides
    @Singleton
    fun provideControlServerApi(retrofit: Retrofit): ControlServerApi =
        retrofit.create(ControlServerApi::class.java)

    @Provides
    @Singleton
    fun provideIpServerApi(retrofit: Retrofit): IpApi =
        retrofit.create(IpApi::class.java)

    @Provides
    @Singleton
    fun provideMapServerApi(retrofit: Retrofit): MapServerApi = retrofit.create(MapServerApi::class.java)

    @Provides
    @Singleton
    fun provideRetrofit(controlEndpointProvider: ControlEndpointProvider): Retrofit = Retrofit.Builder()
        .baseUrl(controlEndpointProvider.host + "/")
        .addConverterFactory(
            GsonConverterFactory.create(
                GsonBuilder().registerTypeAdapter(
                    FilterBaseOptionResponse::class.java,
                    FilterBaseOptionDeserializer()
                ).create()
            )
        )
        .client(createOkHttpClient(controlEndpointProvider))
        .build()

    fun createOkHttpClient(controlEndpointProvider: ControlEndpointProvider): OkHttpClient {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64              // total parallel requests (default 64)
            maxRequestsPerHost = 32       // increased this to prevent failing of map filters loading (default is ONLY 5!), but it can still happen
        }

        val builder = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(CONNECTION_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
            .writeTimeout(CONNECTION_TIMEOUT_SEC, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(0, 5, TimeUnit.MINUTES))
            .addInterceptor(ControlServerInterceptor(controlEndpointProvider))
            .addInterceptor(RetryInterceptor(3))

        return setupOkHttpClient(builder).build()
    }
}