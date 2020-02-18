package at.specure.location

import android.content.Context
import android.location.LocationManager
import androidx.lifecycle.LiveData
import at.specure.location.LocationProviderState.DISABLED_APP
import at.specure.location.LocationProviderState.DISABLED_DEVICE
import at.specure.location.LocationProviderState.ENABLED
import at.specure.util.permission.LocationAccess
import timber.log.Timber
import javax.inject.Inject

/**
 * LiveData that observes enabled/disabled states of location providers(gps/network)
 */
class LocationProviderStateLiveData @Inject constructor(
    private val context: Context,
    private val locationState: LocationProviderStateWatcher,
    private val locationAccess: LocationAccess
) : LiveData<LocationProviderState>(), LocationProviderStateWatcher.LocationEnabledChangeListener,
    LocationAccess.LocationAccessChangeListener {

    /**
     * Location provider is enabled at the device settings
     */
    private var deviceEnabled: Boolean = locationState.isLocationEnabled()

    /**
     * Permission granted for app
     */
    private var appEnabled: Boolean = locationAccess.isAllowed

    private val isLocationManagerEnabled: Boolean
        get() {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var gpsEnabled = false
            var networkEnabled = false

            try {
                gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } catch (ex: Exception) {
                Timber.w(ex)
            }

            try {
                networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (ex: Exception) {
                Timber.w(ex)
            }

            return gpsEnabled && networkEnabled
        }

    init {
        value = when {
            deviceEnabled && appEnabled -> ENABLED
            !appEnabled -> DISABLED_APP
            !deviceEnabled -> DISABLED_DEVICE
            else -> null
        }
    }

    override fun getValue(): LocationProviderState? {
        deviceEnabled = locationState.isLocationEnabled()
        appEnabled = locationAccess.isAllowed

        return when {
            deviceEnabled && appEnabled -> ENABLED
            !appEnabled -> DISABLED_APP
            !deviceEnabled -> DISABLED_DEVICE
            !isLocationManagerEnabled -> DISABLED_DEVICE
            else -> null
        }
    }

    override fun onActive() {
        super.onActive()
        locationState.addListener(this)
        locationAccess.addListener(this)

        deviceEnabled = locationState.isLocationEnabled()
        appEnabled = locationAccess.isAllowed
        notifyChange()
    }

    override fun onInactive() {
        super.onInactive()
        locationState.removeListener(this)
        locationAccess.removeListener(this)
    }

    override fun onLocationStateChange(enabled: Boolean) {
        deviceEnabled = enabled
        notifyChange()
    }

    override fun onLocationAccessChanged(isAllowed: Boolean) {
        appEnabled = isAllowed
        notifyChange()
    }

    private fun notifyChange() {
        when {
            deviceEnabled && appEnabled -> postValue(ENABLED)
            !appEnabled -> postValue(DISABLED_APP)
            !deviceEnabled -> postValue(DISABLED_DEVICE)
            !isLocationManagerEnabled -> postValue(DISABLED_DEVICE)
        }
    }
}