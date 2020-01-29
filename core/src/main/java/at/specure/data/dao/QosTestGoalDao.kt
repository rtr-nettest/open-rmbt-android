package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.QosTestGoalRecord

@Dao
abstract class QosTestGoalDao {

    @Query("SELECT * from ${Tables.QOS_TEST_GOAL} WHERE qosTestId == :qosTestId AND testUUID == :testUUID")
    abstract fun get(testUUID: String, qosTestId: Long): LiveData<List<QosTestGoalRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(qosTestItems: List<QosTestGoalRecord>)

    @Query("DELETE FROM ${Tables.QOS_TEST_GOAL} WHERE testUUID == :testUUID")
    abstract fun clearQosGoalsForTest(testUUID: String)

    @Transaction
    open fun clearQosGoalsInsert(qosItemGoal: List<QosTestGoalRecord>) {
        if (qosItemGoal.isNotEmpty()) {
            clearQosGoalsForTest(qosItemGoal.first().testUUID)
            insert(qosItemGoal)
        }
    }
}