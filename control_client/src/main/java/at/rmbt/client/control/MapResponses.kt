package at.rmbt.client.control

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class MarkersResponse(val measurements: List<MarkerMeasurementsResponse>) : BaseResponse()

@Keep
data class MarkerMeasurementsResponse(
    @SerializedName("lat")
    val latitude: Double,
    @SerializedName("lon")
    val longitude: Double,
    val measurement: List<MarkerMeasurementItem>,
    @SerializedName("measurement_result")
    val measurementResult: MeasurementResultItem,
    @SerializedName("net")
    val networkItems: List<MarkerNetworkItem>,
    @SerializedName("network_info")
    val networkInfo: TestResultNetworkInfoItem,
    @SerializedName("open_test_uuid")
    val openTestUUID: String,
    val time: Long,
    @SerializedName("time_string")
    val timeString: String?
)

@Keep
data class MeasurementResultItem(
    @SerializedName("download_classification")
    val downloadClassification: Int,
    @SerializedName("download_kbit")
    val downloadKbit: Long?,
    @SerializedName("lte_rsrp")
    val lteRsrp: Int?,
    @SerializedName("ping_classification")
    val pingClassification: Int,
    @SerializedName("ping_ms")
    val pingMs: Double?,
    @SerializedName("signal_classification")
    val signalClassification: Int,
    @SerializedName("signal_strength")
    val signalStrength: Int?,
    @SerializedName("upload_classification")
    val uploadClassification: Int,
    @SerializedName("upload_kbit")
    val uploadKbit: Long?
)

@Keep
data class MarkerMeasurementItem(val classification: Int, val title: String, val value: String)

@Keep
data class MarkerNetworkItem(val title: String, val value: String)

@Keep
data class TestResultNetworkInfoItem(
    @SerializedName("network_type_label")
    val networkTypeLabel: String,
    @SerializedName("provider_name")
    val providerName: String?,
    @SerializedName("roaming_type_label")
    val roamingTypeLabel: String?,
    @SerializedName("wifi_ssid")
    val wifiSSID: String
)