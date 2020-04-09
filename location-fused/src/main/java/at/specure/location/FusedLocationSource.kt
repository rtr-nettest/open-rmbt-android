package at.specure.location

import android.content.Context
import android.location.Location
import android.os.HandlerThread
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val LATEST_LOCATION_MAX_AGE_SEC = 30L

/**
 * Location Source that uses fused location manager and Google Play services
 */
class FusedLocationSource(context: Context) : LocationSource {

    private val manager = LocationServices.getFusedLocationProviderClient(context)

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val monitor: java.lang.Object = Any() as java.lang.Object

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val lastLocationMonitor: java.lang.Object = Any() as java.lang.Object

    private var latestLocation: LocationInfo? = null
    private var listener: LocationSource.Listener? = null
    private var handlerThread: HandlerThread? = null

    private val fetchedLocation: LocationInfo?
        get() {
            var location: Location? = null
            synchronized(lastLocationMonitor) {
                manager.lastLocation.addOnCompleteListener { task ->
                    location = task.result
                    synchronized(lastLocationMonitor) {
                        lastLocationMonitor.notify()
                    }
                }
                synchronized(lastLocationMonitor) {
                    lastLocationMonitor.wait(200, 1000)
                }
            }
            Timber.e("location update: GET FETCHED SOURCE!")
            return location?.let { LocationInfo(it) }
        }

    private val locationRequest = LocationRequest().apply {
        interval = LocationSource.MINIMUM_UPDATE_TIME_MS
        fastestInterval = LocationSource.MINIMUM_UPDATE_TIME_MS
        smallestDisplacement = LocationSource.MINIMUM_DISTANCE_METERS
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            val location = result?.let { LocationInfo(result.lastLocation) }
            latestLocation = location

            Timber.d("location update: fused location result ${result.toString()}")
            listener?.onLocationChanged(latestLocation)
        }
    }

    override val location: LocationInfo?
        get() {
            val ageSeconds = TimeUnit.NANOSECONDS.toSeconds(latestLocation?.ageNanos ?: LATEST_LOCATION_MAX_AGE_SEC)
            return if (ageSeconds >= LATEST_LOCATION_MAX_AGE_SEC) {
                fetchedLocation
            } else {
                latestLocation
            }
        }

    override fun start(listener: LocationSource.Listener) {
        Timber.d("location update: request start")
        this.listener = listener
        if (handlerThread == null) {
            handlerThread = object : HandlerThread("locationAcquiringThread") {
                override fun onLooperPrepared() {
                    super.onLooperPrepared()
                    synchronized(monitor) {
                        monitor.notify()
                    }
                }
            }
            handlerThread?.start()
            synchronized(monitor) {
                try {
                    monitor.wait()
                } catch (ex: InterruptedException) {
                    Timber.e(ex)
                }
            }

            val task = manager.requestLocationUpdates(locationRequest, locationCallback, handlerThread!!.looper)

            if (!task.isSuccessful) {
                Timber.e(task.exception, "Location updates will not be delivered. Failed requesting fused location updates!")
            }
        }
    }

    override fun stop() {
        synchronized(monitor) {
            manager.removeLocationUpdates(locationCallback)
            handlerThread?.quit()
            handlerThread = null
            listener = null
        }
    }
}