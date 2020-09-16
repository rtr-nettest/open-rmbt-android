package at.rtr.rmbt.android.map

import android.os.Bundle
import at.rtr.rmbt.android.map.wrapper.EmptyMapWrapper
import at.rtr.rmbt.android.map.wrapper.MapViewWrapper
import at.rtr.rmbt.android.map.wrapper.MapWrapper
import com.google.android.gms.maps.MapView

class GoogleMapViewWrapper(private val mapView : MapView) : MapViewWrapper {

    private lateinit var wrapper: MapWrapper

    override val mapWrapper: MapWrapper
        get() {
            return if(!this::wrapper.isInitialized) {
                EmptyMapWrapper()
            } else {
                wrapper
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
    }

    override fun onResume() {
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
    }

    override fun onStart() {
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle?) {
        mapView.onSaveInstanceState(savedInstanceState)
    }

    override fun onDestroy() {
        mapView.onDestroy()
    }

    override fun loadMapAsync(mapLoaded: () -> Unit) {
        mapView.getMapAsync {
            with(it.uiSettings) {
                isRotateGesturesEnabled = false
                isMyLocationButtonEnabled = false
            }
            wrapper = GoogleMapWrapper(it)
            mapLoaded.invoke()
        }
    }
}