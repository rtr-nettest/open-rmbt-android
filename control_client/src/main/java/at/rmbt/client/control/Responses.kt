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
import com.google.gson.JsonObject
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

@Keep
data class ServerTestResultResponse(
    @SerializedName("testresult")
    val resultItem: List<ServerTestResultItem>
) : BaseResponse()

@Keep
data class SpeedCurveBodyResponse(
    // a lot of fields are not important for us, so we will parse only those one we need
    @SerializedName("speed_curve")
    val speedCurve: SpeedCurveResponse
) : BaseResponse()

@Keep
data class SpeedCurveResponse(
    @SerializedName("download")
    val download: List<SpeedGraphItemResponse>,

    @SerializedName("upload")
    val upload: List<SpeedGraphItemResponse>,

    @SerializedName("ping")
    val ping: List<PingGraphItemResponse>,

    @SerializedName("signal")
    val signal: List<SignalGraphItemResponse>
)

@Keep
data class SpeedGraphItemResponse(

    /**
     * Total bytes transferred until the timestamp
     */
    @SerializedName("bytes_total")
    val bytes: Long,

    /**
     * Relative time in milliseconds form the start of the test
     */
    @SerializedName("time_elapsed")
    val timeMillis: Long
)

@Keep
data class PingGraphItemResponse(
    // cell infos are not parsed (it is not relevant for the application)
    /**
     * Ping value in milliseconds
     */
    @SerializedName("ping_ms")
    val durationMillis: Double,

    /**
     * Relative time in milliseconds form the start of the test
     */
    @SerializedName("time_elapsed")
    val timeMillis: Long
)

@Keep
data class SignalGraphItemResponse(

    /**
     * Type of the network, e.g. GSM, EDGE, UMTS, HSPA, LTE, LAN, WLAN…
     */
    @SerializedName("network_type")
    val networkType: String,

    /**
     * Signal strength (RSSI) in dBm.
     */
    @SerializedName("signal_strength")
    val signalStrength: Int?,

    /**
     * LTE signal strength in dBm.
     */
    @SerializedName("lte_rsrp")
    val lteRsrp: Int?,

    /**
     * LTE signal quality in decibels.
     */
    @SerializedName("lte_rsrq")
    val lteRsrq: Int?,

    /**
     * Technology category of the network, e.g. “3G”, “4G”, “5G”, “WLAN”.
     */
    @SerializedName("cat_technology")
    val technologyCategory: String,

    /**
     * Timing advance value for LTE, as a value in range of 0..1282. Refer to 3GPP 36.213 Sec 4.2.3
     */
    @SerializedName("timing_advance")
    val timingAdvance: Int?,

    /**
     * Relative time in milliseconds form the start of the test
     */
    @SerializedName("time_elapsed")
    val timeMillis: Long
)

@Keep
data class ServerTestResultItem(

    /**
     * open uuid of the client used for identify user in opendata
     */
    @SerializedName("open_uuid")
    val clientOpenUUID: String,

    /**
     * open uuid of the test used for identify test in opendata and request opendata result details (necessary for graph values)
     */
    @SerializedName("open_test_uuid")
    val testOpenUUID: String,

    /**
     * Subject of the share message used to share result via other app
     */
    @SerializedName("share_subject")
    val shareSubject: String,

    /**
     * Formatted text of the share message used to share result via other app
     */
    @SerializedName("share_text")
    val shareText: String,

    /**
     * Human readable format of the timezone e.g. "Europe/Bratislava"
     */
    @SerializedName("timezone")
    val timezone: String,

    /**
     * Human readable format of geolocation (only coordinates formatted)
     */
    @SerializedName("location")
    val locationText: String?,

    /**
     * longitude coordinate of geolocation
     */
    @SerializedName("geo_long")
    val longitude: Double?,

    /**
     * latitude coordinate of geolocation
     */
    @SerializedName("geo_lat")
    val latitude: Double?,

    /**
     * Time, when the test was performed in milliseconds
     */
    @SerializedName("time")
    val timestamp: Long,

    /**
     * Formatted time, when the test was performed (ready to be displayed)
     */
    @SerializedName("time_string")
    val timeText: String,

    /**
     * Object holding all basic measured values (ping, download, upload, signal)
     */
    @SerializedName("measurement_result")
    val measurementItem: MeasurementItem,

    /**
     * Object holding all basic network information
     */
    @SerializedName("network_info")
    val networkItem: NetworkItem,

    /**
     * Server type of the network
     */
    @SerializedName("network_type")
    val networkType: Int,

    /**
     * List with all QoE classifications
     */
    @SerializedName("qoe_classification")
    val qoeClassifications: List<QoEClassification>
)

