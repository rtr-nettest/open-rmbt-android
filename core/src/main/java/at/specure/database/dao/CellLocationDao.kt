package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.CellLocationRecord

@Dao
interface CellLocationDao {

    @Query("SELECT * from ${Tables.CELL_LOCATION} WHERE testUUID == :testUUID")
    fun getCellLocationsForTest(testUUID: String): List<CellLocationRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(cellLocation: CellLocationRecord): Long
}