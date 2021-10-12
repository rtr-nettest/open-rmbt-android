package at.rtr.rmbt.android.util.model

import com.google.gson.annotations.SerializedName

data class MapSearchResult(
    @SerializedName("title")
    val title: String,
    @SerializedName("address")
    val address: Address,
    @SerializedName("position")
    val position: Position,
    @SerializedName("mapView")
    val bounds: Bounds?
)

data class Address(
    @SerializedName("label")
    val label: String
)

data class Position(
    @SerializedName("lat")
    val latitude: Double,
    @SerializedName("lng")
    val longitude: Double
)

data class Bounds(
    @SerializedName("north")
    val north: Double,
    @SerializedName("east")
    val east: Double,
    @SerializedName("south")
    val south: Double,
    @SerializedName("west")
    val west: Double
)
