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
import com.google.gson.annotations.SerializedName

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
    val osVersion: String = android.os.Build.VERSION.RELEASE + "(" + android.os.Build.VERSION.INCREMENTAL + ")",
    @SerializedName("api_level")
    val apiLevel: String = android.os.Build.VERSION.SDK_INT.toString(),
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
data class LastSignalItem(
    val time: Long = 1570452441549,
    @SerializedName("network_type_id")
    val networkTypeId: Int = 99,
    @SerializedName("wifiLinkSpeed")
    val wifi_link_speed: Int = 72,
    @SerializedName("wifiRssi")
    val wifi_rssi: Int = -44
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
data class QOS(val supports_info: Boolean = false)