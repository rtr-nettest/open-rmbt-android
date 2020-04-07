package at.specure.location

import android.content.Context
import android.location.Location
//import com.google.android.gms.location.LocationCallback
//import com.google.android.gms.location.LocationRequest
//import com.google.android.gms.location.LocationResult
//import com.google.android.gms.location.LocationServices

class LocationInfoWatcherFusedImpl(context: Context) : LocationWatcherOld {

    /*private var handlerThread: HandlerThread? = null

    private val locationRequest = LocationRequest()
    private val monitor = Any()

    private var lastKnownLocation: Location? = null
        get() {
            if (field == null) {
                mFusedLocationProviderClient.lastLocation.addOnSuccessListener {
                    field = it
                }.addOnCanceledListener {
                    field = null
                }.addOnFailureListener {
                    field = null
                }
            }
            return field
        }

    private val lastKnownLocationInfo: LocationInfo?
        get() {
            val latestLocation = getLatestLocation()
            if (latestLocation != null) {
                return LocationInfo(latestLocation)
            }
            return null
        }

    private val locationInfoListeners = Collections.synchronizedSet(mutableSetOf<LocationWatcher.LocationInfoChangeListener>())

    private val mFusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(location: LocationResult?) {
            lastKnownLocation = location?.lastLocation
            notifyInfoListeners(lastKnownLocationInfo)
        }
    }

    init {
        locationRequest.interval = LocationWatcher.MINIMUM_UPDATE_TIME_MS
        locationRequest.fastestInterval = LocationWatcher.MINIMUM_UPDATE_TIME_MS
        locationRequest.smallestDisplacement = LocationWatcher.MINIMUM_DISTANCE_METERS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }*/

    override fun removeLocationInfoListener(listener: LocationWatcherOld.LocationInfoChangeListener) {
        /*locationInfoListeners.remove(listener)
        if (locationInfoListeners.isEmpty()) {
            unregisterReceiver()
        }*/
    }

    override fun addLocationInfoListener(listener: LocationWatcherOld.LocationInfoChangeListener) {
        /*locationInfoListeners.add(listener)
        registerReceiver()
        if (lastKnownLocationInfo != null) {
            notifyInfoListeners(lastKnownLocationInfo)
        }*/
    }

    private fun registerReceiver() {

        /*if (handlerThread == null) {
            handlerThread = object : HandlerThread("locationAcquiringThread") {
                override fun onLooperPrepared() {
                    super.onLooperPrepared()
                    synchronized(monitor) {
                        (monitor as java.lang.Object).notify()
                    }
                }
            }
            if (!handlerThread!!.isAlive) {
                handlerThread!!.start()

                synchronized(monitor) {
                    try {
                        (monitor as java.lang.Object).wait()
                    } catch (e: InterruptedException) {
                        Timber.e(e)
                    }
                }
            }
        }

        val task = mFusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            handlerThread!!.looper
        )

        if (!task.isSuccessful) {
            Timber.e("Location updates will not be delivered. Failed requesting fused location updates!")
        }*/
    }

    /*private fun unregisterReceiver() {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback)
        handlerThread?.quit()
        handlerThread = null
    }*/

    override fun getLatestLocation(): Location? {
//        return lastKnownLocation
        return null
    }

    override fun getLatestLocationInfo(): LocationInfo? {
//        return lastKnownLocationInfo
        return null
    }

    /*private fun notifyInfoListeners(lastKnownLocationInfo: LocationInfo?) {
        locationInfoListeners.synchronizedForEach { it.onLocationInfoChanged(lastKnownLocationInfo) }
    }*/
}