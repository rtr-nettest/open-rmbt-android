package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.VoipTestResultRecord

@Dao
interface JplResultsDao {

    @Query("SELECT * from ${Tables.JPL} WHERE testUUID == :testUUID LIMIT 1")
    fun get(testUUID: String): VoipTestResultRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(voipTestResultRecord: VoipTestResultRecord)

    @Query("DELETE FROM ${Tables.JPL} WHERE testUUID==:testUUID")
    fun remove(testUUID: String)
}