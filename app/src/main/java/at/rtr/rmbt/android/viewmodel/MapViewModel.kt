package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import at.rmbt.client.control.data.MapPresentationType
import at.rtr.rmbt.android.ui.viewstate.MapViewState
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.data.repository.MapRepository
import at.specure.location.LocationInfoLiveData
import at.specure.location.LocationProviderStateLiveData
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import javax.inject.Inject

class MapViewModel @Inject constructor(
    private val repository: MapRepository,
    val locationInfoLiveData: LocationInfoLiveData,
    val locationProviderStateLiveData: LocationProviderStateLiveData
) : BaseViewModel() {

    val state = MapViewState()

    var providerLiveData: MutableLiveData<RetrofitTileProvider> = MutableLiveData()

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
            Tile(256, 256, repository.loadAutomaticTiles(x, y, zoom))
        } else {
            Tile(256, 256, repository.loadTiles(x, y, zoom, type))
        }
    }
}
