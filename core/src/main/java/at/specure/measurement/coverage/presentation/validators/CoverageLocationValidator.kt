package at.specure.measurement.coverage.presentation.validators

import at.specure.config.Config
import at.specure.measurement.coverage.domain.validators.LocationValidator
import at.specure.test.DeviceInfo
import at.specure.test.toLocation
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class CoverageLocationValidator(
    private val appConfig: Config,
    ): LocationValidator {
    override fun isLocationFreshAndAccurate(newLocation: DeviceInfo.Location?): Boolean {
        return isLocationNotTooOld(newLocation) && isLocationAccuracyPreciseEnough(newLocation)
    }

    override fun isLocationValidAndDistantEnough(newLocation: DeviceInfo.Location?, lastSavedLocation: DeviceInfo.Location?): Boolean {
        if (newLocation == null) return false

        if (!isLocationFreshAndAccurate(newLocation)) {
            Timber.d("Accuracy or age is not enough $newLocation")
            return false
        }

        val newLocationInfo = newLocation.toLocation()

        return if (lastSavedLocation != null) {
            val distance = newLocation.toLocation().distanceTo(lastSavedLocation.toLocation())
            Timber.d("Distance is: $distance")
            (distance >= appConfig.minDistanceMetersToLogNewLocationOnMapDuringSignalMeasurement)
        } else {
            Timber.d("Distance is good because no previous point loaded")
            true
        }
    }

    override fun isTheSameLocation(newLocation: DeviceInfo.Location?, lastSavedLocation: DeviceInfo.Location?): Boolean {
        if (newLocation == null) return false

        if (!isLocationFreshAndAccurate(newLocation)) {
            return false
        }

        return if (lastSavedLocation != null) {
            val distance = newLocation.toLocation().distanceTo(lastSavedLocation.toLocation())
            (distance < appConfig.sameLocationDistanceMetersForSignalMeasurement)
        } else {
            false
        }
    }

    override fun isLocationNotTooOld(newLocation: DeviceInfo.Location?): Boolean {
        return if (newLocation == null) false
        else {
            val currentAgeMillis = calculateActualAgeOfLocation(newLocation)
            val isLocationFreshEnough = currentAgeMillis != null && currentAgeMillis <= appConfig.minimalFenceDurationMillisForSignalMeasurement
            Timber.d("Location is fresh enough: $isLocationFreshEnough - $currentAgeMillis")
            isLocationFreshEnough
        }
    }

    override fun isLocationAccuracyPreciseEnough(newLocation: DeviceInfo.Location?): Boolean {
        return if (newLocation == null) false
        else {
            val preciseEnough = newLocation.accuracy != null && (newLocation.accuracy <= appConfig.minLocationAccuracyMetersDuringSignalMeasurement)
            Timber.d("Location accuracy: ${newLocation.accuracy} - $preciseEnough")
            preciseEnough
        }
    }

    private fun calculateActualAgeOfLocation(newLocation: DeviceInfo.Location?): Long? {
        if (newLocation == null) return null

        val currentTime = System.currentTimeMillis()
        val locationAcquireTime = newLocation.time
        val currentLocationAge = currentTime - locationAcquireTime - (newLocation.age ?: 0)
        return currentLocationAge
    }
}