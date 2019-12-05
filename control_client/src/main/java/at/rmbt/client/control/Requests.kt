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

import android.os.Build
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.util.Locale
import kotlin.collections.ArrayList

@Keep
data class NewsRequestBody(
    val language: String = "en",
    val lastNewsUid: Long = 54654656,
    @SerializedName("plattform")
    val platform: String = "Android",
    val softwareVersionCode: String = 30604.toString(),
    val uuid: String
)

@Keep
data class SettingsRequestBody(
    val type: String = "MOBILE",
    val name: String = "RMBT",
    val language: String = "en",
    @SerializedName("plattform")
    val platform: String = "Android",
    @SerializedName("os_version")
    val osVersion: String = Build.VERSION.RELEASE + "(" + Build.VERSION.INCREMENTAL + ")",
    @SerializedName("api_level")
    val apiLevel: String = Build.VERSION.SDK_INT.toString(),
    val device: String = android.os.Build.DEVICE,
    val model: String = android.os.Build.MODEL,
    val product: String = android.os.Build.PRODUCT,
    val timezone: String = "Europe/Bratislava",
    @SerializedName("software_revision")
    val softwareRevision: String = "master_64bc39c-dirty",
    val softwareVersionCode: String = 30604.toString(),
    val softwareVersionName: String = "3.6.4",
    @SerializedName("version_name")
    val versionName: String = "3.6.4",
    @SerializedName("version_code")
    val versionCode: String = 30604.toString(),
    val uuid: String = "",
    @SerializedName("user_server_selection")
    val userServerSelectionEnabled: Boolean = false,
    @SerializedName("terms_and_conditions_accepted_version")
    var tacVersion: Int = 6,
    @SerializedName("terms_and_conditions_accepted")
    var tacAccepted: Boolean = true,
    var capabilities: Capabilities = Capabilities()
)

@Keep
data class IpRequestBody(
    @SerializedName("plattform")
    val platform: String = "Android",
    val os_version: String = "9(V10.0.13.0.PDHMIXM)",
    val api_level: String = "28",
    val device: String = "tissot_sprout",
    val model: String = "Mi A1",
    val product: String = "tissot",
    val language: String = "en",
    val timezone: String = "Europe/Kiev",
    val softwareRevision: String = "master_ddbcfae-dirty",
    val softwareVersionCode: String = 30604.toString(),
    val softwareVersionName: String = "3.6.4",
    val type: String = "MOBILE",
    val location: LocationBody = LocationBody(),
    @SerializedName("last_signal_item")
    val lastSignalItem: LastSignalItem = LastSignalItem(),
    val uuid: String = "e90d3585-f389-4555-addc-2dac438cebd9",
    val capabilities: Capabilities = Capabilities()
)

@Keep
data class TestRequestRequestBody(
    @SerializedName("plattform")
    val platform: String? = "Android",
    val softwareVersionCode: Int?,
    val ndt: Boolean? = false,
    val previousTestStatus: String? = "END",
    val testCounter: Int? = -1,
    val softwareRevision: String?,
    val softwareVersion: String?,
    @SerializedName("user_server_selection")
    val userServerSelection: Boolean?,
    @SerializedName("prefer_server")
    val preferServer: String?,
    @SerializedName("num_threads")
    val numberOfThreads: Int?,
    @SerializedName("protocol_version")
    val protocolVersion: String? = "ipv4",
    val location: LocationBody? = LocationBody(),
    val time: Long? = 1571664344999,
    val timezone: String = "Europe/Bratislava",
    val client: String = "RMBT",
    val version: String = "0.3",
    val type: String = "MOBILE",
    val uuid: String = "c373f294-f332-4f1a-999e-a87a12523f4b",
    val language: String? = "en",
    val capabilities: Capabilities = Capabilities(),
    @SerializedName("loopmode_info")
    val loopModeInfo: LoopModeInfo
)

/**
 * Object used to send Wifi test results
 */
@Keep
data class SendWifiTestResultsRequestBody(
    /**
     * Wifi supplicant state e.g. "COMPLETED"
     */
    @SerializedName("wifi_supplicant_state")
    var wifiSupplicantState: String,
    /**
     * Wifi supplicant state detail e.g. "OBTAINING_IPADDR"
     */
    @SerializedName("wifi_supplicant_state_detail")
    var wifiSupplicantStateDetail: String,
    /**
     * SSID of the wifi network
     */
    @SerializedName("wifi_ssid")
    var wifiSsid: String,
    /**
     * Id of the wifi network
     */
    @SerializedName("wifi_network_id")
    var wifiNetworkId: String,
    /**
     * BSSID of the wifi network
     */
    @SerializedName("wifi_bssid")
    var wifiBssid: String
) : SendTestResultsRequestBody()

