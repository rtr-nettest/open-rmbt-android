package at.specure.data.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.History

@Dao
abstract class HistoryDao {

    @Query("SELECT * from ${Tables.HISTORY} ORDER BY time DESC")
    abstract fun getHistorySource(): DataSource.Factory<Int, History>

    @Query("SELECT COUNT(*) from ${Tables.HISTORY}")
    abstract fun getItemsCount(): Int

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