package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableField
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.util.addOnPropertyChanged

private const val KEY_NETWORKS = "networks"
private const val KEY_DEVICES = "devices"

class CoverageSettingsViewState(
    val appConfig: AppConfig,
) : ViewState {

    val fenceRadiusMeters = ObservableField(appConfig.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement)
    val locationAccuracyMeters = ObservableField(appConfig.minLocationAccuracyMetersDuringSignalMeasurement)

    init {
        fenceRadiusMeters.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement = it
            }
        }
        locationAccuracyMeters.addOnPropertyChanged { value ->
            value.get()?.let {
                appConfig.minLocationAccuracyMetersDuringSignalMeasurement = it
            }
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        super.onSaveState(bundle)
        bundle?.apply {
//            putString(KEY_NETWORKS, networks.get())
//            putString(KEY_DEVICES, devices.get())
        }
    }

    override fun onRestoreState(bundle: Bundle?) {
        super.onRestoreState(bundle)
        bundle?.run {
//            networks.set(getString(KEY_NETWORKS))
//            devices.set(getString(KEY_DEVICES))
        }
    }
}