package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.History

@Dao
abstract class HistoryDao {

    @Query("SELECT * from ${Tables.HISTORY} ORDER BY timeMillis DESC")
    abstract fun get(): LiveData<List<History>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(history: List<History>)

    @Query("DELETE FROM ${Tables.HISTORY}")
    abstract fun clear(): Int

    @Transaction
    open fun clearInsert(history: List<History>) {
        clear()
        insert(history)
    }
}