@Keep
data class SendMobileTestResultsRequestBody(
    /**
     * mcc-mnc of the operator network, mobile networks only, e.g. "231-06"
     */
    @SerializedName("telephony_network_operator")
    val telephonyNetworkOperator: String,
    /**
     * true if the network is roaming, mobile networks only
     */
    @SerializedName("telephony_network_is_roaming")
    val telephonyNetworkIsRoaming: Boolean,
    /**
     * country code for network, mobile networks only e.g. "en"
     */
    @SerializedName("telephony_network_country")
    val telephonyNetworkCountry: String,
    /**
     * name of the network operator, mobile networks only, e.g. "O2 - SK"
     */
    @SerializedName("telephony_network_operator_name")
    val telephonyNetworkOperatorName: String,
    /**
     * name of the sim operator, mobile networks only, e.g. "O2 - SK"
     */
    @SerializedName("telephony_network_sim_operator_name")
    val telephonyNetworkSimOperatorName: String,
    /**
     * mcc-mnc of the sim operator, mobile networks only e.g."231-06"
     */
    @SerializedName("telephony_network_sim_operator")
    val telephonyNetworkSimOperator: String,
    /**
     * phone type, mobile networks only e.g. "1"
     */
    @SerializedName("telephony_phone_type")
    val telephonyPhoneType: String,
    /**
     * data state, mobile networks only e.g. "2"
     */
    @SerializedName("telephony_data_state")
    val telephonyDataState: String,
    /**
     * name of the access point, mobile networks only e.g. "o2internet"
     */
    @SerializedName("telephony_apn")
    val telephonyApn: String,
    /**
     * country code of the sim card issuer, mobile networks only, e.g. "sk"
     */
    @SerializedName("telephony_network_sim_country")
    val telephonyNetworkSimCountry: String
) : SendTestResultsRequestBody()

@Keep
abstract class SendTestResultsRequestBody(
    /**
     * Platform constant ("Android" for android client)
     */
    @SerializedName("plattform")
    val platform: String = "Android",
    /**
     * Client uuid
     */
    val clientUUID: String = "c373f294-f332-4f1a-999e-a87a12523f4b",
    /**
     * Client uuid (backward compatibility?)
     */
    val uuid: String = "c373f294-f332-4f1a-999e-a87a12523f4b",
    /**
     * Type of the client {"RMBT", "RMBTws", "HW-PROBE"}, for android devices it is "RMBT"
     */
    @SerializedName("client_name")
    val clientName: String = "RMBT",
    /**
     * Version name of the client version
     */
    @SerializedName("client_version")
    val clientVersion: String = BuildConfig.VERSION_NAME,
    /**
     * Language code of the current locale (ISO 639)
     */
    @SerializedName("client_language")
    val clientLanguage: String = Locale.getDefault().language,
    /**
     * Time of the test in millis
     */
    @SerializedName("time")
    val timeMillis: Long = 0,
    /**
     * Test token generated by control server
     */
    @SerializedName("test_token")
    val testToken: String = "",
    /**
     * Port of the test server the test was performed on
     */
    @SerializedName("test_port_remote")
    val testPortRemote: Int = 0,
    /**
     * Bytes downloaded during download phase
     */
    @SerializedName("test_bytes_download")
    val bytesDownloadedDuringDownTest: Long = 0,
    /**
     * Bytes uploaded during upload phase
     */
    @SerializedName("test_bytes_upload")
    val bytesUploadedDuringUpTest: Long = 0,
    /**
     * Bytes uploaded during the whole test
     */
    @SerializedName("test_total_bytes_download")
    val totalBytesDownloadedDuringTest: Long = 0,
    /**
     * Bytes uploaded during the whole test
     */
    @SerializedName("test_total_bytes_upload")
    val totalBytesUploadedDuringTest: Long = 0,
    /**
     * Test encryption type string (e.g. "TLSv1.2 (TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256)")
     */
    @SerializedName("test_encryption")
    val testEncryptionType: String = "",
    /**
     * Client public ip address, sent by control server
     */
    @SerializedName("test_ip_local")
    val clientPublicIp: String = "",
    /**
     * Server public ip address, sent by control server
     */
    @SerializedName("test_ip_server")
    val serverPublicIp: String = "",
    /**
     * Duration of the download phase in nanoseconds
     */
    @SerializedName("test_nsec_download")
    val downloadPhaseDurationNanos: Long = 0,
    /**
     * Duration of the upload phase in nanoseconds
     */
    @SerializedName("test_nsec_upload")
    val uploadPhaseDurationNanos: Long = 0,
    /**
     * Number of threads used during the measurement
     */
    @SerializedName("test_num_threads")
    val numberOfThreads: Int = 0,
    /**
     * Download speed in kbs
     */
    @SerializedName("test_speed_download")
    val downloadSpeedKbs: Long = 0,
    /**
     * Upload speed in kbs
     */
    @SerializedName("test_speed_upload")
    val uploadSpeedKbs: Long = 0,
    /**
     * Shortest ping in nanos
     */
    @SerializedName("test_ping_shortest")
    val pingShortestNanos: Long = 0,
    /**
     * Bytes downloaded on the interface during test
     */
    @SerializedName("test_if_bytes_download")
    val interfaceBytesDownload: Long = 0,
    /**
     * Bytes uploaded on the interface during test
     */
    @SerializedName("test_if_bytes_upload")
    val interfaceBytesUpload: Long = 0,
    /**
     * Bytes downloaded on the interface during download phase
     */
    @SerializedName("testdl_if_bytes_download")
    val bytesDownloadedAtDownloadPhase: Long = 0,
    /**
     * Bytes uploaded on the interface during download phase
     */
    @SerializedName("testdl_if_bytes_upload")
    val bytesUploadedAtDownloadPhase: Long = 0,
    /**
     * Bytes downloaded on the interface during download phase
     */
    @SerializedName("testul_if_bytes_download")
    val bytesDownloadedAtUploadPhase: Long = 0,
    /**
     * Bytes uploaded on the interface during download phase
     */
    @SerializedName("testul_if_bytes_upload")
    val bytesUploadedAtUploadPhase: Long = 0,
    /**
     * Relative start time of download phase in nanos
     */
    @SerializedName("time_dl_ns")
    val timeDownloadPhaseStartNanos: Long = 0,
    /**
     * Relative start time of download phase in nanos
     */
    @SerializedName("time_ul_ns")
    val timeUploadPhaseStartNanos: Long = 0,
    val product: String = Build.PRODUCT,
    @SerializedName("os_version")
    val osVersion: String = Build.VERSION.RELEASE + "(" + Build.VERSION.INCREMENTAL + ")",
    @SerializedName("api_level")
    val apiLevel: String = Build.VERSION.SDK_INT.toString(),
    val device: String = Build.DEVICE,
    val model: String = Build.MODEL,
    @SerializedName("client_software_version")
    val clientSoftwareVersion: String = BuildConfig.VERSION_NAME,
    /**
     * Server id for the network TODO:(shared/Helperfunctions line 156 - conversion from number to name)
     */
    @SerializedName("network_type")
    val networkType: Long = 0,
    val geoLocations: List<ResultLocationBody>? = null,
    val capabilities: Capabilities = Capabilities(),
    val pings: List<ResultPing>? = null,
    val radioInfo: List<ResultRadioInfo> = ArrayList(),
    @SerializedName("speed_detail")
    val speedDetail: List<ResultSpeedItem> = ArrayList(),
    val cellLocations: List<ResultCellLocation> = ArrayList(),
    @SerializedName("android_permission_status")
    val androidPermissionStatus: List<PermissionStatus> = ArrayList()
)

