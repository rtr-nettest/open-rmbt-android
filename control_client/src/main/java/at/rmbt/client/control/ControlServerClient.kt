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

import at.rmbt.util.Maybe
import javax.inject.Inject

class ControlServerClient @Inject constructor(private val endpointProvider: ControlEndpointProvider, private val api: ControlServerApi) {

    fun getSettings(body: SettingsRequestBody): Maybe<SettingsResponse> {
        return api.settingsCheck(endpointProvider.checkSettingsUrl, body).exec()
    }

    fun getTestSettings(body: TestRequestRequestBody): Maybe<TestRequestResponse> {
        return api.testRequest(endpointProvider.testRequestUrl, body).exec()
    }

    fun sendTestResults(body: TestResultBody): Maybe<BaseResponse> {
        return api.sendTestResult(endpointProvider.sendTestResultsUrl, body).exec()
    }

    fun sendQoSTestResults(body: QoSResultBody): Maybe<BaseResponse> {
        return api.sendQoSTestResult(endpointProvider.sendQoSTestResultsUrl, body).exec()
    }

    fun sendQoSTestResultsONT(body: QoSResultBody): Maybe<QosResultResponse> {
        return api.sendQoSTestResultONT(endpointProvider.sendQoSTestResultsUrl, body).exec()
    }

    fun getHistory(body: HistoryRequestBody): Maybe<HistoryResponse> {
        return api.getHistory(endpointProvider.getHistoryUrl, body).exec()
    }

    fun getHistoryONT(
        body: HistoryONTRequestBody,
        size: Long,
        page: Long
    ): Maybe<HistoryONTResponse> {
        return api.getHistoryONT(
            endpointProvider.getHistoryUrl + "?page=$page&size=$size&sort=measurementDate,desc",
            body
        ).exec()
    }

    fun getTestResult(body: ServerTestResultBody): Maybe<ServerTestResultResponse> {
        return api.getTestResult(endpointProvider.getTestResultsBasicUrl, body).exec()
    }

    fun getDetailedTestResults(openTestUUID: String): Maybe<SpeedCurveBodyResponse> {
        return api.getTestResultOpenDetails(endpointProvider.getTestResultsOpenDataUrl + "/" + openTestUUID)
            .exec()
    }

    /**
     * For ONT based apps to obtain graph data - basically optimized getDetailedTestResults, because only graphs were used from all that information received
     */
    fun getTestResultGraphs(testUUID: String): Maybe<SpeedCurveBodyResponseONT> {
        return api.getTestResultGraphs(endpointProvider.getTestResultsOpenDataUrl + "/" + testUUID)
            .exec()
    }

    fun getTestResultDetail(body: TestResultDetailBody): Maybe<TestResultDetailResponse> {
        return api.getTestResultDetail(endpointProvider.getTestResultsDetailsUrl, body).exec()
    }

    fun getQosTestResultDetail(body: QosTestResultDetailBody): Maybe<QosTestResultDetailResponse> {
        return api.getQosTestResultDetail(endpointProvider.getQosResultDetailsUrl, body).exec()
    }

    /**
     * Suitable to get all necessary results in ONT based apps
     */
    fun getTestResultDetailONT(testUUID: String): Maybe<ResultDetailONTResponse> {
        return api.getResultDetailONT(endpointProvider.getQosResultDetailsUrl + "/" + testUUID)
            .exec()
    }

    fun getDeviceSyncCode(body: GetSyncCodeBody): Maybe<GetSyncCodeResponse> {
        return api.getSyncCode(endpointProvider.getSyncCodeUrl, body).exec()
    }

    fun syncDevices(body: SyncDevicesBody): Maybe<DeviceSyncResponse> {
        return api.syncDevices(endpointProvider.syncDevicesUrl, body).exec()
    }

    fun getNews(body: NewsRequestBody): Maybe<NewsResponse> {
        return api.getNews(endpointProvider.getNewsUrl, body).exec()
    }

    fun signalRequest(body: SignalMeasurementRequestBody): Maybe<SignalMeasurementRequestResponse> {
        return api.signalRequest(endpointProvider.signalRequestUrl, body).exec()
    }

    fun signalResult(body: SignalMeasurementChunkBody): Maybe<SignalMeasurementChunkResultResponse> {
        return api.signalResult(endpointProvider.signalResultUrl, body).exec()
    }
}