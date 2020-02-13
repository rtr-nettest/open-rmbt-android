package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import at.specure.data.Tables
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.QoSResultRecord
import at.specure.data.entity.TestRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord

@Dao
interface TestDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(test: TestRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(test: TestTelephonyRecord)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(test: TestWlanRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(qosResult: QoSResultRecord)

    @Update
    fun update(test: TestRecord)

    @Query("DELETE FROM ${Tables.TEST}")
    fun deleteAll(): Int

    @Delete
    fun deleteTest(test: TestRecord): Int

    @Query("SELECT * from ${Tables.TEST} WHERE uuid == :uuid")
    fun get(uuid: String): TestRecord?

    @Query("SELECT * from ${Tables.QOS_RESULT} WHERE uuid == :uuid")
    fun getQoSRecord(uuid: String): QoSResultRecord?

    @Query("SELECT * from ${Tables.TEST_TELEPHONY_RECORD} WHERE testUUID == :uuid")
    fun getTelephonyRecord(uuid: String): TestTelephonyRecord?

    @Query("SELECT * from ${Tables.TEST_WLAN_RECORD} WHERE testUUID == :uuid")
    fun getWlanRecord(uuid: String): TestWlanRecord?

    @Query("SELECT submissionRetryCount FROM ${Tables.TEST} WHERE uuid == :uuid")
    fun getSubmissionsRetryCount(uuid: String): Int?

    @Query("UPDATE ${Tables.TEST} SET submissionRetryCount = submissionRetryCount + 1 WHERE uuid == :uuid")
    fun updateSubmissionsRetryCounter(uuid: String)

    @Query("UPDATE ${Tables.TEST} SET isSubmitted = 1 WHERE uuid == :uuid")
    fun updateTestIsSubmitted(uuid: String)

    @Query("UPDATE ${Tables.QOS_RESULT} SET isSubmitted = 1 WHERE uuid == :uuid")
    fun updateQoSTestIsSubmitted(uuid: String)

    @Query("UPDATE ${Tables.TEST} SET lastQoSStatus=:status WHERE uuid == :uuid")
    fun updateQoSTestStatus(uuid: String, status: Int?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveLoopModeRecord(loopModeRecord: LoopModeRecord)

    @Update
    fun updateLoopModeRecord(loopModeRecord: LoopModeRecord)

    @Query("SELECT * FROM ${Tables.LOOP_MODE} WHERE uuid == :uuid")
    fun getLoopModeRecord(uuid: String): LiveData<LoopModeRecord?>
}