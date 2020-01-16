package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.MarkerMeasurementRecord

@Dao
interface MapDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(items: List<MarkerMeasurementRecord>)

    @Query("SELECT * FROM ${Tables.MAP_MARKER_MEASUREMENTS} WHERE abs(latitude - :latitude) < 0.001 AND abs(longitude - :longitude) < 0.001")
    fun get(latitude: Double?, longitude: Double?): LiveData<List<MarkerMeasurementRecord>>
}