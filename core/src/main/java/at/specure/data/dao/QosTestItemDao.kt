package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import at.specure.data.Tables
import at.specure.data.entity.QosTestItemRecord
import at.specure.result.QoSCategory

@Dao
abstract class QosTestItemDao {

    @Query("SELECT * from ${Tables.QOS_TEST_RESULT_ITEM} WHERE testUUID == :testUUID AND category == :category ORDER BY testNumber")
    abstract fun get(testUUID: String, category: QoSCategory): LiveData<List<QosTestItemRecord>>

    @Upsert
    abstract fun insert(qosTestItems: List<QosTestItemRecord>)

    @Query("DELETE FROM ${Tables.QOS_TEST_RESULT_ITEM} WHERE testUUID == :testUUID")
    abstract fun clearQosItemsForTest(testUUID: String)

    @Transaction
    open fun clearQosItemsInsert(qosCategory: List<QosTestItemRecord>) {
        if (qosCategory.isNotEmpty()) {
            clearQosItemsForTest(qosCategory.first().testUUID)
            insert(qosCategory)
        }
    }
}