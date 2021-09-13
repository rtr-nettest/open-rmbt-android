package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import at.rtr.rmbt.android.ui.viewstate.MapViewState
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.data.repository.MapRepository
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import com.mapbox.mapboxsdk.geometry.LatLng
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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

//    var providerLiveData: MutableLiveData<RetrofitTileProvider> = MutableLiveData()

    var markersLiveData: LiveData<List<MarkerMeasurementRecord>> =
        Transformations.switchMap(state.coordinatesLiveData) { repository.getMarkers(it?.latitude, it?.longitude, state.zoom.toInt()) }

    init {
        addStateSaveHandler(state)
    }

    fun obtainFilters() {
        // TODO:
    }

    fun loadMarkers(latitude: Double, longitude: Double, zoom: Int) {
        repository.loadMarkers(latitude, longitude, zoom) {
            state.coordinatesLiveData.postValue(LatLng(latitude, longitude))
        }
    }

    fun prepareDetailsLink(openUUID: String) = repository.prepareDetailsLink(openUUID)

    fun provideStyle(): String {
        return "mapbox://styles/specure/ckgqqcmvg51fj19qlisdg0vde"
    }

    fun buildCurrentLayersName(): List<String> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val date = SimpleDateFormat("yyyyMM", Locale.US).format(calendar.time)

        return listOf(
            "C-$date-ALL-ALL",
            "M-$date-ALL-ALL",
            "H10-$date-ALL-ALL",
            "H1-$date-ALL-ALL",
            "H01-$date-ALL-ALL",
            "H001-$date-ALL-ALL"
        )
    }
}

