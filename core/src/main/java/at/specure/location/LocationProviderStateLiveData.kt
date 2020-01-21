package at.specure.location

import androidx.lifecycle.LiveData
import at.specure.location.LocationProviderState.DISABLED_APP
import at.specure.location.LocationProviderState.DISABLED_DEVICE
import at.specure.location.LocationProviderState.ENABLED
import at.specure.util.permission.LocationAccess
import javax.inject.Inject

/**
 * LiveData that observes enabled/disabled states of location providers(gps/network)
 */
class LocationProviderStateLiveData @Inject constructor(
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

    init {
        value = when {
            deviceEnabled && appEnabled -> ENABLED
            !appEnabled -> DISABLED_APP
            !deviceEnabled -> DISABLED_DEVICE
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
        }
    }
}