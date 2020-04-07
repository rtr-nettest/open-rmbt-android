package at.specure.location

import timber.log.Timber

private const val LOCATION_MAX_AGE = 30_000L

class DefaultLocationDispatcher : LocationDispatcher {

    private var lastLocation: LocationInfo? = null
    private var lastSource: LocationSource? = null
    private var lastTimestamp = 0L

    override val latestLocation: LocationInfo?
        get() = lastLocation

    override fun onPermissionsDisabled() {
        lastLocation = null
        lastSource = null
        lastTimestamp = 0
    }

    override fun onLocationInfoChanged(source: LocationSource, location: LocationInfo?): LocationDispatcher.Decision {
        Timber.i("location update: new location came: ${location?.provider} ${location?.accuracy}")
        val timestamp = System.currentTimeMillis()
        if (location == null) {
            return if (lastSource == source) {
                LocationDispatcher.Decision(null, true)
            } else {
                LocationDispatcher.Decision(null, false)
            }
        } else {
            return if (lastLocation == null) {
                Timber.v("location update: no last location")
                updateLastLocation(location, timestamp, source)
            } else if (lastLocation?.accuracy ?: Float.MAX_VALUE > location.accuracy) {
                Timber.v("location update: accuracy is better")
                updateLastLocation(location, timestamp, source)
            } else if (timestamp >= lastTimestamp + LOCATION_MAX_AGE) {
                Timber.v("location update: last outdated")
                updateLastLocation(location, timestamp, source)
            } else if (lastSource != null && lastSource == source) {
                Timber.v("location update: source")
                updateLastLocation(location, timestamp, source)
            } else {
                Timber.v("location update: else")
                LocationDispatcher.Decision(null, false)
            }
        }
    }

    private fun updateLastLocation(location: LocationInfo?, timestamp: Long, source: LocationSource): LocationDispatcher.Decision {
        lastLocation = location
        lastTimestamp = timestamp
        lastSource = source
        return LocationDispatcher.Decision(location, true)
    }
}