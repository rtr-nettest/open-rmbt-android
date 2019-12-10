package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.History

@Dao
interface HistoryDao {

    @Query("SELECT * from ${Tables.HISTORY} ORDER BY timeMillis DESC")
    fun get(): List<History>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(history: History)

    @Query("DELETE FROM ${Tables.HISTORY}")
    fun deleteAll(): Int
}