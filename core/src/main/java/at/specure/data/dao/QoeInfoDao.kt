package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.QoeInfoRecord

@Dao
abstract class QoeInfoDao {

    @Query("SELECT * from ${Tables.QOE} WHERE testUUID == :testUUID")
    abstract fun get(testUUID: String): LiveData<List<QoeInfoRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(qoeInfo: List<QoeInfoRecord>)

    @Query("DELETE FROM ${Tables.QOE} WHERE testUUID == :testUUID")
    abstract fun clear(testUUID: String)

    @Transaction
    open fun clearInsert(qoe: List<QoeInfoRecord>) {
        if (qoe.isNotEmpty()) {
            clear(qoe.first().testUUID)
            insert(qoe)
        }
    }
}