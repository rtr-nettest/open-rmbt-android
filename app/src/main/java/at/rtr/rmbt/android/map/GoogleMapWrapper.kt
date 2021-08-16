package at.rtr.rmbt.android.map

import android.annotation.SuppressLint
import android.content.Context
import at.rmbt.client.control.data.MapStyleType
import at.rtr.rmbt.android.map.wrapper.GMSMarker
import at.rtr.rmbt.android.map.wrapper.GMSOverlayWrapper
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.map.wrapper.MapWrapper
import at.rtr.rmbt.android.map.wrapper.MarkerWrapper
import at.rtr.rmbt.android.map.wrapper.TileOverlayWrapper
import at.rtr.rmbt.android.map.wrapper.TileWrapperProvider
import at.rtr.rmbt.android.util.iconFromVector
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions

class GoogleMapWrapper(private val googleMap: GoogleMap) : MapWrapper {

    override fun moveCamera(latLngW: LatLngW, zoom: Float) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngW.toGMSLatLng(), zoom))
    }

    override fun animateCamera(latLngW: LatLngW) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLngW.toGMSLatLng()))
    }

    override fun animateCamera(latLngW: LatLngW, zoom: Float) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngW.toGMSLatLng(), zoom))
    }

    override fun addTileOverlay(tileWProvider: TileWrapperProvider): TileOverlayWrapper {
        val overlay = googleMap.addTileOverlay(TileOverlayOptions().tileProvider { x, y, zoom ->
            tileWProvider.getTileW(x, y, zoom).toGMSTile()
        })
        return GMSOverlayWrapper(overlay)
    }

    override fun addMarker(
        context: Context,
        latLngW: LatLngW,
        anchorU: Float,
        anchorV: Float,
        iconId: Int
    ): MarkerWrapper {
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(latLngW.toGMSLatLng())
                .anchor(anchorU, anchorV)
                .iconFromVector(context, iconId)
        )
        return GMSMarker(marker)
    }

    @SuppressLint("MissingPermission")
    override fun setMyLocationEnabled(enabled: Boolean) {
        googleMap.isMyLocationEnabled = enabled
    }

    override fun currentCameraZoom(): Float {
        return googleMap.cameraPosition.zoom
    }

    override fun setOnMapClickListener(listener: (latlngW: LatLngW) -> Unit) {
        googleMap.setOnMapClickListener {
            listener.invoke(LatLngW(it.latitude, it.longitude))
        }
    }

    override fun setOnCameraChangeListener(listener: (latlngW: LatLngW, currentZoom: Float) -> Unit) {
        googleMap.setOnCameraMoveListener {
            val ll = googleMap.cameraPosition.target
            listener.invoke(LatLngW(ll.latitude, ll.longitude), googleMap.cameraPosition.zoom)
        }
    }

    override fun setMapStyleType(style: MapStyleType) {
        googleMap.mapType = when (style) {
            MapStyleType.STANDARD -> GoogleMap.MAP_TYPE_NORMAL
            MapStyleType.SATELLITE -> GoogleMap.MAP_TYPE_SATELLITE
            MapStyleType.HYBRID -> GoogleMap.MAP_TYPE_HYBRID
        }
    }

    override fun addCircle(
        latLngW: LatLngW,
        fillColor: Int,
        strokeColor: Int,
        strokeWidth: Float,
        circleRadius: Double
    ) {
        googleMap.addCircle(
            CircleOptions()
                .center(latLngW.toGMSLatLng())
                .fillColor(fillColor)
                .strokeColor(strokeColor)
                .strokeWidth(strokeWidth)
                .radius(circleRadius)
        )
    }

    override fun supportSatelliteAndHybridView(): Boolean {
        return true
    }
}