package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.client.control.data.MapStyleType
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.ui.fragment.START_ZOOM_LEVEL

private const val KEY_TYPE = "KEY_TYPE"
private const val KEY_STYLE = "KEY_STYLE"
private const val KEY_PLAY_SERVICES = "KEY_PLAY_SERVICES"

private const val KEY_ZOOM = "KEY_ZOOM"

private const val KEY_LATITUDE = "KEY_LATITUDE"
private const val KEY_LONGITUDE = "KEY_LONGITUDE"

private const val KEY_LOCATION_CHANGED = "KEY_LOCATION_CHANGED"
private const val KEY_CAMERA_POSITION_LAT = "KEY_CAMERA_POSITION_LAT"
private const val KEY_CAMERA_POSITION_LON = "KEY_CAMERA_POSITION_LON"

private const val KEY_FILTERS_LOADED = "KEY_FILTERS_LOADED"

class MapViewState : ViewState {

    var isFilterLoaded = ObservableBoolean(false)
    var playServicesAvailable = ObservableBoolean(false)
    var coordinatesLiveData: MutableLiveData<LatLngW> = MutableLiveData()
    var cameraPositionLiveData: MutableLiveData<LatLngW> = MutableLiveData()

    val type = ObservableField<MapPresentationType>(MapPresentationType.AUTOMATIC)
    val style = ObservableField<MapStyleType>(MapStyleType.STANDARD)
    val locationChanged = ObservableBoolean(false)

    var zoom: Float = START_ZOOM_LEVEL

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.run {
            type.set(MapPresentationType.values()[getInt(KEY_TYPE)])
            style.set(MapStyleType.values()[getInt(KEY_STYLE)])
            coordinatesLiveData.postValue(LatLngW(getDouble(KEY_LATITUDE), getDouble(KEY_LONGITUDE)))
            playServicesAvailable.set(getBoolean(KEY_PLAY_SERVICES))
            zoom = getFloat(KEY_ZOOM)
            isFilterLoaded.set(getBoolean(KEY_FILTERS_LOADED, false))
            locationChanged.set(getBoolean(KEY_LOCATION_CHANGED))
            cameraPositionLiveData.postValue(LatLngW(getDouble(KEY_CAMERA_POSITION_LAT), getDouble(KEY_CAMERA_POSITION_LON)))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putInt(KEY_TYPE, type.get()?.ordinal ?: 1)
            putInt(KEY_STYLE, style.get()?.ordinal ?: 0)
            coordinatesLiveData.value?.latitude?.let { putDouble(KEY_LATITUDE, it) }
            coordinatesLiveData.value?.longitude?.let { putDouble(KEY_LONGITUDE, it) }
            putFloat(KEY_ZOOM, zoom)
            putBoolean(KEY_FILTERS_LOADED, isFilterLoaded.get())
            putBoolean(KEY_PLAY_SERVICES, playServicesAvailable.get())
            putBoolean(KEY_LOCATION_CHANGED, locationChanged.get())
            cameraPositionLiveData.value?.longitude?.let { putDouble(KEY_CAMERA_POSITION_LON, it) }
            cameraPositionLiveData.value?.latitude?.let { putDouble(KEY_CAMERA_POSITION_LAT, it) }
        }
    }
}