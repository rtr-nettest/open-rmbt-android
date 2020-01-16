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

    @Query("SELECT * from ${Tables.TEST_RESULT_GRAPH_ITEM} WHERE testOpenUUID == :testUUID AND type == ${TestResultGraphItemRecord.RESULT_GRAPH_ITEM_TYPE_UPLOAD} ORDER BY time ASC")
    abstract fun getUploadGraphLiveData(testUUID: String): LiveData<List<TestResultGraphItemRecord>?>

    @Query("SELECT * from ${Tables.TEST_RESULT_GRAPH_ITEM} WHERE testOpenUUID == :testUUID AND type == ${TestResultGraphItemRecord.RESULT_GRAPH_ITEM_TYPE_DOWNLOAD} ORDER BY time ASC")
    abstract fun getDownloadGraphLiveData(testUUID: String): LiveData<List<TestResultGraphItemRecord>?>

    @Query("SELECT * from ${Tables.TEST_RESULT_GRAPH_ITEM} WHERE testOpenUUID == :testUUID AND type == ${TestResultGraphItemRecord.RESULT_GRAPH_ITEM_TYPE_PING} ORDER BY time ASC")
    abstract fun getPingGraphLiveData(testUUID: String): LiveData<List<TestResultGraphItemRecord>?>

    @Query("SELECT * from ${Tables.TEST_RESULT_GRAPH_ITEM} WHERE testOpenUUID == :testUUID AND type == ${TestResultGraphItemRecord.RESULT_GRAPH_ITEM_TYPE_SIGNAL} ORDER BY time ASC")
    abstract fun getSignalGraphLiveData(testUUID: String): LiveData<List<TestResultGraphItemRecord>?>

    @Query("DELETE FROM ${Tables.TEST_RESULT_GRAPH_ITEM} WHERE (testOpenUUID == :testOpenUUID AND type == :type)")
    abstract fun removeGraphItem(testOpenUUID: String, type: Int)

    @Transaction
    open fun clearInsertItems(graphItems: List<TestResultGraphItemRecord>) {
        if (!graphItems.isNullOrEmpty()) {
            removeGraphItem(graphItems.first().testOpenUUID, graphItems.first().type)
            insertItem(graphItems)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertItem(graphItem: List<TestResultGraphItemRecord>)

    @Query("DELETE FROM ${Tables.TEST_RESULT_GRAPH_ITEM}")
    abstract fun deleteAll(): Int
}