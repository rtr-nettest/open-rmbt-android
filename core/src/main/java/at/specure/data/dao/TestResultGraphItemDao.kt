package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.TestResultGraphItemRecord

@Dao
abstract class TestResultGraphItemDao {

    @Query("SELECT * from ${Tables.TEST_RESULT_GRAPH_ITEM} WHERE testUUID == :testUUID AND type == :typeValue ORDER BY time ASC")
    abstract fun getGraphDataLiveData(testUUID: String, typeValue: Int): LiveData<List<TestResultGraphItemRecord>>

    @Query("DELETE FROM ${Tables.TEST_RESULT_GRAPH_ITEM} WHERE (testUUID == :testOpenUUID AND type == :typeValue)")
    abstract fun removeGraphItem(testOpenUUID: String, typeValue: Int)

    @Transaction
    open fun clearInsertItems(graphItems: List<TestResultGraphItemRecord>) {
        if (!graphItems.isNullOrEmpty()) {
            removeGraphItem(graphItems.first().testUUID, graphItems.first().type.typeValue)
            insertItem(graphItems)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertItem(graphItem: List<TestResultGraphItemRecord>)

    @Query("DELETE FROM ${Tables.TEST_RESULT_GRAPH_ITEM}")
    abstract fun deleteAll(): Int
}