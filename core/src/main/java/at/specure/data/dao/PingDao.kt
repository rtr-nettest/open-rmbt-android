package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.PingRecord

@Dao
interface PingDao {

    @Query("SELECT * from ${Tables.PING} WHERE testUUID == :testUUID ORDER BY testTimeNanos")
    fun get(testUUID: String): List<PingRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(ping: PingRecord)
}