package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.TestResultGraphItemRecord
import timber.log.Timber

@Dao
abstract class TestResultGraphItemDao {

    @Query("SELECT * from ${Tables.TEST_RESULT_GRAPH_ITEM} WHERE testUUID == :testUUID AND type == :typeValue GROUP BY time ORDER BY time ASC ")
    abstract fun getGraphDataLiveData(testUUID: String, typeValue: Int): LiveData<List<TestResultGraphItemRecord>>

    @Query("DELETE FROM ${Tables.TEST_RESULT_GRAPH_ITEM} WHERE (testUUID == :testOpenUUID AND type == :typeValue)")
    abstract fun removeGraphItem(testOpenUUID: String, typeValue: Int): Int

    @Transaction
    open fun clearInsertItems(graphItems: List<TestResultGraphItemRecord>?) {
        if (!graphItems.isNullOrEmpty()) {
            val removedCount = removeGraphItem(graphItems.first().testUUID, graphItems.first().type.typeValue)
            Timber.d("DB: clearing graph items:  $removedCount for uuid: ${graphItems.first().testUUID}, type: ${graphItems.first().type.typeValue}")
            val inserted = insertItem(graphItems)
            Timber.d("DB: inserted graph items:  $inserted for uuid: ${graphItems.first().testUUID}, type: ${graphItems.first().type.typeValue}")
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertItem(graphItem: List<TestResultGraphItemRecord>)

    @Query("DELETE FROM ${Tables.TEST_RESULT_GRAPH_ITEM}")
    abstract fun deleteAll(): Int
}