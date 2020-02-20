package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.CapabilitiesRecord

@Dao
interface CapabilitiesDao {

    @Query("SELECT * from ${Tables.CAPABILITIES} WHERE testUUID == :testUUID LIMIT 1")
    fun get(testUUID: String): CapabilitiesRecord

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(capabilities: CapabilitiesRecord)

    @Query("DELETE FROM ${Tables.CAPABILITIES} WHERE testUUID=:testUUID")
    fun remove(testUUID: String)
}