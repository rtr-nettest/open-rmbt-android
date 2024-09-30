package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.ui.fragment.START_ZOOM_LEVEL
import at.rtr.rmbt.android.util.InfoWindowStatus
import at.rtr.rmbt.android.util.InformationAccessProblem
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.specure.data.MeasurementServers
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.ip.IpInfo
import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.location.LocationState

private const val KEY_IAP = "KEY_IAP"
private const val KEY_ZOOM = "KEY_ZOOM"

private const val KEY_LATITUDE = "KEY_LATITUDE"
private const val KEY_LONGITUDE = "KEY_LONGITUDE"

private const val KEY_LOCATION_CHANGED = "KEY_LOCATION_CHANGED"
private const val KEY_CAMERA_POSITION_LAT = "KEY_CAMERA_POSITION_LAT"
private const val KEY_CAMERA_POSITION_LON = "KEY_CAMERA_POSITION_LON"

private const val KEY_CLOSE_DIALOG_DISPLAYED = "KEY_CLOSE_DIALOG_DISPLAYED"

class HomeViewState(
    private val config: AppConfig,
    private val measurementServers: MeasurementServers
) : ViewState {

    val isConnected = ObservableField<Boolean?>()
    val isLocationEnabled = ObservableField<LocationState>()
    val signalStrength = ObservableField<SignalStrengthInfo>()
    val secondary5GSignalStrength = ObservableField<SignalStrengthInfo>()
    val activeNetworkInfo = ObservableField<DetailedNetworkInfo?>()
    val secondary5GActiveNetworkInfo = ObservableField<CellNetworkInfo?>()
    val infoWindowStatus = ObservableField(InfoWindowStatus.NONE)
    val ipV4Info = ObservableField<IpInfo?>()
    val ipV6Info = ObservableField<IpInfo?>()
    val isSignalMeasurementActive = ObservableField<Boolean>()
    val isLoopModeActive = ObservableBoolean(config.loopModeEnabled)
    val expertModeIsEnabled = ObservableField(config.expertModeEnabled)
    val developerModeIsEnabled = ObservableField(config.developerModeIsEnabled)
    val coverageModeIsEnabled = ObservableField(config.coverageModeEnabled)
    val selectedMeasurementServer = ObservableField(measurementServers.selectedMeasurementServer)
    val informationAccessProblem = ObservableField(InformationAccessProblem.NO_PROBLEM)
    val locationChanged = ObservableBoolean(false)
    val locationWarningDialogSilenced = ObservableBoolean(false)

    var coordinatesLiveData: MutableLiveData<LatLngW> = MutableLiveData()
    var cameraPositionLiveData: MutableLiveData<LatLngW> = MutableLiveData()
    var zoom: Float = START_ZOOM_LEVEL
    var closeDialogDisplayed = ObservableBoolean(false)

    init {
        isLoopModeActive.addOnPropertyChanged {
            config.loopModeEnabled = it.get()
        }
    }

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.run {
            informationAccessProblem.set(InformationAccessProblem.values()[(getInt(KEY_IAP))])
            locationChanged.set(getBoolean(KEY_LOCATION_CHANGED))
            closeDialogDisplayed.set(getBoolean(KEY_CLOSE_DIALOG_DISPLAYED))
            coordinatesLiveData.postValue(LatLngW(getDouble(KEY_LATITUDE), getDouble(KEY_LONGITUDE)))
            zoom = getFloat(KEY_ZOOM)
            cameraPositionLiveData.postValue(LatLngW(getDouble(KEY_CAMERA_POSITION_LAT), getDouble(KEY_CAMERA_POSITION_LON)))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putInt(KEY_IAP, informationAccessProblem.get()?.ordinal ?: InformationAccessProblem.NO_PROBLEM.ordinal)
            putBoolean(KEY_LOCATION_CHANGED, locationChanged.get())
            putBoolean(KEY_CLOSE_DIALOG_DISPLAYED, closeDialogDisplayed.get())
            coordinatesLiveData.value?.latitude?.let { putDouble(KEY_LATITUDE, it) }
            coordinatesLiveData.value?.longitude?.let { putDouble(KEY_LONGITUDE, it) }
            putFloat(KEY_ZOOM, zoom)
            cameraPositionLiveData.value?.longitude?.let { putDouble(KEY_CAMERA_POSITION_LON, it) }
            cameraPositionLiveData.value?.latitude?.let { putDouble(KEY_CAMERA_POSITION_LAT, it) }
        }
    }

    fun checkConfig() {
        isLoopModeActive.set(config.loopModeEnabled)
        developerModeIsEnabled.set(config.developerModeIsEnabled)
        coverageModeIsEnabled.set(config.coverageModeEnabled)
        selectedMeasurementServer.set(measurementServers.selectedMeasurementServer)
        expertModeIsEnabled.set(config.expertModeEnabled)
    }
}