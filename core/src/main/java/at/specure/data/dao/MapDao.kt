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

    @Query("DELETE FROM ${Tables.MAP_MARKER_MEASUREMENTS}")
    fun clear(): Int

    @Query("SELECT * FROM ${Tables.MAP_MARKER_MEASUREMENTS}")
    fun get(): LiveData<List<MarkerMeasurementRecord>>
}