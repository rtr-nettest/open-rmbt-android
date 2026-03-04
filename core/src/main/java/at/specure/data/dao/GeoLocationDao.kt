package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.GeoLocationRecord

@Dao
interface GeoLocationDao {

    @Query("SELECT * from ${Tables.GEO_LOCATION} WHERE ((testUUID IS :testUUID) AND (signalChunkId IS :signalChunkId))")
    fun get(testUUID: String?, signalChunkId: String?): List<GeoLocationRecord>

    @Query("SELECT * from ${Tables.GEO_LOCATION} WHERE (testUUID IS :testUUID) ORDER BY timestampMillis DESC LIMIT 1")
    fun getLatestLocationForTest(testUUID: String?): GeoLocationRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(geoLocations: GeoLocationRecord): Long

    @Query("DELETE FROM ${Tables.GEO_LOCATION} WHERE ((testUUID IS :testUUID) AND (signalChunkId IS :signalChunkId))")
    fun remove(testUUID: String?, signalChunkId: String?)

    @Query("DELETE FROM ${Tables.GEO_LOCATION} WHERE (testUUID IS :testUUID)")
    fun removeForTest(testUUID: String)

    @Query("SELECT COUNT(*) FROM ${Tables.GEO_LOCATION} WHERE testUUID IS :localMeasurementId")
    fun getCountForCoverageMeasurement(localMeasurementId: String): Int
}