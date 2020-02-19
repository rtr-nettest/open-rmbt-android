package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import at.specure.data.Tables
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementInfo
import at.specure.data.entity.SignalMeasurementRecord

@Dao
interface SignalMeasurementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSignalMeasurementRecord(record: SignalMeasurementRecord)

    @Update
    fun updateSignalMeasurementRecord(record: SignalMeasurementRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSignalMeasurementInfo(info: SignalMeasurementInfo)

    @Query("SELECT * FROM ${Tables.SIGNAL_MEASUREMENT} WHERE id=:id")
    fun getSignalMeasurementRecord(id: String): SignalMeasurementRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSignalMeasurementChunk(chunk: SignalMeasurementChunk)

    @Query("SELECT * FROM ${Tables.SIGNAL_MEASUREMENT_CHUNK} WHERE id=:chunkId")
    fun getSignalMeasurementChunk(chunkId: String): SignalMeasurementChunk?

    @Query("SELECT * FROM ${Tables.SIGNAL_MEASUREMENT_INFO} WHERE measurementId=:measurementId")
    fun getSignalMeasurementInfo(measurementId: String): SignalMeasurementInfo?
}