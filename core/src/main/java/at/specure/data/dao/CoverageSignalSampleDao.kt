package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.CoverageSignalSampleRecord

@Dao
interface CoverageSignalSampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(record: CoverageSignalSampleRecord)

    /**
     * Samples of one session whose time is within [fromMillis, toMillis], ordered by time. One sample
     * just before the range is intentionally left to the caller to fetch separately if a leading
     * segment is needed; here we keep it simple and return only the in-range points.
     */
    @Query(
        "SELECT * FROM ${Tables.COVERAGE_SIGNAL_SAMPLE} " +
            "WHERE sessionId = :sessionId AND timeMillis BETWEEN :fromMillis AND :toMillis " +
            "ORDER BY timeMillis ASC"
    )
    fun getRange(sessionId: String, fromMillis: Long, toMillis: Long): List<CoverageSignalSampleRecord>

    @Query("SELECT MIN(timeMillis) FROM ${Tables.COVERAGE_SIGNAL_SAMPLE} WHERE sessionId = :sessionId")
    fun getMinTime(sessionId: String): Long?

    @Query("SELECT MAX(timeMillis) FROM ${Tables.COVERAGE_SIGNAL_SAMPLE} WHERE sessionId = :sessionId")
    fun getMaxTime(sessionId: String): Long?

    @Query("DELETE FROM ${Tables.COVERAGE_SIGNAL_SAMPLE} WHERE sessionId = :sessionId")
    fun deleteForSession(sessionId: String)
}
