package at.rtr.rmbt.android.map

import android.os.Bundle
import at.rtr.rmbt.android.map.wrapper.EmptyMapWrapper
import at.rtr.rmbt.android.map.wrapper.MapViewWrapper
import at.rtr.rmbt.android.map.wrapper.MapWrapper
import com.huawei.hms.maps.SupportMapFragment

class HuaweiMapViewWrapper(private val mapFragment: SupportMapFragment) : MapViewWrapper {

    private lateinit var wrapper: MapWrapper

    override val mapWrapper: MapWrapper
        get() {
            return if (!this::wrapper.isInitialized) {
                EmptyMapWrapper()
            } else {
                wrapper
            }
        }

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

    override fun onSaveInstanceState(savedInstanceState: Bundle?) {
    }

    override fun onDestroy() {
    }

    override fun loadMapAsync(mapLoaded: () -> Unit) {
        mapFragment.getMapAsync {
            with(it.uiSettings) {
                isRotateGesturesEnabled = false
                isMyLocationButtonEnabled = false
                isZoomControlsEnabled = false
                isCompassEnabled = false
                isIndoorLevelPickerEnabled = false
                isTiltGesturesEnabled = false
            }
            wrapper = HuaweiMapWrapper(it)
            mapLoaded.invoke()
        }
    }
}