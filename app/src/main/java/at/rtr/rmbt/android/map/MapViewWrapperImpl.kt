package at.rtr.rmbt.android.map

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import at.rtr.rmbt.android.map.wrapper.EmptyMapViewWrapper
import at.rtr.rmbt.android.map.wrapper.MapViewWrapper
import at.rtr.rmbt.android.map.wrapper.MapWrapper
import at.rtr.rmbt.android.util.isGmsAvailable
import at.rtr.rmbt.android.util.isHmsAvailable
import com.google.android.gms.maps.MapView

class MapViewWrapperImpl @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), MapViewWrapper {

    private val mapViewWrapper: MapViewWrapper

    init {
        mapViewWrapper = when {
            context.isGmsAvailable() -> {
                val gmsMapView = MapView(context)
                addView(gmsMapView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
                GoogleMapViewWrapper(gmsMapView)
            }
            context.isHmsAvailable() -> {
                val hmsMapView = com.huawei.hms.maps.MapView(context)
                addView(hmsMapView, LayoutParams(MATCH_PARENT, MATCH_PARENT))
                HuaweiMapViewWrapper(hmsMapView)
            }
            else -> EmptyMapViewWrapper()
        }
    }

    override val mapWrapper: MapWrapper
        get() = mapViewWrapper.mapWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        mapViewWrapper.onCreate(savedInstanceState)
    }

    override fun onResume() {
        mapViewWrapper.onResume()
    }

    override fun onPause() {
        mapViewWrapper.onPause()
    }

    override fun onStart() {
        mapViewWrapper.onStart()
    }

    override fun onStop() {
        mapViewWrapper.onStop()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle?) {
        mapViewWrapper.onSaveInstanceState(savedInstanceState)
    }

    override fun onDestroy() {
        mapViewWrapper.onDestroy()
    }

    override fun loadMapAsync(mapLoaded : () -> Unit) {
        mapViewWrapper.loadMapAsync(mapLoaded)
    }

}