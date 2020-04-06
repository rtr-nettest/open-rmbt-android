package at.specure.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import java.util.Collections

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
            listeners.add(listener)
            if (listeners.size == 1) {
                context.registerReceiver(receiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
            }
        }
        notifyStateChanged()
    }

    fun removeListener(listener: Listener) {
        synchronized(monitor) {
            listeners.remove(listener)
            if (listeners.isEmpty()) {
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