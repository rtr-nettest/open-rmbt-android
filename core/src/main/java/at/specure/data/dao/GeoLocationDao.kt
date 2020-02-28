package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.GeoLocationRecord

@Dao
interface GeoLocationDao {

    @Query("SELECT * from ${Tables.GEO_LOCATION} WHERE testUUID == :testUUID")
    fun get(testUUID: String): List<GeoLocationRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(geoLocations: GeoLocationRecord): Long

    @Query("DELETE FROM ${Tables.GEO_LOCATION} WHERE testUUID=:testUUID")
    fun remove(testUUID: String)
}