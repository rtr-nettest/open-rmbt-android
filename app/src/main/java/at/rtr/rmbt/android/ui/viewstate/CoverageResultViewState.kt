package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.ui.fragment.START_ZOOM_LEVEL
import at.specure.data.entity.TestResultRecord

private const val KEY_TEST_UUID = "KEY_TEST_UUID"
private const val KEY_PLAY_SERVICES = "KEY_PLAY_SERVICES"
private const val KEY_ZOOM = "KEY_ZOOM"

private const val KEY_LATITUDE = "KEY_LATITUDE"
private const val KEY_LONGITUDE = "KEY_LONGITUDE"

private const val KEY_LOCATION_CHANGED = "KEY_LOCATION_CHANGED"
private const val KEY_CAMERA_POSITION_LAT = "KEY_CAMERA_POSITION_LAT"
private const val KEY_CAMERA_POSITION_LON = "KEY_CAMERA_POSITION_LON"

private const val KEY_CLOSE_DIALOG_DISPLAYED = "KEY_CLOSE_DIALOG_DISPLAYED"
private const val KEY_MARKER_DETAILS_DISPLAYED = "KEY_MARKER_DETAILS_DISPLAYED"

class CoverageResultViewState constructor(
    val appConfig: AppConfig,

) : ViewState {

    var playServicesAvailable = ObservableBoolean(false)
    val testResult = ObservableField<TestResultRecord?>()
    val expertModeEnabled = ObservableField(appConfig.expertModeEnabled)
    val coverageModeEnabled = ObservableField(appConfig.coverageModeEnabled)
    var coordinatesLiveData: MutableLiveData<LatLngW> = MutableLiveData()
    var cameraPositionLiveData: MutableLiveData<LatLngW> = MutableLiveData()
    var zoom: Float = START_ZOOM_LEVEL
    var closeDialogDisplayed = ObservableBoolean(false)
    var markerDetailsDisplayed = ObservableBoolean(false)
    val activeCircles = mutableListOf<com.google.android.gms.maps.model.Circle>()
    val displayedPointIds = mutableSetOf<String>()
    var testUUID: String = ""

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.run {
            testUUID = getString(KEY_TEST_UUID, "")
            playServicesAvailable.set(getBoolean(KEY_PLAY_SERVICES))
            markerDetailsDisplayed.set(getBoolean(KEY_MARKER_DETAILS_DISPLAYED))
            coordinatesLiveData.postValue(LatLngW(getDouble(KEY_LATITUDE), getDouble(KEY_LONGITUDE)))
            zoom = getFloat(KEY_ZOOM)
            cameraPositionLiveData.postValue(LatLngW(getDouble(KEY_CAMERA_POSITION_LAT), getDouble(KEY_CAMERA_POSITION_LON)))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_TEST_UUID, testUUID)
            putBoolean(KEY_PLAY_SERVICES, playServicesAvailable.get())
            putBoolean(KEY_CLOSE_DIALOG_DISPLAYED, closeDialogDisplayed.get())
            putBoolean(KEY_MARKER_DETAILS_DISPLAYED, markerDetailsDisplayed.get())
            coordinatesLiveData.value?.latitude?.let { putDouble(KEY_LATITUDE, it) }
            coordinatesLiveData.value?.longitude?.let { putDouble(KEY_LONGITUDE, it) }
            putFloat(KEY_ZOOM, zoom)
            cameraPositionLiveData.value?.longitude?.let { putDouble(KEY_CAMERA_POSITION_LON, it) }
            cameraPositionLiveData.value?.latitude?.let { putDouble(KEY_CAMERA_POSITION_LAT, it) }
        }
    }
}