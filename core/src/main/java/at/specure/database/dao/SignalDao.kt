package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.Signal

@Dao
interface SignalDao {

    @Query("SELECT * from ${Tables.SIGNAL} WHERE testUUID == :testUUID")
    fun getSignalsForTest(testUUID: String): List<Signal>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(signal: Signal)
}