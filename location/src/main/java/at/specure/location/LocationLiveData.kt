package at.specure.location

import androidx.lifecycle.MutableLiveData

class LocationLiveData(private val producer: LocationWatcher) : MutableLiveData<LocationInfo>(), LocationWatcher.Listener {

    override fun onActive() {
        super.onActive()
        producer.addListener(this)
    }

    override fun onInactive() {
        super.onInactive()
        producer.removeListener(this)
    }

    override fun onLocationInfoChanged(locationInfo: LocationInfo?) {
        postValue(locationInfo)
    }
}