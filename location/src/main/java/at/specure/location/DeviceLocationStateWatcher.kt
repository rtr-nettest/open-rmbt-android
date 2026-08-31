package at.specure.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import java.util.Collections

/**
 * State location watcher that is used to monitor System location permission changes
 */
class DeviceLocationStateWatcher(private val context: Context) {

    private val monitor = Any()
    private val locationManager: LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val listeners = Collections.synchronizedSet(mutableSetOf<Listener>())

    val isEnabled: Boolean
        get() = LocationManagerCompat.isLocationEnabled(locationManager)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent.action) {
                notifyStateChanged()
            }
        }
    }

    fun addListener(listener: Listener) {
        synchronized(monitor) {
            // Register only on the genuine 0 -> 1 transition; re-adding a known listener must not
            // register the receiver a second time.
            if (listeners.add(listener) && listeners.size == 1) {
                context.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
            }
        }
        notifyStateChanged()
    }

    fun removeListener(listener: Listener) {
        synchronized(monitor) {
            // Unregister only when this call actually emptied the set; a double remove (e.g. two
            // stopMeasurement() calls) would otherwise unregister an already-unregistered receiver
            // and crash with "Receiver not registered".
            if (listeners.remove(listener) && listeners.isEmpty()) {
                context.unregisterReceiver(receiver)
            }
        }
    }

    private fun notifyStateChanged() {
        listeners.forEach { it.onDeviceLocationStateChanged(isEnabled) }
    }

    interface Listener {

        fun onDeviceLocationStateChanged(isEnabled: Boolean)
    }
}