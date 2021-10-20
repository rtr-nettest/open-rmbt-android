package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.SignalRecord

@Dao
interface SignalDao {

    @Query("SELECT * from ${Tables.SIGNAL} WHERE testUUID == :testUUID ORDER BY timeNanos")
    fun get(testUUID: String): List<SignalRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(signal: SignalRecord)

    @Query("DELETE FROM ${Tables.SIGNAL} WHERE testUUID=:testUUID")
    fun remove(testUUID: String)
}