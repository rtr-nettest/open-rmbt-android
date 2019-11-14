package at.specure.database.dao

import androidx.lifecycle.MutableLiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.History

@Dao
interface HistoryDao {

    @Query("SELECT * from ${Tables.HISTORY} ORDER BY time DESC")
    fun getHistoryItems(): MutableLiveData<List<History>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(history: History)

    @Query("DELETE FROM ${Tables.HISTORY}")
    suspend fun deleteAll()
}