@Keep
data class LoopModeInfo(
    val uid: Long?,
    @SerializedName("test_uuid")
    val testUUID: String?,
    @SerializedName("test_uuid")
    val clientUUID: String?,
    @SerializedName("max_delay")
    val maxDelaySec: Int?,
    @SerializedName("max_movement")
    val maxMovementMeters: Int?,
    @SerializedName("max_tests")
    val maxTests: Int?,
    @SerializedName("test_counter")
    val testCounter: Int?,
    @SerializedName("loop_uuid")
    val loopUUID: String?
)

@Keep
data class LocationBody(
    @SerializedName("lat")
    val latitude: Double = 50.00329588,
    @SerializedName("long")
    val longitude: Double = 36.24086389,
    val provider: String = "gps",
    val speed: Int = 0,
    val altitude: Double = 176.9620361328125,
    val time: Long = 1570452268000,
    val age: Long = 173134,
    val accuracy: Double = 51.45600128173828,
    @SerializedName("mock_location")
    val mockLocation: Boolean = false,
    val satellites: Int = 5
)

@Keep
data class ResultPing(
    /**
     * ping value in nanos on client side, used to get shortest ping
     */
    @SerializedName("value")
    val differenceClient: Long,
    /**
     * ping value in nanos on server side, used to get median ping
     */
    @SerializedName("value_server")
    val differenceServer: Long,
    /**
     * Relative time from the start of the test in nanos
     */
    @SerializedName("value_ns")
    val timeNanos: Long
)

@Keep
data class ResultRadioInfo(
    /**
     * Cell infos tracked during measurement
     */
    val cells: Array<ResultCellInfo>?,
    /**
     * Signal values tracked during measurement
     */
    val signals: Array<ResultSignal>?
)

