package at.rmbt.client.control

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
data class MarkersResponse(val measurements: List<MarkerMeasurementsResponse>?) : BaseResponse()

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

@Keep
data class MapFilterResponse(@SerializedName("map_filters") val filters: List<MapFilterItemV2>?) : BaseResponse()

@Keep
data class MapFilterObjectResponse(val mapFilters: MapFiltersResponse, val mapTypes: List<MapTypeResponse>)

@Keep
data class MapTypeResponse(val options: List<MapTypeOptionsResponse>, val title: String)

@Keep
data class NationalTableResponse(
    @SerializedName("statsByProvider")
    val providerStats: List<ProviderStatistics>?,

    @SerializedName("averageUpload")
    val averageUploadKbps: String?,

    @SerializedName("averageDownload")
    val averageDownloadKbps: String?,

    @SerializedName("averageLatency")
    val averageLatencyMillis: String?,

    @SerializedName("allMeasurements")
    val allMeasurementsCount: String?
) : BaseResponse()

@Keep
data class ProviderStatistics(
    val providerName: String?,

    @SerializedName("upload")
    val averageUploadKbps: String?,

    @SerializedName("download")
    val averageDownloadKbps: String?,

    @SerializedName("latency")
    val averageLatencyMillis: String?,

    @SerializedName("measurements")
    val allMeasurementsCount: String?
)

@Keep
@Parcelize
data class MapTypeOptionsResponse(
    @SerializedName("map_options")
    val mapOptions: String,
    val summary: String,
    val title: String
) : Parcelable

@Keep
data class MapFiltersResponse(
    val all: List<TypeOptionsResponse>,
    val browser: List<TypeOptionsResponse>,
    val mobile: List<TypeOptionsResponse>,
    val wifi: List<TypeOptionsResponse>
)

@Keep
@Parcelize
data class FilterTechnologyOptionResponse(val technology: String) : FilterBaseOptionResponse()

@Keep
@Parcelize
data class FilterStatisticOptionResponse(@SerializedName("statistical_method") val statisticalMethod: Double) : FilterBaseOptionResponse()

@Keep
@Parcelize
data class FilterPeriodOptionResponse(val period: Int) : FilterBaseOptionResponse()

@Keep
@Parcelize
data class FilterProviderOptionResponse(val provider: String) : FilterBaseOptionResponse()

@Keep
@Parcelize
data class FilterOperatorOptionResponse(val operator: String) : FilterBaseOptionResponse()

@Keep
@Parcelize
open class FilterBaseOptionResponse(var default: Boolean = false, var summary: String = "", var title: String = "") : Parcelable

@Keep
data class TypeOptionsResponse(val options: List<FilterBaseOptionResponse>, val title: String)

@Keep
data class MapFiltersResponseV2(
    val mapFilters: List<MapFilterItemV2>,
)

@Keep
// MapFilterTypeClass.MAP_FILTER_STATISTIC.stringValue is default value because statistic has no "icon" field filled in API
data class MapFilterItemV2(
    val options: List<FilterBaseOptionResponseV2>,
    val title: String,
    val icon: String? = MapFilterTypeClass.MAP_FILTER_STATISTIC.serverValue,
    @SerializedName("depends_on")
    val dependsOn: MapFilterDependsOnV2?,
    val functions: List<FilterBaseFunctionResponseV2>?,
    val default: Boolean = false)

@Keep
@Parcelize
open class FilterBaseOptionResponseV2(
    var default: Boolean = false,
    var summary: String? = "",
    var title: String = "",
    var params: FilterBasePeriodParamsV2?,
    var functions: List<FilterBaseFunctionResponseV2>?,
    val options: List<FilterBaseOptionResponseV2>?
) : Parcelable

@Keep
@Parcelize
open class FilterBasePeriodParamsV2(
    val period: Int?, // for @MapFilterTypeClass.MAP_FILTER_PERIOD
    @SerializedName("statistical_method")
    val statisticalMethod: Double?, // for @MapFilterTypeClass.MAP_FILTER_STATISTIC
    val provider: String?, // provider number or ""
    val operator: String?, // operator number or ""
    val technology: String?,
    @SerializedName("map_type_is_mobile") // ???
    val mapTypeIsMobile: Boolean?,
    @SerializedName("map_options")
    val mapOptions: String?,
    @SerializedName("overlay_type")
    val overlayType: String?,
) : Parcelable

@Keep
@Parcelize
open class FilterBaseFunctionResponseV2(
    @SerializedName("func_name")
    val functionName: String,
    @SerializedName("func_params")
    val functionParams: FilterBaseFunctionParamV2
) : Parcelable

@Keep
@Parcelize
open class FilterBaseFunctionParamV2(
    val type: String,
    val path: String?,
    @SerializedName("z_index")
    val zIndex: Int?,
    @SerializedName("tile_size")
    val tileSize: Int?,
    val key: String?
) : Parcelable

@Keep
@Parcelize
open class MapFilterDependsOnV2(
    @SerializedName("map_type_is_mobile")
    val mapTypeIsMobile: Boolean
) : Parcelable