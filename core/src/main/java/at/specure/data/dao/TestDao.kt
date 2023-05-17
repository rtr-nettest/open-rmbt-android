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
}