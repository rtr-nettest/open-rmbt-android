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

    @Query("SELECT * from ${Tables.SIGNAL} WHERE testUUID == :testUUID AND cellUuid == :cellUUID ORDER BY timeNanos LIMIT 1")
    fun getLatestForCell(testUUID: String, cellUUID: String): SignalRecord

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(signal: SignalRecord)

    @Query("DELETE FROM ${Tables.SIGNAL} WHERE testUUID=:testUUID")
    fun remove(testUUID: String)
}