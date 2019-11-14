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

import androidx.annotation.Keep
import at.rmbt.client.control.data.ErrorStatus
import com.google.gson.annotations.SerializedName

/**
 * Basic response class
 */
@Keep
open class BaseResponse(val error: List<String>? = null)

/**
 * Response of all settings information used by [ControlServerApi.settingsCheck]
 */
@Keep
data class SettingsResponse(
    val settings: List<SettingItem>
) : BaseResponse()

/**
 * Response of private Ip information used by [ControlServerApi.ipCheck]
 */
@Keep
data class IpInfoResponse(
    @SerializedName("v")
    val ipVersion: Int?,
    @SerializedName("ip")
    val ipAddress: String?
) : BaseResponse()

/**
 * Response with list of news to display for the user
 */
@Keep
data class NewsResponse(
    val news: List<NewsItem>?
) : BaseResponse()

/**
 * Response with configuration for executing basic measurement test
 */
@Keep
data class TestRequestResponse(
    @SerializedName("client_remote_ip")
    val clientRemoteIP: String?,
    @SerializedName("test_uuid")
    val testUUID: String?,
    @SerializedName("result_url")
    val resultURL: String?,
    @SerializedName("result_qos_url")
    val resultQoSURL: String?,
    @SerializedName("test_duration")
    val testDuration: Int?,
    @SerializedName("test_server_name")
    val testServerName: String?,
    @SerializedName("test_server_address")
    val testServerAddress: String?,
    @SerializedName("test_wait")
    val testWaitingTimeSeconds: String,
    @SerializedName("test_numthreads")
    val testNumberOfThreads: String?,
    @SerializedName("test_server_port")
    val testServerPort: Int?,
    @SerializedName("open_test_uuid")
    val openTestUUID: String?,
    @SerializedName("test_server_type")
    val testServerType: String?,
    @SerializedName("test_server_encryption")
    val testServerEncryption: Boolean?,
    @SerializedName("test_token")
    val testToken: String?,
    @SerializedName("test_numpings")
    val testNumberOfPings: String?,
    @SerializedName("test_id")
    val testID: Long?,
    @SerializedName("loop_uuid")
    val loopUUID: String?,
    @SerializedName("provider")
    val providerName: String?,
    @SerializedName("error_flags")
    val errorFlags: HashSet<ErrorStatus>?
) : BaseResponse()

/**
 * Response of news used by [ControlServerApi.newsCheck]
 */
@Keep
data class NewsItem(
    val uid: Long?,
    val title: String?,
    val text: String?
)

@Keep
data class SettingItem(
    @SerializedName("terms_and_conditions")
    val termsAndConditions: TermsAndConditionsSettings?,
    val urls: Urls?,
    @SerializedName("qostesttype_desc")
    val qosTestTypeDesc: List<QosTestTypeDesc>?,
    val versions: VersionsSettings?,
    val servers: List<Server>?,
    @SerializedName("server_ws")
    val serversWebsockets: List<Server>?,
    @SerializedName("server_qos")
    val serversQoS: List<Server>?,
    val history: HistoryFilterSettings?,
    val uuid: String?,
    @SerializedName("map_server")
    val mapServerSettings: MapServerSettings?
)

@Keep
data class TermsAndConditionsSettings(
    val version: Long?,
    val url: String?,
    @SerializedName("ndt_url")
    val ndtURL: String?
)

@Keep
data class Urls(
    @SerializedName("url_share")
    val shareUrl: String?,
    @SerializedName("url_ipv4_check")
    val ipv4CheckUrl: String?,
    @SerializedName("url_ipv6_check")
    val ipv6CheckUrl: String?,
    @SerializedName("control_ipv4_only")
    val ipv4OnlyControlServerUrl: String?,
    @SerializedName("control_ipv6_only")
    val ipv6OnlyControlServerUrl: String?,
    @SerializedName("open_data_prefix")
    val openDataPrefixUrl: String?,
    @SerializedName("url_map_server")
    val mapServerUrl: String?,
    @SerializedName("statistics")
    val statisticsUrl: String?
)

@Keep
data class QosTestTypeDesc(
    val name: String?,
    @SerializedName("test_type")
    val testType: String?
)

@Keep
data class VersionsSettings(
    @SerializedName("control_server_version")
    val controlServerVersion: String?
)

@Keep
data class Server(
    val name: String?,
    val uuid: String?
)

@Keep
data class MapServerSettings(
    val port: Int?,
    val host: String?,
    val ssl: Boolean?
)

@Keep
data class HistoryFilterSettings(
    val devices: List<String>?,
    val networks: List<String>?
)