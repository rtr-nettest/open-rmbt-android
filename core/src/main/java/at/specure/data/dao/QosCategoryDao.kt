package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.QosCategoryRecord
import at.specure.result.QoSCategory

@Dao
abstract class QosCategoryDao {

    @Query("SELECT * from ${Tables.QOS_CATEGORY} WHERE testUUID == :testUUID")
    abstract fun get(testUUID: String): LiveData<List<QosCategoryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(qosCategory: QosCategoryRecord)

    @Query("DELETE FROM ${Tables.QOS_CATEGORY} WHERE testUUID == :testUUID AND category == :category")
    abstract fun clearQosCategory(testUUID: String, category: QoSCategory)

    @Transaction
    open fun clearQoSInsert(qosCategory: QosCategoryRecord) {
        clearQosCategory(qosCategory.testUUID, qosCategory.category)
        insert(qosCategory)
    }
}