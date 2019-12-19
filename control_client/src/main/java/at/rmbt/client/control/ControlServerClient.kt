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

    fun getHistory(body: HistoryRequestBody): Maybe<HistoryResponse> {
        return api.getHistory(endpointProvider.getHistoryUrl, body).exec()
    }
}