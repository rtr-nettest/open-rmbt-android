package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.map.DefaultLocation
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.ui.fragment.START_ZOOM_LEVEL
import at.specure.data.entity.TestResultRecord
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

private const val KEY_TEST_UUID = "KEY_TEST_UUID"
private const val KEY_PLAY_SERVICES = "KEY_PLAY_SERVICES"
private const val KEY_ZOOM = "KEY_ZOOM"

private const val KEY_CAMERA_POSITION_LAT = "KEY_CAMERA_POSITION_LAT"
private const val KEY_CAMERA_POSITION_LON = "KEY_CAMERA_POSITION_LON"

private const val KEY_CLOSE_DIALOG_DISPLAYED = "KEY_CLOSE_DIALOG_DISPLAYED"
private const val KEY_MARKER_DETAILS_DISPLAYED = "KEY_MARKER_DETAILS_DISPLAYED"

private const val KEY_COVERAGE_SESSION_DETAILS = "KEY_COVERAGE_SESSION_DETAILS"
private const val KEY_PIP_ACTIVE = "KEY_PIP_ACTIVE"

class CoverageResultViewState constructor(
    val appConfig: AppConfig,

) : ViewState {

    val pipActive = ObservableBoolean(false)
    var playServicesAvailable = ObservableBoolean(false)
    val testResult = ObservableField<TestResultRecord?>()
    val expertModeEnabled = ObservableField(appConfig.expertModeEnabled)
    val coverageModeEnabled = ObservableField(appConfig.coverageModeEnabled)
    var cameraPositionLiveData: MutableLiveData<LatLng> = MutableLiveData()
    var zoom: Float = DefaultLocation.austriaZoomLevel
    var closeDialogDisplayed = ObservableBoolean(false)
    var markerDetailsDisplayed = ObservableBoolean(false)
    val markers: ArrayDeque<Marker> = ArrayDeque()
    val displayedPointIds = mutableSetOf<String>()
    var testUUID: String = ""
    var coverageSessionStart: Long? = 0

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.run {
            testUUID = getString(KEY_TEST_UUID, "")
            pipActive.set(getBoolean(KEY_PIP_ACTIVE, false))
            playServicesAvailable.set(getBoolean(KEY_PLAY_SERVICES))
            markerDetailsDisplayed.set(getBoolean(KEY_MARKER_DETAILS_DISPLAYED))
            zoom = getFloat(KEY_ZOOM)
            coverageSessionStart = getLong(KEY_COVERAGE_SESSION_DETAILS)
            cameraPositionLiveData.postValue(LatLng(getDouble(KEY_CAMERA_POSITION_LAT, DefaultLocation.austriaLocation.latitude), getDouble(KEY_CAMERA_POSITION_LON, DefaultLocation.austriaLocation.longitude)))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_TEST_UUID, testUUID)
            putBoolean(KEY_PLAY_SERVICES, playServicesAvailable.get())
            putBoolean(KEY_CLOSE_DIALOG_DISPLAYED, closeDialogDisplayed.get())
            putBoolean(KEY_MARKER_DETAILS_DISPLAYED, markerDetailsDisplayed.get())
            putBoolean(KEY_PIP_ACTIVE, pipActive.get())
            putFloat(KEY_ZOOM, zoom)
            putLong(KEY_COVERAGE_SESSION_DETAILS, coverageSessionStart ?: 0)
            cameraPositionLiveData.value?.longitude?.let { putDouble(KEY_CAMERA_POSITION_LON, it) }
            cameraPositionLiveData.value?.latitude?.let { putDouble(KEY_CAMERA_POSITION_LAT, it) }
        }
    }
}