@Keep
data class QoEClassification(

    /**
     * Type of QoE that is classified by this attribute
     *
     * - streaming_audio_streaming
     * - video_sd
     * - video_hd
     * - video_uhd
     * - gaming
     * - gaming_cloud
     * - gaming_streaming
     * - gaming_download
     * - voip
     * - video_telephony
     * - video_conferencing
     * - messaging
     * - web
     * - cloud
     */
    val category: String,

    /**
     * Classification value for assigning traffic-light-color
     *  0 = not available (greyed out)
     *  1 = red
     *  2 = yellow
     *  3 = green
     *  4 = dark green
     */
    val classification: Int,

    /**
     * Quality of Experience value, [0.0, 1.0] whereas
     * 0.0 = worst possible value
     * 1.0 = best possible value
     */
    val quality: Float
)

/**
 * Basic measurement result from server
 */
@Keep
data class MeasurementItem(

    /**
     * Classification value for assigning traffic-light-color
     *  0 = not available (greyed out)
     *  1 = red
     *  2 = yellow
     *  3 = green
     *  4 = dark green
     */
    @SerializedName("upload_classification")
    val uploadClass: Int,

    /**
     * Upload speed in kbit per second
     */
    @SerializedName("upload_kbit")
    val uploadSpeedKbs: Long,

    /**
     * Classification value for assigning traffic-light-color
     *  0 = not available (greyed out)
     *  1 = red
     *  2 = yellow
     *  3 = green
     *  4 = dark green
     */
    @SerializedName("download_classification")
    val downloadClass: Int,

    /**
     * Download speed in kbit per second
     */
    @SerializedName("download_kbit")
    val downloadSpeedKbs: Long,

    /**
     * Signal value in dBm for 4G measurement connections
     */
    @SerializedName("lte_rsrp")
    val lte_rsrp: Int?,

    /**
     * Signal value in dBm for WIFI, 3G, 2G measurement connections
     */
    @SerializedName("signal_strength")
    val signalStrength: Int?,

    /**
     * Classification value for assigning traffic-light-color
     *  0 = not available (greyed out)
     *  1 = red
     *  2 = yellow
     *  3 = green
     *  4 = dark green
     */
    @SerializedName("signal_classification")
    val signalClass: Int,

    /**
     * Median ping (round-trip time) in milliseconds, measured on the server side. In previous versions (before June 3rd 2015) this was the minimum ping measured on the client side.
     */
    @SerializedName("ping_ms")
    val pingMillis: Double,

    /**
     * Classification value for assigning traffic-light-color
     *  0 = not available (greyed out)
     *  1 = red
     *  2 = yellow
     *  3 = green
     *  4 = dark green
     */
    @SerializedName("ping_classification")
    val pingClass: Int
)

@Keep
data class NetworkItem(

    @SerializedName("network_type_label")
    val networkTypeString: String,

    @SerializedName("wifi_ssid")
    val wifiNetworkSSID: String?,

    @SerializedName("provider_name")
    val providerName: String?
)

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
    val version: Int?,
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

@Keep
data class HistoryResponse(
    val history: List<HistoryItemResponse>
) : BaseResponse()

