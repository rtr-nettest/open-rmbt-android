package at.specure.database.dao

import androidx.lifecycle.MutableLiveData
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.GeoLocations

interface GeoLocationDao {

    @Query("SELECT * from ${Tables.GEO_LOCATION} WHERE testUUID == :testUUID")
    fun getGeoLocationsForTest(testUUID: String): MutableLiveData<List<GeoLocations>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(geoLocations: GeoLocations)
}