package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import at.specure.data.Tables
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.QoSResultRecord
import at.specure.data.entity.TestRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord
import timber.log.Timber

// --- Regular-test data retention (see SignalMeasurementDao for the coverage equivalent) ---
// Keep only the newest N tests as quick-access "recent results"; the browsable history lives in the
// HISTORY* overview cache and is untouched. Submissions give up after these limits so nothing retries
// or lingers forever.
const val TEST_RECENT_RESULTS_KEEP_COUNT = 10
const val TEST_SUBMISSION_MAX_RETRY_COUNT = 30
const val TEST_SUBMISSION_GIVE_UP_MILLIS = 7L * 24 * 60 * 60 * 1000 // 7 days

@Dao
interface TestDao {

    @Upsert
    fun insert(test: TestRecord)

    @Upsert
    fun insert(test: TestTelephonyRecord)

    @Upsert
    fun insert(test: TestWlanRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(qosResult: QoSResultRecord)

    @Update
    fun update(test: TestRecord):Int

    @Transaction
    fun deleteAll() {
        val generalTestCount = deleteAllTest()
        Timber.d("DB: Deleted all tests: $generalTestCount")
        val wlanCount = deleteAllWLAN()
        Timber.d("DB: Deleted all wlan tests: $wlanCount")
        val mobileCount = deleteAllTelephony()
        Timber.d("DB: Deleted all mobile tests: $mobileCount")
        if (mobileCount + wlanCount != generalTestCount) {
            Timber.e("DB: Counts of deleted all tests does not match!!!")
        }
    }
    @Query("DELETE FROM ${Tables.TEST}")
    fun deleteAllTest(): Int

    @Query("DELETE FROM ${Tables.TEST_WLAN_RECORD}")
    fun deleteAllWLAN(): Int

    @Query("DELETE FROM ${Tables.TEST_TELEPHONY_RECORD}")
    fun deleteAllTelephony(): Int

    @Transaction
    fun deleteTest(test: TestRecord) {
        val generalTest = deleteTestRecord(test)
        Timber.d("DB: Deleted test: $generalTest")
        val wlanTest = removeWlanRecord(test.uuid)
        Timber.d("DB: Deleted wlan test: $wlanTest")
        val mobileTest = removeTelephonyInfo(test.uuid)
        Timber.d("DB: Deleted mobile test: $mobileTest")
        if (mobileTest + wlanTest != generalTest) {
            Timber.e("Counts of deleted all tests does not match!!!")
        }
    }
    @Delete
    fun deleteTestRecord(test: TestRecord): Int

    @Query("SELECT * FROM ${Tables.TEST} WHERE uuid == :uuid")
    fun get(uuid: String): TestRecord?

    @Query("SELECT * FROM ${Tables.QOS_RESULT} WHERE uuid == :uuid")
    fun getQoSRecord(uuid: String): QoSResultRecord?

    @Query("SELECT * FROM ${Tables.TEST_TELEPHONY_RECORD} WHERE testUUID == :uuid")
    fun getTelephonyRecord(uuid: String): TestTelephonyRecord?

    @Query("DELETE FROM ${Tables.TEST_TELEPHONY_RECORD} WHERE testUUID=:uuid")
    fun removeTelephonyInfo(uuid: String): Int

    @Query("SELECT * from ${Tables.TEST_WLAN_RECORD} WHERE testUUID == :uuid")
    fun getWlanRecord(uuid: String): TestWlanRecord?

    @Query("DELETE FROM ${Tables.TEST_WLAN_RECORD} WHERE testUUID=:uuid")
    fun removeWlanRecord(uuid: String): Int

    @Query("SELECT submissionRetryCount FROM ${Tables.TEST} WHERE uuid == :uuid")
    fun getSubmissionsRetryCount(uuid: String): Int?

    @Query("UPDATE ${Tables.TEST} SET submissionRetryCount = submissionRetryCount + 1 WHERE uuid == :uuid")
    fun updateSubmissionsRetryCounter(uuid: String): Int

    @Query("UPDATE ${Tables.TEST} SET isSubmitted = 1 WHERE uuid == :uuid")
    fun updateTestIsSubmitted(uuid: String): Int

    @Query("UPDATE ${Tables.QOS_RESULT} SET isSubmitted = 1 WHERE uuid == :uuid")
    fun updateQoSTestIsSubmitted(uuid: String): Int

    @Query("UPDATE ${Tables.TEST} SET lastQoSStatus=:status WHERE uuid == :uuid")
    fun updateQoSTestStatus(uuid: String, status: Int?): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveLoopModeRecord(loopModeRecord: LoopModeRecord)

    @Update
    fun updateLoopModeRecord(loopModeRecord: LoopModeRecord): Int

    @Query("SELECT * FROM ${Tables.LOOP_MODE} WHERE localUuid == :localUuid")
    fun getLoopModeRecord(localUuid: String): LiveData<LoopModeRecord?>

    // ===== Test data retention sweep (deletes measurement detail; keeps recent-results summary) =====

    @Query(
        """
        DELETE FROM ${Tables.SPEED}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_speed(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.TEST_GRAPH_ITEM}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_test_graph_item(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.PING}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_ping(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.CELL_INFO}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_cell_info(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.SIGNAL}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_signal(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.GEO_LOCATION}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_geo_location(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.CELL_LOCATION}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_cell_location(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.PERMISSIONS_STATUS}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_permissions_status(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.CAPABILITIES}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_capabilities(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.TEST_TELEPHONY_RECORD}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_test_telephony_record(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.TEST_WLAN_RECORD}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_test_wlan_record(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.TEST_RESULT_GRAPH_ITEM}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_test_result_graph_item(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.TEST_RESULT_DETAILS}
        WHERE testUUID IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND testUUID NOT IN (SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore)
    """
    )
    fun deletePruned_test_result_details(maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.TEST_RESULT}
        WHERE uuid IN (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 OR isSubmitted = 1)
          AND uuid NOT IN (
                SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore
                UNION
                SELECT uuid FROM (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 ORDER BY testTimeMillis DESC LIMIT :keepCount)
              )
    """
    )
    fun deletePrunedTestResults(keepCount: Int, maxRetry: Int, giveUpBefore: Long): Int

    @Query(
        """
        DELETE FROM ${Tables.TEST}
        WHERE (testTimeMillis > 0 OR isSubmitted = 1)
          AND uuid NOT IN (
                SELECT uuid FROM ${Tables.TEST} WHERE isSubmitted = 0 AND submissionRetryCount < :maxRetry AND testTimeMillis > :giveUpBefore
                UNION
                SELECT uuid FROM (SELECT uuid FROM ${Tables.TEST} WHERE testTimeMillis > 0 ORDER BY testTimeMillis DESC LIMIT :keepCount)
              )
    """
    )
    fun deletePrunedTests(keepCount: Int, maxRetry: Int, giveUpBefore: Long): Int

    /**
     * Prunes regular-test data: deletes all measurement detail (payload + result caches) for tests no
     * longer awaiting submission (submitted, or given up after the retry limits), then drops whole
     * tests beyond the newest [keepCount]. In-progress tests (never submitted and not yet timestamped),
     * coverage data and the HISTORY* overview are never touched.
     */
    @Transaction
    fun pruneTestData(
        nowMillis: Long,
        keepCount: Int = TEST_RECENT_RESULTS_KEEP_COUNT,
        maxRetry: Int = TEST_SUBMISSION_MAX_RETRY_COUNT,
        giveUpMillis: Long = TEST_SUBMISSION_GIVE_UP_MILLIS
    ) {
        val giveUpBefore = nowMillis - giveUpMillis
        deletePruned_speed(maxRetry, giveUpBefore)
        deletePruned_test_graph_item(maxRetry, giveUpBefore)
        deletePruned_ping(maxRetry, giveUpBefore)
        deletePruned_cell_info(maxRetry, giveUpBefore)
        deletePruned_signal(maxRetry, giveUpBefore)
        deletePruned_geo_location(maxRetry, giveUpBefore)
        deletePruned_cell_location(maxRetry, giveUpBefore)
        deletePruned_permissions_status(maxRetry, giveUpBefore)
        deletePruned_capabilities(maxRetry, giveUpBefore)
        deletePruned_test_telephony_record(maxRetry, giveUpBefore)
        deletePruned_test_wlan_record(maxRetry, giveUpBefore)
        deletePruned_test_result_graph_item(maxRetry, giveUpBefore)
        deletePruned_test_result_details(maxRetry, giveUpBefore)
        deletePrunedTestResults(keepCount, maxRetry, giveUpBefore)
        deletePrunedTests(keepCount, maxRetry, giveUpBefore)
    }
}
