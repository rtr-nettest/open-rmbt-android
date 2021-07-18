package at.rtr.rmbt.android.ui

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import at.bluesource.choicesdk.core.MobileService
import at.bluesource.choicesdk.core.MobileServicesDetector
import at.bluesource.choicesdk.maps.common.Map
import at.bluesource.choicesdk.maps.common.MapFragment
import at.bluesource.choicesdk.maps.common.options.TileOverlay
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileOverlayOptions
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.DisposableObserver
import timber.log.Timber

/**
 * containerId should always be R.id.mapFrameLayout
 */
fun FragmentManager.loadMapFragment(@IdRes containerId: Int, mapReady : (map : Map) -> Unit) : Disposable {
    val mapFragment: MapFragment = MapFragmentPatched()
    beginTransaction().apply {
        replace(containerId, mapFragment)
        commit()
    }

    val disposable = object : DisposableObserver<Map>() {
        override fun onComplete() {}

        override fun onNext(map: Map) {
            with(map.getUiSettings()) {
                isRotateGesturesEnabled = false
                isMyLocationButtonEnabled = false
                isZoomControlsEnabled = false
                isCompassEnabled = false
                isIndoorLevelPickerEnabled = false
                isTiltGesturesEnabled = false
            }

            mapReady(map)
        }

        override fun onError(e: Throwable) {
            Timber.e(e, "Failed to load map")
        }
    }
    mapFragment.getMapObservable().subscribe(disposable)

    return disposable
}

fun Map.addTileOverlayPatched(tileProvider: at.bluesource.choicesdk.maps.common.TileProvider) : TileOverlay? {
    return when(MobileServicesDetector.getAvailableMobileService()) {
        MobileService.GMS -> {
            TileOverlayImpl(getGoogleMap()?.addTileOverlay(TileOverlayOptions()
                .fadeIn(true)
                .tileProvider { x, y, zoom ->
                    val cT = tileProvider.getTile(x, y, zoom)!!
                    Tile(cT.width, cT.height, cT.data)
                }
            ), null)
        }
        MobileService.HMS -> {
            TileOverlayImpl(null, getHuaweiMap()?.addTileOverlay(com.huawei.hms.maps.model.TileOverlayOptions()
                .fadeIn(true)
                .tileProvider { x, y, zoom ->
                    val cT = tileProvider.getTile(x, y, zoom)!!
                    com.huawei.hms.maps.model.Tile(cT.width, cT.height, cT.data)
                }
            ))
        }
        else -> null
    }
}

class TileOverlayImpl(private val gmsTileOverlay: com.google.android.gms.maps.model.TileOverlay?,
                      private val hmsTileOverlay: com.huawei.hms.maps.model.TileOverlay?) : TileOverlay {
    override fun remove() {
        gmsTileOverlay?.remove()
        hmsTileOverlay?.remove()
    }

    override fun toGmsTileOverlay(): com.google.android.gms.maps.model.TileOverlay {
        TODO("Not yet implemented")
    }
}

