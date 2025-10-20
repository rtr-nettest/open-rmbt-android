package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import at.rmbt.client.control.data.MapPresentationType
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.map.wrapper.TileW
import at.rtr.rmbt.android.map.wrapper.TileWrapperProvider
import at.rtr.rmbt.android.ui.viewstate.MapViewState
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.data.repository.MapRepository
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import javax.inject.Inject
import javax.inject.Named

class MapViewModel @Inject constructor(
    private val repository: MapRepository,
    @Named("GPSAndNetworkLocationProvider") private val locationWatcher: LocationWatcher
) : BaseViewModel() {

    val state = MapViewState()

    val locationLiveData: LiveData<LocationInfo?>
        get() = locationWatcher.liveData

    val locationStateLiveData: LiveData<LocationState?>
        get() = locationWatcher.stateLiveData

    var providerLiveData: MutableLiveData<RetrofitTileProvider> = MutableLiveData()

    var markersLiveData: LiveData<List<MarkerMeasurementRecord>> =
        state.coordinatesLiveData.switchMap { repository.getMarkers(it?.latitude, it?.longitude, state.zoom.toInt()) }

    init {
        addStateSaveHandler(state)
    }

    fun isFilterLoaded(): Boolean {
        return this.state.isFilterLoaded.get()
    }

    fun obtainFilters() {
        repository.obtainFilters {
            this.state.isFilterLoaded.set(it.filterData.isNotEmpty())
            providerLiveData.postValue(RetrofitTileProvider(repository, state))
        }
    }

    fun loadMarkers(latitude: Double, longitude: Double, zoom: Int) {
        repository.loadMarkers(latitude, longitude, zoom) {
            state.coordinatesLiveData.postValue(LatLngW(latitude, longitude))
        }
    }

    fun prepareDetailsLink(openUUID: String) = repository.prepareDetailsLink(openUUID)
}

class RetrofitTileProvider(private val repository: MapRepository, private val state: MapViewState) : TileWrapperProvider {

    override fun getTileW(x: Int, y: Int, zoom: Int): TileW {
        val type = state.type.get() ?: MapPresentationType.POINTS
        return if (type == MapPresentationType.AUTOMATIC && zoom > 10) {
            TileW(TILE_SIZE, TILE_SIZE, repository.loadAutomaticTiles(x, y, zoom))
        } else {
            TileW(TILE_SIZE, TILE_SIZE, repository.loadTiles(x, y, zoom, type))
        }
    }

    companion object {
        private const val TILE_SIZE = 256
    }
}
