package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import at.specure.data.Tables
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.entity.SignalRecord

@Dao
interface SignalMeasurementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSignalMeasurementRecord(record: SignalMeasurementRecord)

    @Update
    fun updateSignalMeasurementRecord(record: SignalMeasurementRecord): Int

    @Query("SELECT * FROM ${Tables.SIGNAL_MEASUREMENT} WHERE id=:id")
    fun getSignalMeasurementRecord(id: String): SignalMeasurementRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSignalMeasurementChunk(chunk: SignalMeasurementChunk)

    @Query("SELECT * FROM ${Tables.SIGNAL_MEASUREMENT_CHUNK} WHERE id=:chunkId")
    fun getSignalMeasurementChunk(chunkId: String): SignalMeasurementChunk?

    @Query("SELECT * FROM ${Tables.SIGNAL_MEASUREMENT_FENCE} WHERE sessionId=:sessionId ORDER BY sequenceNumber ASC")
    fun getSignalMeasurementPoints(sessionId: String): LiveData<List<CoverageMeasurementFenceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSignalMeasurementPoint(point: CoverageMeasurementFenceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveDedicatedSignalMeasurementSession(session: CoverageMeasurementSession)

    @Query("SELECT * FROM ${Tables.SIGNAL_MEASUREMENT_SESSION} WHERE sessionId=:sessionId LIMIT 1")
    fun getDedicatedSignalMeasurementSession(sessionId: String): CoverageMeasurementSession?

    @Query("SELECT * FROM ${Tables.SIGNAL_MEASUREMENT_SESSION} WHERE measurementId=:measurementId LIMIT 1")
    fun getDedicatedSignalMeasurementSessionForMeasurementId(measurementId: String): CoverageMeasurementSession?

    @Query("SELECT * FROM ${Tables.SIGNAL} WHERE signalMeasurementPointId=:id LIMIT 1")
    suspend fun getSignalRecord(id: String): SignalRecord?

    suspend fun getSignalRecordNullable(id: String?): SignalRecord? {
        return if (id == null) {
            null
        } else {
            getSignalRecord(id)
        }
    }

    @Update
    fun updateSignalMeasurementPoint(updatedPoint: CoverageMeasurementFenceRecord)
}