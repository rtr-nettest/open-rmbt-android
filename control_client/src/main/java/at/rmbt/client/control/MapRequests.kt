package at.rmbt.client.control

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class MarkersRequestBody(
    val capabilities: Capabilities? = null,
    @SerializedName("coords")
    val coordinates: Coordinates? = null,
    val filter: Filter? = null,
    val language: String,
    val options: MapOptions? = null
)

@Keep
data class MapOptions(
    /**
     * type of the shown data from this array ["mobile/download", "mobile/upload", "mobile/ping", "mobile/signal", "wlan/download", "wlan/upload",
     * "wlan/ping", "wlan/signal", "browser/download", "browser/upload", "browser/ping", "all/download", "all/upload", "all/ping"]
     */
    @SerializedName("map_options")
    val mapOptions: String
)

@Keep
data class Capabilities(
    val RMBThttp: Boolean? = null,
    val classification: Classification? = null,
    val qos: Qos? = null
)

@Keep
data class Classification(val count: Int)

@Keep
data class Qos(
    @SerializedName("supports_info")
    val isSupported: Boolean
)

@Keep
data class RMBThttp(
    /**
     * True, if the client can handle the RMBThttp protocol
     */
    val RMBThttp: Boolean
)

@Keep
data class Coordinates(
    @SerializedName("lat")
    val latitude: Double,
    @SerializedName("lon")
    val longitude: Double,
    @SerializedName("z")
    val zoom: Int,
    /**
     * size of the area to get results
     */
    val size: Int? = 20
)

@Keep
data class Filter(
    /**
     * id of the operator to show on map, only for mobile
     */
    val operator: String? = null,
    /**
     * number of days to the past to take results ["1", "7", "30", "90", "180", "365", "730", "1460", "2920"]
     */
    val period: String,
    /**
     * id of the provider to show on map, only for wlan
     */
    val provider: String? = null,
    /**
     * statistical method for coloring heatmap ["0.8", "0.5", "0.2"]
     */
    @SerializedName("statistical_method")
    val statisticalMethod: String,
    /**
     * type of the technology for mobile [2, 3, 4, 5, 345, 45, 34] (number according to technology, nothing if all technologies, only for mobile)
     */
    val technology: String? = null
)