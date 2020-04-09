package at.specure.location

import androidx.lifecycle.LiveData

/**
 * LiveData that is used to track [LocationState] changes from [LocationStateWatcher]
 */
class LocationStateLiveData(private val stateWatcher: LocationStateWatcher) : LiveData<LocationState?>(), LocationStateWatcher.Listener {

    override fun onLocationStateChanged(state: LocationState?) {
        postValue(state)
    }

    override fun onActive() {
        super.onActive()
        stateWatcher.addListener(this)
    }

    override fun onInactive() {
        super.onInactive()
        stateWatcher.removeListener(this)
    }
}