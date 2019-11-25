package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.GRAPH_ITEM_TYPE_DOWNLOAD
import at.specure.database.entity.GRAPH_ITEM_TYPE_UPLOAD
import at.specure.database.entity.GraphItem

@Dao
interface GraphItemDao {

    @Query("SELECT * from ${Tables.TEST_GRAPH_ITEM} WHERE testUUID == :testUUID AND type == $GRAPH_ITEM_TYPE_UPLOAD ORDER BY progress asc")
    fun getUploadGraph(testUUID: String): List<GraphItem>

    @Query("SELECT * from ${Tables.TEST_GRAPH_ITEM} WHERE testUUID == :testUUID AND type == $GRAPH_ITEM_TYPE_DOWNLOAD ORDER BY progress asc")
    fun getDownloadGraph(testUUID: String): List<GraphItem>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItem(graphItem: GraphItem): Long

    @Query("DELETE FROM ${Tables.TEST_GRAPH_ITEM}")
    fun deleteAll(): Int
}