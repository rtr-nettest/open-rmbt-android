package at.specure.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import java.util.Collections

class LocationProviderStateWatcherImpl(val context: Context) : LocationProviderStateWatcher {

    private val listeners =
        Collections.synchronizedSet(mutableSetOf<LocationProviderStateWatcher.LocationEnabledChangeListener>())
    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val gpsSwitchStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent.action) {
                notifyStateChanged()
            }
        }
    }

    override fun addListener(listener: LocationProviderStateWatcher.LocationEnabledChangeListener) {
        listeners.add(listener)
        if (listeners.size == 1) {
            registerReceiver()
        }
        notifyStateChanged()
    }

    override fun removeListener(listener: LocationProviderStateWatcher.LocationEnabledChangeListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            unregisterReceiver()
        }
    }

    override fun isLocationEnabled(): Boolean {
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun registerReceiver() {
        context.registerReceiver(gpsSwitchStateReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    private fun unregisterReceiver() {
        context.unregisterReceiver(gpsSwitchStateReceiver)
    }

    private fun notifyStateChanged() {
        listeners.forEach { it.onLocationStateChange(isLocationEnabled()) }
    }
}