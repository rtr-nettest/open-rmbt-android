package at.rtr.rmbt.android.ui

import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import at.bluesource.choicesdk.maps.common.Map
import at.bluesource.choicesdk.maps.common.MapFragment
import at.bluesource.choicesdk.maps.common.TileProvider
import at.bluesource.choicesdk.maps.common.options.TileOverlay
import at.bluesource.choicesdk.maps.common.options.TileOverlayOptions
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.observers.DisposableObserver
import timber.log.Timber

/**
 * containerId should always be R.id.mapFrameLayout
 */
fun FragmentManager.loadMapFragment(@IdRes containerId: Int, mapReady : (map : Map) -> Unit) : Disposable {
    val mapFragment: MapFragment = MapFragment()
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

fun Map.addTileOverlayExt(tileProvider: TileProvider) : TileOverlay? {
    return addTileOverlay(
        TileOverlayOptions.create().tileProvider(
        TileProvider.create { x, y, zoom ->
            tileProvider.getTile(x,y,zoom)
        }
    ))
}
