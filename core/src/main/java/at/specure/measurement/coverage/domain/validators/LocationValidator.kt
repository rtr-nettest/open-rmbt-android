package at.specure.measurement.coverage.domain.validators

import at.specure.test.DeviceInfo

interface LocationValidator {

    fun isLocationFreshAndAccurate(newLocation: DeviceInfo.Location?): Boolean

    fun isLocationNotTooOld(newLocation: DeviceInfo.Location?): Boolean

    fun isLocationAccuracyPreciseEnough(newLocation: DeviceInfo.Location?): Boolean

    fun isLocationValidAndDistantEnough(newLocation: DeviceInfo.Location?, lastSavedLocation: DeviceInfo.Location?): Boolean

    fun isTheSameLocation(newLocation: DeviceInfo.Location?, lastSavedLocation: DeviceInfo.Location?): Boolean

}