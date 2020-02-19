package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.CellLocationRecord

@Dao
interface CellLocationDao {

    @Query("SELECT * from ${Tables.CELL_LOCATION} WHERE testUUID == :testUUID")
    fun get(testUUID: String): List<CellLocationRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(cellLocation: CellLocationRecord): Long

    @Query("DELETE FROM ${Tables.CELL_LOCATION} WHERE testUUID=:testUUID")
    fun remove(testUUID: String)
}