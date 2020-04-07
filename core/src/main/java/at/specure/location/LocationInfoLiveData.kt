package at.specure.location

import androidx.lifecycle.LiveData
import javax.inject.Inject

/**
 * LiveData that observes location changes
 */
class LocationInfoLiveData @Inject constructor(private val locationInfo: LocationWatcherOld) : LiveData<LocationInfo>(), LocationWatcherOld.LocationInfoChangeListener {

    override fun onActive() {
        super.onActive()
        locationInfo.addLocationInfoListener(this)
    }

    override fun onInactive() {
        super.onInactive()
        locationInfo.removeLocationInfoListener(this)
    }

    override fun onLocationInfoChanged(locationInfo: LocationInfo?) {
        postValue(locationInfo)
    }
}