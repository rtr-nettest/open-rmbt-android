package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.GeoLocationRecord

@Dao
interface GeoLocationDao {

    @Query("SELECT * from ${Tables.GEO_LOCATION} WHERE testUUID == :testUUID")
    fun getGeoLocationsForTest(testUUID: String): List<GeoLocationRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(geoLocations: GeoLocationRecord): Long
}