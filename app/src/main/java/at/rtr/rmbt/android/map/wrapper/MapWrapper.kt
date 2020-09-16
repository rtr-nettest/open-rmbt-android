package at.rtr.rmbt.android.map.wrapper

import android.content.Context
import android.os.Bundle
import androidx.annotation.DrawableRes
import at.rmbt.client.control.data.MapStyleType

interface MapWrapper {

    fun moveCamera(latLngW : LatLngW, zoom : Float)

    fun animateCamera(latLngW : LatLngW)

    fun animateCamera(latLngW : LatLngW, zoom: Float)

    fun addTileOverlay(tileWProvider : TileWrapperProvider) : TileOverlayWrapper

    fun addMarker(context: Context, latLngW : LatLngW, anchorU : Float, anchorV : Float, @DrawableRes iconId : Int) : MarkerWrapper

    fun setMyLocationEnabled(enabled : Boolean)

    fun currentCameraZoom() : Float

    fun setOnMapClickListener(listener : (latlngW : LatLngW) -> Unit)

    fun setOnCameraChangeListener(listener : (latlngW : LatLngW, currentZoom : Float) -> Unit)

    fun setMapStyleType(style : MapStyleType)
}

interface MapViewWrapper {

    val mapWrapper : MapWrapper

    fun onCreate(savedInstanceState : Bundle?)

    fun onResume()

    fun onPause()

    fun onStart()

    fun onStop()

    fun loadMapAsync(mapLoaded : () -> Unit)
}

class EmptyMapWrapper : MapWrapper {

    override fun moveCamera(latLngW: LatLngW, zoom: Float) {
    }

    override fun animateCamera(latLngW: LatLngW) {

    }

    override fun animateCamera(latLngW: LatLngW, zoom: Float) {
    }

    override fun addTileOverlay(tileWProvider: TileWrapperProvider): TileOverlayWrapper {
        return EmptyTileOverlay()
    }

    override fun addMarker(
        context: Context,
        latLngW: LatLngW,
        anchorU: Float,
        anchorV: Float,
        iconId: Int
    ): MarkerWrapper {
        return EmptyMarker()
    }

    override fun setMyLocationEnabled(enabled: Boolean) {
    }

    override fun currentCameraZoom(): Float {
        return 1f
    }

    override fun setOnMapClickListener(listener: (latlngW: LatLngW) -> Unit) {
    }

    override fun setOnCameraChangeListener(listener: (latlngW: LatLngW, currentZoom: Float) -> Unit) {
    }

    override fun setMapStyleType(style: MapStyleType) {
    }
}

class EmptyMapViewWrapper : MapViewWrapper {

    override val mapWrapper: MapWrapper = EmptyMapWrapper()

    override fun onCreate(savedInstanceState: Bundle?) {
    }

    override fun onResume() {
    }

    override fun onPause() {
    }

    override fun onStart() {
    }

    override fun onStop() {
    }

    override fun loadMapAsync(mapLoaded: () -> Unit) {
    }
}