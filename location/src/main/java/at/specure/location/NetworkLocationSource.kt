package at.specure.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import timber.log.Timber

/**
 * Location source that uses network services to get and observe location
 */
class NetworkLocationSource(context: Context) : LocationSource {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var listener: LocationSource.Listener? = null

    override val location: LocationInfo?
        @SuppressLint("MissingPermission")
        get() = try {
            val location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            location?.let { LocationInfo(it) }
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to get last known network location")
            null
        }

    private val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location?) {
            listener?.onLocationChanged(location?.let { LocationInfo(it) })
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String?) {}

        override fun onProviderDisabled(provider: String?) {}
    }

    @SuppressLint("MissingPermission")
    override fun start(listener: LocationSource.Listener) {
        try {
            this.listener = listener
            manager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                LocationSource.MINIMUM_UPDATE_TIME_MS,
                LocationSource.MINIMUM_DISTANCE_METERS,
                locationListener
            )
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to register gps updates")
        }
    }

    override fun stop() {
        try {
            listener = null
            manager.removeUpdates(locationListener)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to unregister gps updates")
        }
    }
}