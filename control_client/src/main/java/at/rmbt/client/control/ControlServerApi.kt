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

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Api interface of Control Server
 */
interface ControlServerApi {

    /**
     * Request to get these information:
     *  - settings of servers (ports, urls)
     *  - settings of history filter
     *  - links to newest terms and conditions available on the web
     *  - registers client if uuid is not sent
     *  - list of available test servers
     *  - list of available set of qos tests
     */
    @POST
    fun settingsCheck(@Url url: String, @Body body: SettingsRequestBody): Call<SettingsResponse>

    /**
     * Request to get news to show, when app starts
     */
    @POST
    fun newsCheck(@Url url: String, @Body body: NewsRequestBody): Call<NewsResponse>

    /**
     * Request to get basic measurement test settings
     */
    @POST
    fun testRequest(@Url url: String, @Body body: TestRequestRequestBody): Call<TestRequestResponse>

    /**
     * Request to send basic measurement test results
     */
    @POST
    fun sendTestResult(@Url url: String, @Body body: TestResultBody): Call<BaseResponse>

    /**
     * Request to send QoS test results
     */
    @POST
    fun sendQoSTestResult(@Url url: String, @Body body: QoSResultBody): Call<BaseResponse>

    @POST
    fun getHistory(@Url url: String, @Body body: HistoryRequestBody): Call<HistoryResponse>

    /**
     * Request to get basic measurement results
     */
    @POST
    fun getTestResult(@Url url: String, @Body body: ServerTestResultBody): Call<ServerTestResultResponse>

    /**
     * Request to get detailed measurement results via opendata
     */
    @GET
    fun getTestResultOpenDetails(@Url url: String): Call<SpeedCurveBodyResponse>

    /**
     * Request to get test result details
     */
    @POST
    fun getTestResultDetail(@Url url: String, @Body body: TestResultDetailBody): Call<TestResultDetailResponse>

    /**
     * Request to get QoS test result details
     */
    @POST
    fun getQosTestResultDetail(@Url url: String, @Body body: QosTestResultDetailBody): Call<QosTestResultDetailResponse>

    /**
     * Request to get sync code for current device
     */
    @POST
    fun getSyncCode(@Url url: String, @Body body: GetSyncCodeBody): Call<GetSyncCodeResponse>

    /**
     * Request to sync two devices
     */
    @POST
    fun syncDevices(@Url url: String, @Body body: SyncDevicesBody): Call<DeviceSyncResponse>

    /**
     * Request to get news
     */
    @POST
    fun getNews(@Url url: String, @Body body: NewsRequestBody): Call<NewsResponse>

    /**
     * Request to obtain signal measurement required data
     */
    @POST
    fun signalRequest(@Url url: String, @Body body: SignalMeasurementRequestBody): Call<SignalMeasurementRequestResponse>

    /**
     * Request to obtain signal measurement required data
     */
    @POST
    fun signalResult(@Url url: String, @Body body: SignalMeasurementChunkBody): Call<SignalMeasurementChunkResultResponse>
}