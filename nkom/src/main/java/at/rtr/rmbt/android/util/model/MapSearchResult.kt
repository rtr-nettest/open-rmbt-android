package at.rtr.rmbt.android.util.model

import com.mapbox.mapboxsdk.geometry.LatLngBounds

data class MapSearchResult(
    val title: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val bounds: LatLngBounds
)