@Keep
data class HistoryItemResponse(
    val model: String,
    @SerializedName("network_type")
    val networkType: String,
    val ping: String,
    @SerializedName("ping_classification")
    val pingClassification: Int,
    @SerializedName("ping_shortest")
    val pingShortest: String,
    @SerializedName("ping_shortest_classification")
    val pingShortestClassification: Int,
    @SerializedName("speed_download")
    val speedDownload: String,
    @SerializedName("speed_download_classification")
    val speedDownloadClassification: Int,
    @SerializedName("speed_upload")
    val speedUpload: String,
    @SerializedName("speed_upload_classification")
    val speedUploadClassification: Int,
    @SerializedName("test_uuid")
    val testUUID: String,
    val time: Long,
    @SerializedName("time_string")
    val timeString: String,
    val timezone: String
)

@Keep
data class TestResultDetailItem(
    @SerializedName("open_test_uuid")
    val openTestUUID: String?,
    @SerializedName("open_uuid")
    val openUuid: String?,
    val time: Long?,
    val timezone: String?,
    val title: String,
    val value: String
)

@Keep
data class TestResultDetailResponse(
    @SerializedName("testresultdetail")
    val details: List<TestResultDetailItem>
) : BaseResponse()

@Keep
data class QosTestResultDetailResponse(

    /**
     * Results with technical details
     */
    @SerializedName("testresultdetail")
    val qosResultDetails: List<QosTestResult>,

    /**
     * Described results to be human readable with localized description
     */
    @SerializedName("testresultdetail_desc")
    val qosResultDetailsDesc: List<QosTestDescription>,

    /**
     * Localized description of each test category
     */
    @SerializedName("testresultdetail_testdesc")
    val qosResultDetailsTestDesc: List<QosTestCategoryDescription>,

    /**
     * Times of the qos results evaluation
     */
    @SerializedName("eval_times")
    val evaluationTimes: EvalTimes
) : BaseResponse()

@Keep
data class QosTestResult(

    /**
     * UID of test for which is this description, this is used to use across all qos details
     */
    @SerializedName("uid")
    val qosTestUid: Long,

    /**
     * ???
     */
    @SerializedName("nn_test_uid")
    val qosNnTestUid: Long,

    /**
     * ???
     */
    @SerializedName("qos_test_uid")
    val qosTestUidConf: Long,

    /**
     * ???
     */
    @SerializedName("test_uid")
    val testUid: Long,

    /**
     * Array with info about which keys are delivering result information in @test_result_key_map
     */
    @SerializedName("test_result_keys")
    val testResultKeys: List<String>,

    @SerializedName("test_result_key_map")
    val testResultMap: List<String>,

    @SerializedName("test_summary")
    val testSummary: List<String>,

    @SerializedName("success_count")
    val successCount: Int,

    @SerializedName("failure_count")
    val failureCount: Int,

    @SerializedName("test_type")
    val testType: String,

    @SerializedName("test_desc")
    val testDescription: String,

    @SerializedName("result")
    val result: JsonObject
)

@Keep
data class QosTestDescription(

    /**
     * UIDs of tests for which is this description
     */
    @SerializedName("uid")
    val qosTestUids: List<Long>,

    /**
     * Test category
     */
    @SerializedName("test")
    val testCategory: String,

    /**
     * Key to be evaluated in the @qosResultDetails
     */
    @SerializedName("key")
    val keyResult: String,

    /**
     * Status associated with the test
     */
    @SerializedName("status")
    val resultStatus: String,

    /**
     * Localized description associated with the test
     */
    @SerializedName("desc")
    val resultDescription: String
)

@Keep
data class QosTestCategoryDescription(

    /**
     * type constant of the qos test category
     */
    @SerializedName("test_type")
    val testType: String,

    /**
     * Localized name of the qos test type category
     */
    @SerializedName("name")
    val nameLocalized: String,

    /**
     * Localized description and explanation of the test
     */
    @SerializedName("desc")
    val descLocalized: String
)

@Keep
data class EvalTimes(
    /**
     * time to evaluate results on the server side in millis without loading results
     */
    @SerializedName("eval")
    val evalTimeMillis: String?,

    /**
     * time to evaluate results on the server side in millis with loading results from DB
     */
    @SerializedName("full")
    val evalTimeWithLoadMillis: String?
)