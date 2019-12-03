package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.PingRecord

@Dao
interface PingDao {

    @Query("SELECT * from ${Tables.PING} WHERE testUUID == :testUUID")
    fun getPingsForTest(testUUID: String): List<PingRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(ping: PingRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ping: List<PingRecord>)
}