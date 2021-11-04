package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.HistoryLoopMedian
import at.specure.data.Tables
import at.specure.data.entity.History

@Dao
abstract class HistoryMedianDao {

    @Query("SELECT COUNT(*) from ${Tables.HISTORY_MEDIAN}")
    abstract fun getItemsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveHistory(history: HistoryLoopMedian)

    @Transaction
    open fun insert(historyLoopMedian: HistoryLoopMedian) {
        saveHistory(historyLoopMedian)
    }

    @Query("DELETE FROM ${Tables.HISTORY_MEDIAN}")
    abstract fun clearReferences(): Int

    @Query("DELETE FROM ${Tables.HISTORY_MEDIAN}")
    abstract fun clearHistory(): Int

    @Transaction
    open fun clearInsert(history: HistoryLoopMedian) {
        insert(history)
    }

    @Transaction
    open fun clear() {
        clearHistory()
    }

    @Query("SELECT * FROM ${Tables.HISTORY_MEDIAN} WHERE loopUUID =:loopUuid")
    abstract fun getItemByLoopUUID(loopUuid: String): LiveData<HistoryLoopMedian?>
}
