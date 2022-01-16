package at.rmbt.client.control

import android.os.Parcelable
import androidx.annotation.Keep
import at.rmbt.client.control.data.MapFilterType
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
data class MapFilterResponse(@SerializedName("mapfilter") val filter: MapFilterObjectResponse) : BaseResponse()

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
    val mapFilters: List<MFTBaseResponseV2>,
)

@Keep
@Parcelize
// MapFilterTypeClass.MAP_FILTER_STATISTIC.stringValue is default value because statistic has no "icon" field filled in API
open class MFTBaseResponseV2(
    val title: String,
    val icon: String = MapFilterTypeClass.MAP_FILTER_STATISTIC.stringValue,
    @SerializedName("depends_on")
    val default: Boolean = false) : Parcelable

@Keep
@Parcelize
open class FilterBaseOptionResponseV2(
    var default: Boolean = false,
    var summary: String = "",
    var title: String = ""
//    var functions: List<FilterBaseFunctionResponseV2>?,
//    var options: List<FilterBaseOptionResponseV2>?
) : Parcelable

@Keep
@Parcelize
open class MapFilterBaseParamsV2(
) : Parcelable

@Keep
@Parcelize
open class MapFilterStatisticTypeResponseV2(
    var options: List<MFStatisticalOptionsResponseV2>
) : MFTBaseResponseV2(icon = MapFilterTypeClass.MAP_FILTER_STATISTIC.stringValue, title = "")

@Keep
@Parcelize
open class MFStatisticalOptionsResponseV2(
    var params: MapFilterStatisticalParamsV2?,
): FilterBaseOptionResponseV2()

@Keep
@Parcelize
open class MapFilterStatisticalParamsV2(
    @SerializedName("statistical_method")
    val statisticalMethod: Double? // for @MapFilterTypeClass.MAP_FILTER_STATISTIC
) : MapFilterBaseParamsV2()

@Keep
@Parcelize
open class MapFilterPeriodTypeResponseV2(
    var options: List<MFPeriodOptionsResponseV2>
) : MFTBaseResponseV2(icon = MapFilterTypeClass.MAP_FILTER_PERIOD.stringValue, title = "")

@Keep
@Parcelize
open class MFPeriodOptionsResponseV2(
    var params: MapFilterPeriodParamsV2?,
): FilterBaseOptionResponseV2()

@Keep
@Parcelize
open class MapFilterPeriodParamsV2(
    val period: Int
) : MapFilterBaseParamsV2()

@Keep
@Parcelize
open class MapFilterOverlayTypeResponseV2(
    var options: List<MFOverlayOptionsResponseV2>
) : MFTBaseResponseV2(icon = MapFilterTypeClass.MAP_OVERLAY_TYPE.stringValue, title = "")

@Keep
@Parcelize
open class MFOverlayOptionsResponseV2(
    var functions: MapFilterOverlayFunctionsV2
): FilterBaseOptionResponseV2()

@Keep
@Parcelize
open class MapFilterOverlayFunctionsV2(
    @SerializedName("func_name")
    val functionName: String,
    @SerializedName("func_params")
    val functionParams: FilterOverlayFunctionParamsV2
) : MapFilterBaseParamsV2()

@Keep
@Parcelize
open class FilterOverlayFunctionParamsV2(
    val path: String?,
    @SerializedName("z_index")
    val zIndex: Int?,
    @SerializedName("tile_size")
    val tileSize: Int?
): FilterBaseFunctionParamV2(type = "")


@Keep
@Parcelize
open class MapFilterAppearanceTypeResponseV2(
    var options: List<FilterBaseFunctionResponseV2>
) : MFTBaseResponseV2(icon = MapFilterTypeClass.MAP_FILTER_APPEARANCE.stringValue, title = "")

@Keep
@Parcelize
open class MapFilterCarrierTypeResponseV2(
    var options: List<MFCarrierOptionsResponseV2>,
    @SerializedName("depends_on")
    val dependsOn: MapFilterDependsOnV2
) : MFTBaseResponseV2(icon = MapFilterTypeClass.MAP_FILTER_CARRIER.stringValue, title = "")

@Keep
@Parcelize
open class MFCarrierOptionsResponseV2(
    var params: MapFilterCarrierParamsV2
): FilterBaseOptionResponseV2()

@Keep
@Parcelize
open class MapFilterCarrierParamsV2(
    @SerializedName("provider")
    var providerId: String
): MapFilterBaseParamsV2()

@Keep
@Parcelize
open class MapFilterTechnologyTypeResponseV2(
    var options: List<MFTechnologyOptionsResponseV2>,
    @SerializedName("depends_on")
    val dependsOn: MapFilterDependsOnV2
) : MFTBaseResponseV2(icon = MapFilterTypeClass.MAP_FILTER_CARRIER.stringValue, title = "")

@Keep
@Parcelize
open class MFTechnologyOptionsResponseV2(
    var params: MapFilterTechnologyParamsV2
): FilterBaseOptionResponseV2()

@Keep
@Parcelize
open class MapFilterTechnologyParamsV2(
    val technology: String
): MapFilterBaseParamsV2()

@Keep
@Parcelize
open class MapFilterMapTypeResponseV2(
    var options: List<MFMapTypeOptionsResponseV2>,
    @SerializedName("depends_on")
    val functions: List<MFMapTypeFunctionsV2>
) : MFTBaseResponseV2(icon = MapFilterTypeClass.MAP_FILTER_CARRIER.stringValue, title = "")

@Keep
@Parcelize
open class MFMapTypeFunctionsV2(
    var params: MapFilterTechnologyParamsV2
): FilterBaseFunctionResponseV2()

@Keep
@Parcelize
open class MFMapTypeOptionsResponseV2(
    var params: MapFilterTechnologyParamsV2
): FilterBaseOptionResponseV2()

@Keep
@Parcelize
open class MapFilterTechnologyParamsV2(
    val technology: String
): MapFilterBaseParamsV2()


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
    val type: String
//    val provider: String?, // provider number or ""
//    val operator: String?, // operator number or ""
//    val technology: String?,
//    val key: String?
) : Parcelable

@Keep
@Parcelize
open class MapFilterDependsOnV2(
    @SerializedName("map_type_is_mobile")
    val mapTypeIsMobile: Boolean
) : Parcelable