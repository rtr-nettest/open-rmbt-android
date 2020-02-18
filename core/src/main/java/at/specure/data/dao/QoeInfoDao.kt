package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.QoeInfoRecord
import at.specure.result.QoECategory

@Dao
abstract class QoeInfoDao {

    @Query("SELECT * from ${Tables.QOE} WHERE testUUID == :testUUID ORDER BY priority DESC")
    abstract fun get(testUUID: String): LiveData<List<QoeInfoRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(qoeInfo: List<QoeInfoRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(qoeInfo: QoeInfoRecord)

    @Query("DELETE FROM ${Tables.QOE} WHERE testUUID == :testUUID")
    abstract fun clear(testUUID: String)

    @Query("DELETE FROM ${Tables.QOE} WHERE testUUID == :testUUID AND category == :category")
    abstract fun clearQoeCategory(testUUID: String, category: QoECategory)

    @Transaction
    open fun clearInsert(qoe: List<QoeInfoRecord>) {
        if (qoe.isNotEmpty()) {
            clear(qoe.first().testUUID)
            insert(qoe)
        }
    }

    @Transaction
    open fun clearQoSInsert(qoe: QoeInfoRecord) {
        clearQoeCategory(qoe.testUUID, qoe.category)
        insert(qoe)
    }
}