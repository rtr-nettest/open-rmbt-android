package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import at.specure.data.Tables
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.data.entity.SignalRecord

const val COVERAGE_MEASUREMENT_SUBMISSION_MAX_RETRY_COUNT = 3

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

    @Query("SELECT * FROM ${Tables.COVERAGE_MEASUREMENT_FENCE} WHERE sessionId=:sessionId ORDER BY sequenceNumber ASC")
    fun getCoverageMeasurementFences(sessionId: String): LiveData<List<CoverageMeasurementFenceRecord>>

    @Query("SELECT * FROM ${Tables.COVERAGE_MEASUREMENT_FENCE} WHERE sessionId=:sessionId ORDER BY sequenceNumber ASC")
    fun getCoverageMeasurementFencesList(sessionId: String): List<CoverageMeasurementFenceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSignalMeasurementPoint(point: CoverageMeasurementFenceRecord)

    @Upsert()
    fun saveDedicatedSignalMeasurementSession(session: CoverageMeasurementSession)

    // TODO: Change to return list according to loop id
    @Query("SELECT * FROM ${Tables.COVERAGE_MEASUREMENT_SESSION} WHERE localLoopId=:loopId ORDER BY sequenceNumber ASC")
    fun getCoverageMeasurementSessionsForLoopId(loopId: String): CoverageMeasurementSession?

    @Query("SELECT * FROM ${Tables.COVERAGE_MEASUREMENT_SESSION} WHERE localMeasurementId=:measurementId LIMIT 1")
    fun getCoverageMeasurementSessionForMeasurementId(measurementId: String): CoverageMeasurementSession?

    @Query("""
        SELECT * FROM ${Tables.COVERAGE_MEASUREMENT_SESSION} 
        WHERE retryCount < $COVERAGE_MEASUREMENT_SUBMISSION_MAX_RETRY_COUNT 
          AND (startMeasurementResponseReceivedMillis + (maxCoverageMeasurementSeconds * 1000)) < :currentTimeMillis 
          AND serverMeasurementId IS NOT NULL 
          AND synced = 0
    """)
    fun getCoverageMeasurementsForRetrySend(currentTimeMillis: Long = System.currentTimeMillis()): List<CoverageMeasurementSession>

    @Query("SELECT * FROM ${Tables.SIGNAL} WHERE signalMeasurementPointId=:id LIMIT 1")
    suspend fun getSignalRecord(id: String): SignalRecord?

    @Query(
        """
        UPDATE ${Tables.COVERAGE_MEASUREMENT_SESSION}
        SET retryCount = retryCount + 1
        WHERE localMeasurementId = :sessionId
    """
    )
    suspend fun incrementRetryCountForSession(sessionId: String)

    @Query(
        """
        UPDATE ${Tables.COVERAGE_MEASUREMENT_SESSION}
        SET synced = 1
        WHERE localMeasurementId = :sessionId
    """
    )
    suspend fun markSessionAsSynced(sessionId: String)

    @Transaction
    suspend fun deleteSyncedOrFailedSessions(maxRetryCount: Int = COVERAGE_MEASUREMENT_SUBMISSION_MAX_RETRY_COUNT) {
        deleteFencesForDeletableSessions(maxRetryCount)
        deleteDeletableSessions(maxRetryCount)
    }

    @Query("""
        DELETE FROM ${Tables.COVERAGE_MEASUREMENT_FENCE}
        WHERE sessionId IN (
            SELECT sessionId FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
            WHERE synced = 1 OR retryCount >= :maxRetryCount
        )
    """)
    suspend fun deleteFencesForDeletableSessions(maxRetryCount: Int)

    @Query("""
        DELETE FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
        WHERE synced = 1 OR retryCount >= :maxRetryCount
    """)
    suspend fun deleteDeletableSessions(maxRetryCount: Int = COVERAGE_MEASUREMENT_SUBMISSION_MAX_RETRY_COUNT)

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