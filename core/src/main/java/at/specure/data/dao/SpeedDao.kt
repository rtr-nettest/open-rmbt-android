package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.SpeedRecord

@Dao
interface SpeedDao {

    @Query("SELECT * from ${Tables.SPEED} WHERE testUUID == :testUUID ORDER BY timestampNanos")
    fun get(testUUID: String): List<SpeedRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(testTrafficItem: SpeedRecord)
}