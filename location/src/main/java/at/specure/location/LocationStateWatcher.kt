package at.specure.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import timber.log.Timber
import java.util.Collections

/**
 * Class that is used to track location permission changes
 */
class LocationStateWatcher(private val context: Context) : DeviceLocationStateWatcher.Listener {

    private val monitor = Any()
    private val listeners = Collections.synchronizedSet(mutableSetOf<Listener>())
    private val deviceStateWatcher = DeviceLocationStateWatcher(context)

    /**
     * Location provider is enabled at the device settings
     */
    private val deviceEnabled: Boolean
        get() = deviceStateWatcher.isEnabled

    /**
     * Permission granted for app
     */
    private val appEnabled: Boolean
        get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val state: LocationState?
        get() = when {
            deviceEnabled && appEnabled -> LocationState.ENABLED
            !appEnabled -> LocationState.DISABLED_APP
            !deviceEnabled -> LocationState.DISABLED_DEVICE
            !isLocationManagerEnabled -> LocationState.DISABLED_DEVICE
            else -> null
        }

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

    fun addListener(listener: Listener) {
        synchronized(monitor) {
            listeners.add(listener)
            if (listeners.size == 1) {
                deviceStateWatcher.addListener(this)
            }
            listener.onLocationStateChanged(state)
        }
    }

    fun removeListener(listener: Listener) {
        synchronized(monitor) {
            listeners.remove(listener)
            if (listeners.isEmpty()) {
                deviceStateWatcher.removeListener(this)
            }
        }
    }

    fun updateLocationPermissions() {
        notifyChanged()
    }

    override fun onDeviceLocationStateChanged(isEnabled: Boolean) {
        notifyChanged()
    }

    private fun notifyChanged() {
        val localState = state
        synchronized(monitor) {
            listeners.forEach { it.onLocationStateChanged(localState) }
        }
    }

    interface Listener {

        fun onLocationStateChanged(state: LocationState?)
    }
}