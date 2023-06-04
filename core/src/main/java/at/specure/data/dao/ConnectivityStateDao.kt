package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.ConnectivityStateRecord

@Dao
interface ConnectivityStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveState(record: ConnectivityStateRecord): Long

    @Query("SELECT * FROM ${Tables.CONNECTIVITY_STATE} WHERE uuid==:uuid")
    fun getStates(uuid: String): List<ConnectivityStateRecord>

    @Query("DELETE FROM ${Tables.CONNECTIVITY_STATE} WHERE uuid==:testUUID")
    fun remove(testUUID: String)
}