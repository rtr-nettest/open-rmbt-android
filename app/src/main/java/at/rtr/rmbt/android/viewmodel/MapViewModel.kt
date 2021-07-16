package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import at.bluesource.choicesdk.maps.common.LatLng
import at.bluesource.choicesdk.maps.common.Tile
import at.bluesource.choicesdk.maps.common.TileProvider
import at.rmbt.client.control.data.MapPresentationType
import at.rtr.rmbt.android.ui.viewstate.MapViewState
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.data.repository.MapRepository
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import javax.inject.Inject

class MapViewModel @Inject constructor(
    private val repository: MapRepository,
    private val locationWatcher: LocationWatcher
) : BaseViewModel() {

    val state = MapViewState()

    val locationLiveData: LiveData<LocationInfo?>
        get() = locationWatcher.liveData

    val locationStateLiveData: LiveData<LocationState?>
        get() = locationWatcher.stateLiveData

    var providerLiveData: MutableLiveData<TileProvider> = MutableLiveData()

    var markersLiveData: LiveData<List<MarkerMeasurementRecord>> =
        Transformations.switchMap(state.coordinatesLiveData) { repository.getMarkers(it?.latitude, it?.longitude, state.zoom.toInt()) }

    init {
        addStateSaveHandler(state)
    }

    fun obtainFilters() {
        repository.obtainFilters {
            providerLiveData.postValue(RetrofitTileProvider(repository, state))
        }
    }

    fun loadMarkers(latitude: Double, longitude: Double, zoom: Int) {
        repository.loadMarkers(latitude, longitude, zoom) {
            state.coordinatesLiveData.postValue(LatLng(latitude, longitude))
        }
    }

    fun prepareDetailsLink(openUUID: String) = repository.prepareDetailsLink(openUUID)
}

class RetrofitTileProvider(private val repository: MapRepository, private val state: MapViewState) : TileProvider {

    override fun getTile(x: Int, y: Int, zoom: Int): Tile {
        val type = state.type.get() ?: MapPresentationType.POINTS
        return if (type == MapPresentationType.AUTOMATIC && zoom > 10) {
            Tile.getFactory().create(TILE_SIZE, TILE_SIZE, repository.loadAutomaticTiles(x, y, zoom))
        } else {
            Tile.getFactory().create(TILE_SIZE, TILE_SIZE, repository.loadTiles(x, y, zoom, type))
        }
    }

    companion object {
        private const val TILE_SIZE = 256
    }
}
