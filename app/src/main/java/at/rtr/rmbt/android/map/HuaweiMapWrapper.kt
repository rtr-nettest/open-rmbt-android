package at.rtr.rmbt.android.map

import android.content.Context
import at.rmbt.client.control.data.MapStyleType
import at.rtr.rmbt.android.map.wrapper.*
import at.rtr.rmbt.android.util.iconFromVector
import com.huawei.hms.maps.CameraUpdate
import com.huawei.hms.maps.CameraUpdateFactory
import com.huawei.hms.maps.HuaweiMap
import com.huawei.hms.maps.model.MarkerOptions
import com.huawei.hms.maps.model.Tile
import com.huawei.hms.maps.model.TileOverlayOptions
import com.huawei.hms.maps.model.TileProvider
import com.huawei.hms.maps.model.TileProvider.NO_TILE
import timber.log.Timber

class HuaweiMapWrapper(private val huaweiMap : HuaweiMap) : MapWrapper {

    override fun moveCamera(latLngW: LatLngW, zoom: Float) {
        huaweiMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngW.toHMSLatLng(), zoom))
    }

    override fun animateCamera(latLngW: LatLngW) {
        huaweiMap.animateCamera(CameraUpdateFactory.newLatLng(latLngW.toHMSLatLng()))
    }

    override fun animateCamera(latLngW: LatLngW, zoom: Float) {
        huaweiMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngW.toHMSLatLng(), zoom))
    }

    override fun addTileOverlay(tileWProvider: TileWrapperProvider): TileOverlayWrapper {
        val overlay = huaweiMap.addTileOverlay(TileOverlayOptions().tileProvider { x, y, zoom ->
            return@tileProvider try {
                tileWProvider.getTileW(x,y,zoom).toHMSTile()
            } catch (e : Exception) {
                Timber.e(e, "Failed to load tile")
                NO_TILE
            }
        })
        return HMSOverlayWrapper(overlay)
    }

    override fun addMarker(
        context: Context,
        latLngW: LatLngW,
        anchorU: Float,
        anchorV: Float,
        iconId: Int
    ): MarkerWrapper {
        val marker = huaweiMap.addMarker(MarkerOptions()
            .position(latLngW.toHMSLatLng())
            .anchorMarker(anchorU, anchorV)
            .iconFromVector(context, iconId)
        )
        return HMSMarker(marker)
    }

    override fun setMyLocationEnabled(enabled: Boolean) {
        huaweiMap.isMyLocationEnabled = enabled
    }

    override fun currentCameraZoom(): Float {
        return huaweiMap.cameraPosition?.zoom ?: 1f
    }

    override fun setOnMapClickListener(listener: (latlngW: LatLngW) -> Unit) {
        huaweiMap.setOnMapClickListener {
            listener.invoke(LatLngW(it.latitude, it.longitude))
        }
    }

    override fun setOnCameraChangeListener(listener: (latlngW: LatLngW, currentZoom: Float) -> Unit) {
        huaweiMap.setOnCameraMoveListener {
            val ll = huaweiMap.cameraPosition.target
            listener.invoke(LatLngW(ll.latitude, ll.longitude), huaweiMap.cameraPosition.zoom)
        }
    }

    override fun setMapStyleType(style: MapStyleType) {
        huaweiMap.mapType = when(style) {
            MapStyleType.STANDARD -> HuaweiMap.MAP_TYPE_NORMAL
            MapStyleType.SATELLITE -> HuaweiMap.MAP_TYPE_SATELLITE
            MapStyleType.HYBRID -> HuaweiMap.MAP_TYPE_HYBRID
        }
    }

}