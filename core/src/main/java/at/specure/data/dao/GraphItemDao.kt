package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.GraphItemRecord

@Dao
interface GraphItemDao {

    @Query("SELECT * from ${Tables.TEST_GRAPH_ITEM} WHERE testUUID == :testUUID AND type == ${GraphItemRecord.GRAPH_ITEM_TYPE_UPLOAD} ORDER BY progress asc")
    fun getUploadGraphLiveData(testUUID: String): List<GraphItemRecord>

    @Query("SELECT * from ${Tables.TEST_GRAPH_ITEM} WHERE testUUID == :testUUID AND type == ${GraphItemRecord.GRAPH_ITEM_TYPE_DOWNLOAD} ORDER BY progress asc")
    fun getDownloadGraphLiveData(testUUID: String): List<GraphItemRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItem(graphItem: GraphItemRecord): Long

    @Query("DELETE FROM ${Tables.TEST_GRAPH_ITEM}")
    fun deleteAll(): Int
}