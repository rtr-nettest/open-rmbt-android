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
import at.specure.measurement.coverage.domain.models.MobileSignalTechnologyTimestamp

const val COVERAGE_MEASUREMENT_SUBMISSION_MAX_RETRY_COUNT = 3

// Coverage sessions that cannot be submitted (no fences) or are older than this are purged so
// unsendable historic data can never accumulate.
const val COVERAGE_UNSENT_SESSION_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000 // 7 days

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

    @Query(
        """
        SELECT fence.* 
        FROM ${Tables.COVERAGE_MEASUREMENT_FENCE} AS fence
        INNER JOIN ${Tables.COVERAGE_MEASUREMENT_SESSION} AS session
        ON fence.sessionId = session.localMeasurementId
        WHERE session.localLoopId = :sessionLoopId
        ORDER BY fence.entryTimestampMillis DESC
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
    """
    )
    fun getLastFencesListForSessionLoop(
        sessionLoopId: String,
        limit: Int? = null
    ): List<CoverageMeasurementFenceRecord>

    @Query(
        """
        SELECT fence.* 
        FROM ${Tables.COVERAGE_MEASUREMENT_FENCE} AS fence
        INNER JOIN ${Tables.COVERAGE_MEASUREMENT_SESSION} AS session
        ON fence.sessionId = session.localMeasurementId
        WHERE session.localLoopId = :sessionLoopId
        ORDER BY fence.entryTimestampMillis ASC
    """
    )
    fun getFencesListForSessionLoop(sessionLoopId: String): List<CoverageMeasurementFenceRecord>

    @Query(
        """
        SELECT fence.* 
        FROM ${Tables.COVERAGE_MEASUREMENT_FENCE} AS fence
        INNER JOIN ${Tables.COVERAGE_MEASUREMENT_SESSION} AS session
        ON fence.sessionId = session.localMeasurementId
        WHERE session.localLoopId = :sessionLoopId
        ORDER BY fence.entryTimestampMillis ASC
    """
    )
    fun getFencesLiveDataForSessionLoop(sessionLoopId: String): LiveData<List<CoverageMeasurementFenceRecord>>

    @Upsert
    fun upsertSignalMeasurementPoint(point: CoverageMeasurementFenceRecord)

    @Query(
        """
        SELECT COALESCE(MAX(sequenceNumber), -1) 
        FROM ${Tables.COVERAGE_MEASUREMENT_FENCE} 
        WHERE sessionId = :sessionId
    """
    )
    suspend fun getMaxSequence(sessionId: String): Int

    @Transaction
    open suspend fun insertFenceWithNextSequence(
        point: CoverageMeasurementFenceRecord
    ) {
        val nextSeq = getMaxSequence(point.sessionId) + 1

        val updatedSequencePoint = point.copy(
            sequenceNumber = nextSeq
        )
        upsertSignalMeasurementPoint(updatedSequencePoint)
    }

    @Transaction
    suspend fun insertFenceWithNextSequence(
        point: CoverageMeasurementFenceRecord,
        leaveTimestampMillis: Long,
        avgPingMillis: Double?,
        lastFenceMinTechSignal: MobileSignalTechnologyTimestamp?,
    ) {
        val nextSeq = getMaxSequence(point.sessionId) + 1
        val updatedSequencePoint = point.copy(
            sequenceNumber = nextSeq
        )
        upsertSignalMeasurementPoint(updatedSequencePoint)
    }

    @Transaction
    suspend fun updateLastPointForSession(
        sessionId: String,
        leaveTimestampMillis: Long,
        avgPingMillis: Double?,
        lastFenceMinTechSignal: MobileSignalTechnologyTimestamp?,
    ) {
        val session = getCoverageMeasurementSessionForMeasurementId(sessionId)
        val lastPoint = session?.localLoopId?.let {
            getLastFencesListForSessionLoop(sessionLoopId = session.localLoopId, 1).firstOrNull()
        }
        lastPoint?.let { lastFence ->
            val updatedFence = lastFence.copy(
                leaveTimestampMillis = leaveTimestampMillis,
                avgPingMillis = avgPingMillis,
                technologyId = lastFenceMinTechSignal?.type?.intValue,
                signalStrength = lastFenceMinTechSignal?.signalValueDbm
            )
            updateSignalMeasurementPoint(updatedFence)
        }
    }

    @Upsert()
    fun saveDedicatedSignalMeasurementSession(session: CoverageMeasurementSession)

    // TODO: Change to return list according to loop id
    @Query("SELECT * FROM ${Tables.COVERAGE_MEASUREMENT_SESSION} WHERE localLoopId=:loopId ORDER BY sequenceNumber ASC")
    fun getCoverageMeasurementSessionsForLoopId(loopId: String): CoverageMeasurementSession?

    @Query("SELECT * FROM ${Tables.COVERAGE_MEASUREMENT_SESSION} WHERE localMeasurementId=:measurementId LIMIT 1")
    fun getCoverageMeasurementSessionForMeasurementId(measurementId: String): CoverageMeasurementSession?

    @Query(
        """
        SELECT * FROM ${Tables.COVERAGE_MEASUREMENT_SESSION} 
        WHERE retryCount < $COVERAGE_MEASUREMENT_SUBMISSION_MAX_RETRY_COUNT
          AND serverMeasurementId IS NOT NULL 
          AND synced = 0
      ORDER BY 
        startTimeLoopMillis DESC,
        startTimeMeasurementMillis ASC
    """
    )
    fun getCoverageMeasurementsForRetrySend(): List<CoverageMeasurementSession>

    @Query(
        """
        SELECT * FROM ${Tables.COVERAGE_MEASUREMENT_SESSION} 
        WHERE retryCount < $COVERAGE_MEASUREMENT_SUBMISSION_MAX_RETRY_COUNT 
          AND (startMeasurementResponseReceivedMillis + (maxCoverageMeasurementSeconds * 1000)) < :currentTimeMillis 
          AND serverMeasurementId IS NULL 
          AND synced = 0
      ORDER BY 
        startTimeLoopMillis DESC,
        startTimeMeasurementMillis ASC
    """
    )
    fun getNotRegisteredCoverageMeasurements(currentTimeMillis: Long = System.currentTimeMillis()): List<CoverageMeasurementSession>

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

    /**
     * Retires (marks synced so [deleteSyncedOrFailedSessions] purges them) coverage sessions that can
     * never be submitted: their measurement window has ended AND they either have no fences to send
     * or are older than [staleBeforeMillis]. The window check guarantees the currently running
     * measurement is never touched; [protectedLoopId] additionally spares every session of the loop
     * whose result is still being shown to the user (its submitted segments are needed for the map).
     */
    @Query(
        """
        UPDATE ${Tables.COVERAGE_MEASUREMENT_SESSION}
        SET synced = 1
        WHERE synced = 0
          AND maxCoverageMeasurementSeconds IS NOT NULL
          AND (startMeasurementResponseReceivedMillis + maxCoverageMeasurementSeconds * 1000) < :nowMillis
          AND (:protectedLoopId IS NULL OR localLoopId <> :protectedLoopId)
          AND (
                localMeasurementId NOT IN (SELECT DISTINCT sessionId FROM ${Tables.COVERAGE_MEASUREMENT_FENCE})
                OR startMeasurementResponseReceivedMillis < :staleBeforeMillis
              )
    """
    )
    suspend fun retireUnsendableOrStaleCoverageSessions(nowMillis: Long, staleBeforeMillis: Long, protectedLoopId: String?)

    /**
     * Purges coverage sessions that are synced or have exhausted their retries.
     *
     * Two levels of purging:
     *  - The heavy, per-fence submission payload (signal samples, cell info, geolocations, ...) is
     *    dropped for EVERY submitted/failed session, including those of [protectedLoopId]. Once a
     *    session has been submitted this data is no longer needed - neither for a resubmission nor
     *    for the map - so keeping it only inflates the DB and the heap during queries.
     *  - The lightweight fence summaries and the session rows themselves are kept for
     *    [protectedLoopId] (the loop currently running or still shown on the result page) because the
     *    map is drawn from them; they are only deleted once the loop has become historic. Pass null
     *    to purge every deletable loop entirely.
     */
    @Transaction
    suspend fun deleteSyncedOrFailedSessions(
        maxRetryCount: Int = COVERAGE_MEASUREMENT_SUBMISSION_MAX_RETRY_COUNT,
        protectedLoopId: String?
    ) {
        // Heavy submission payload: drop for all deletable sessions (null = ignore the loop guard),
        // so already-submitted fence information cannot pile up even for the current loop.
        deletePermissionsStatusForDeletableCoverageSessions(maxRetryCount, null)
        deleteCapabilitiesForDeletableCoverageSessions(maxRetryCount, null)
        deleteCellLocationsForDeletableCoverageSessions(maxRetryCount, null)
        deleteCellInfosForDeletableCoverageSessions(maxRetryCount, null)
        deleteGeolocationsForDeletableCoverageSessions(maxRetryCount, null)
        deleteTelephonyRecordsForDeletableCoverageSessions(maxRetryCount, null)
        deleteSignalsForDeletableCoverageSessions(maxRetryCount, null)
        // Map data: keep fences + sessions of the protected loop, delete only historic loops.
        deleteFencesForDeletableSessions(maxRetryCount, protectedLoopId)
        deleteDeletableSessions(maxRetryCount, protectedLoopId)
    }

    @Query(
        """
        DELETE FROM ${Tables.PERMISSIONS_STATUS}
            WHERE testUUID IN (
            SELECT localMeasurementId FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
            WHERE (synced = 1 OR retryCount >= :maxRetryCount)
              AND (:protectedLoopId IS NULL OR localLoopId <> :protectedLoopId)
        )
    """
    )
    suspend fun deletePermissionsStatusForDeletableCoverageSessions(maxRetryCount: Int, protectedLoopId: String?)

    @Query(
        """
        DELETE FROM ${Tables.CAPABILITIES}
            WHERE testUUID IN (
            SELECT localMeasurementId FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
            WHERE (synced = 1 OR retryCount >= :maxRetryCount)
              AND (:protectedLoopId IS NULL OR localLoopId <> :protectedLoopId)
        )
    """
    )
    suspend fun deleteCapabilitiesForDeletableCoverageSessions(maxRetryCount: Int, protectedLoopId: String?)

    @Query(
        """
        DELETE FROM ${Tables.CELL_LOCATION}
        WHERE testUUID IN (
            SELECT localMeasurementId FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
            WHERE (synced = 1 OR retryCount >= :maxRetryCount)
              AND (:protectedLoopId IS NULL OR localLoopId <> :protectedLoopId)
        )
    """
    )
    suspend fun deleteCellLocationsForDeletableCoverageSessions(maxRetryCount: Int, protectedLoopId: String?)

    @Query(
        """
        DELETE FROM ${Tables.CELL_INFO}
        WHERE testUUID IN (
            SELECT localMeasurementId FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
            WHERE (synced = 1 OR retryCount >= :maxRetryCount)
              AND (:protectedLoopId IS NULL OR localLoopId <> :protectedLoopId)
        )
    """
    )
    suspend fun deleteCellInfosForDeletableCoverageSessions(maxRetryCount: Int, protectedLoopId: String?)

    @Query(
        """
        DELETE FROM ${Tables.GEO_LOCATION}
        WHERE testUUID IN (
            SELECT localMeasurementId FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
            WHERE (synced = 1 OR retryCount >= :maxRetryCount)
              AND (:protectedLoopId IS NULL OR localLoopId <> :protectedLoopId)
        )
    """
    )
    suspend fun deleteGeolocationsForDeletableCoverageSessions(maxRetryCount: Int, protectedLoopId: String?)

    @Query(
        """
        DELETE FROM ${Tables.TEST_TELEPHONY_RECORD}
        WHERE testUUID IN (
            SELECT localMeasurementId FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
            WHERE (synced = 1 OR retryCount >= :maxRetryCount)
              AND (:protectedLoopId IS NULL OR localLoopId <> :protectedLoopId)
        )
    """
    )
    suspend fun deleteTelephonyRecordsForDeletableCoverageSessions(maxRetryCount: Int, protectedLoopId: String?)

    @Query(
        """
        DELETE FROM ${Tables.SIGNAL}
        WHERE testUUID IN (
            SELECT localMeasurementId FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
            WHERE (synced = 1 OR retryCount >= :maxRetryCount)
              AND (:protectedLoopId IS NULL OR localLoopId <> :protectedLoopId)
        )
    """
    )
    suspend fun deleteSignalsForDeletableCoverageSessions(maxRetryCount: Int, protectedLoopId: String?)

    @Query(
        """
        DELETE FROM ${Tables.COVERAGE_MEASUREMENT_FENCE}
        WHERE sessionId IN (
            SELECT localMeasurementId FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
            WHERE (synced = 1 OR retryCount >= :maxRetryCount)
              AND (:protectedLoopId IS NULL OR localLoopId <> :protectedLoopId)
        )
    """
    )
    suspend fun deleteFencesForDeletableSessions(maxRetryCount: Int, protectedLoopId: String?)

    @Query(
        """
        DELETE FROM ${Tables.COVERAGE_MEASUREMENT_SESSION}
        WHERE (synced = 1 OR retryCount >= :maxRetryCount)
          AND (:protectedLoopId IS NULL OR localLoopId <> :protectedLoopId)
    """
    )
    suspend fun deleteDeletableSessions(maxRetryCount: Int = COVERAGE_MEASUREMENT_SUBMISSION_MAX_RETRY_COUNT, protectedLoopId: String?)

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