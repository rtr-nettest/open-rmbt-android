package at.rtr.rmbt.android.viewmodel

import at.specure.location.LocationProducer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationViewModel @Inject constructor(private val locationProducer: LocationProducer) : BaseViewModel() {

    fun updateLocationPermissions() {
        locationProducer.updateLocationPermissions()
    }
}