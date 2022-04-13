package at.rtr.rmbt.android.map.wrapper

import com.google.android.gms.maps.model.LatLng

data class LatLngW(val latitude: Double, val longitude: Double) {

    fun toGMSLatLng() = LatLng(latitude, longitude)

}