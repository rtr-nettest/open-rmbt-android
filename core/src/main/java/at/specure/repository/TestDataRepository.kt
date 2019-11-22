package at.specure.repository

import at.specure.location.LocationInfo

interface TestDataRepository {

    fun saveGeoLocation(testUUID: String, location: LocationInfo)
}