package at.rtr.rmbt.android.viewmodel

import at.specure.location.LocationWatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationViewModel @Inject constructor(private val locationProducer: LocationWatcher) : BaseViewModel() {

    fun updateLocationPermissions() {
        locationProducer.updateLocationPermissions()
    }
}