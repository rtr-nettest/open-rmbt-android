package at.specure.database.dao

import androidx.lifecycle.MutableLiveData
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.Ping

interface PingDao {

    @Query("SELECT * from ${Tables.PING} WHERE testUUID == :testUUID")
    fun getPingsForTest(testUUID: String): MutableLiveData<List<Ping>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ping: Ping)
}