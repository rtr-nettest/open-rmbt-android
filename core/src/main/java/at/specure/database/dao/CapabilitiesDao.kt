package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.CapabilitiesRecord

@Dao
interface CapabilitiesDao {

    @Query("SELECT * from ${Tables.CAPABILITIES} WHERE testUUID == :testUUID LIMIT 1")
    fun getCapabilitiesForTest(testUUID: String): CapabilitiesRecord

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(capabilities: CapabilitiesRecord)
}