@Keep
data class ResultCellInfo(
    /**
     * True if cell is active (connected one)
     */
    val active: Boolean = false,
    /**
     * Area code from mobile cells (Mobile cells only)
     */
    @SerializedName("area_code")
    val areaCode: Long? = null,
    /**
     * Generated uuid for the current cell
     */
    val uuid: String,
    /**
     * Channel number of the cell
     */
    @SerializedName("channel_number")
    val channelNumber: Int,
    /**
     * Id of the location, mobile only
     */
    @SerializedName("location_id")
    val locationId: Int,
    /**
     * Code of the country of the operator, mobile only
     */
    val mcc: Int?,
    /**
     * code of the operator, mobile only
     */
    val mnc: Int?,
    /**
     * scrambling code, mobile only
     */
    @SerializedName("primary_scrambling_code")
    val primaryScramblingCode: Int?,
    /**
     * 2G, 3G, 4G, 5G, WLAN
     */
    val technology: String,
    /**
     * true if it is connected cell, same as active ???
     */
    val registered: Boolean = false
)

@Keep
data class ResultSignal(
    /**
     * Generated uuid of the cell
     */
    @SerializedName("cell_uuid")
    val cellUuid: String,
    /**
     * Server id for network type
     */
    @SerializedName("network_type_id")
    val networkTypeId: Int,
    /**
     * Only for non 4G signal types
     */
    val signal: Int?,
    /**
     * Error rate for non 4G and WIFI signal
     */
    @SerializedName("bit_error_rate")
    val bitErrorRate: Int?,
    /**
     * Declared wifi speed, only WIFI
     */
    @SerializedName("wifi_link_speed")
    val wifiLinkSpeed: Int?,
    /**
     * Only for 4G/LTE signal strength
     */
    @SerializedName("lte_rsrp")
    val lteRsrp: Int?,
    /**
     * Only for 4G/LTE signal quality
     */
    @SerializedName("lte_rsrq")
    val lteRsrq: Int?,
    /**
     * Only for 4G/LTE
     */
    @SerializedName("lte_rssnr")
    val lteRssnr: Int?,
    /**
     * timing advance
     */
    @SerializedName("timing_advance")
    val timingAdvance: Int,
    /**
     * Relative time in nanos from the start of the test
     */
    @SerializedName("time_ns")
    val timeNanos: Long,
    /**
     * relative timestamp from the start of the test, but time of the last update of the cells (the last updated cells do not have this field filled)
     */
    @SerializedName("time_ns_last")
    val timeLastNanos: Int
)

@Keep
data class ResultSpeedItem(
    /**
     * possible values ["upload", "download"]
     */
    val direction: String,
    /**
     * Thread number of the test which value came from
     */
    val thread: Int,
    /**
     * Time from the test start in nanos
     */
    @SerializedName("time")
    val timeNanos: Long,
    /**
     * Actually transferred bytes by the thread
     */
    val bytes: Long
)

@Keep
data class ResultLocationBody(
    @SerializedName("geo_lat")
    val latitude: Double = 0.0,
    @SerializedName("geo_long")
    val longitude: Double = 0.0,
    val provider: String = "",
    val speed: Int = 0,
    val altitude: Double = 0.0,
    /**
     * Timestamp of the information in millis
     */
    @SerializedName("tstamp")
    val timeMillis: Long = 0,
    /**
     * Relative time from the start of the test
     */
    @SerializedName("time_ns")
    val timeNanos: Long = 0,
    val age: Long = 0,
    val accuracy: Double = 0.0,
    val bearing: Double = 0.0,
    @SerializedName("mock_location")
    val mockLocation: Boolean = false,
    val satellites: Int = 0
)

@Keep
data class ResultCellLocation(
    /**
     * timestamp of the information
     */
    @SerializedName("time")
    val timeMillis: Long,
    /**
     * timestamp of the information
     */
    @SerializedName("time_ns")
    val timeNanos: Long,
    /**
     * id of the location
     */
    @SerializedName("location_id")
    val locationId: Int,
    @SerializedName("area_code")
    val areaCode: Long,
    /**
     * scrambling code, -1 if not available
     */
    @SerializedName("primary_scrambling_code")
    val primaryScramblingCode: Int
)

@Keep
data class PermissionStatus(
    /**
     * Name of the permission
     */
    val permission: String,
    /**
     * true if the permission was granted
     */
    val status: Boolean
)

@Keep
data class LastSignalItem(
    val time: Long = 1570452441549,
    @SerializedName("network_type_id")
    val networkTypeId: Int = 99,
    @SerializedName("wifi_link_speed")
    val wifiLinkSpeed: Int = 72,
    @SerializedName("wifi_rssi")
    val wifiRssi: Int = -44
)

@Keep
data class Capabilities(
    val classification: Classification = Classification(),
    val qos: QOS = QOS(),
    val RMBThttp: Boolean = false
)

@Keep
data class Classification(val count: Int = 4)

@Keep
data class QOS(
    @SerializedName("supports_info")
    val supportsInfo: Boolean = false
)