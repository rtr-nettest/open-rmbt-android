package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.CellLocation

@Dao
interface CellLocationDao {

    @Query("SELECT * from ${Tables.CELL_LOCATION} WHERE testUUID == :testUUID")
    fun getCellLocationsForTest(testUUID: String): List<CellLocation>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cellLocation: CellLocation): Long
}