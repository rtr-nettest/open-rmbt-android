package at.specure.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.GraphItem

@Dao
interface GraphItemDao {

    @Query("SELECT * from ${Tables.TEST_GRAPH_ITEM} WHERE testUUID == :testUUID AND type == ${GraphItem.GRAPH_ITEM_TYPE_UPLOAD} ORDER BY progress asc")
    fun getUploadGraphLiveData(testUUID: String): LiveData<List<GraphItem>>

    @Query("SELECT * from ${Tables.TEST_GRAPH_ITEM} WHERE testUUID == :testUUID AND type == ${GraphItem.GRAPH_ITEM_TYPE_DOWNLOAD} ORDER BY progress asc")
    fun getDownloadGraphLiveData(testUUID: String): LiveData<List<GraphItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItem(graphItem: GraphItem): Long

    @Query("DELETE FROM ${Tables.TEST_GRAPH_ITEM}")
    fun deleteAll(): Int
}