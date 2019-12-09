package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.CellInfoRecord

@Dao
abstract class CellInfoDao {

    @Query("SELECT * FROM ${Tables.CELL_INFO} WHERE testUUID=:testUUID")
    abstract fun get(testUUID: String): List<CellInfoRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(cellInfo: List<CellInfoRecord>)

    @Query("DELETE FROM ${Tables.CELL_INFO} WHERE testUUID=:testUUID")
    abstract fun removeCellInfo(testUUID: String)

    @Transaction
    open fun clearInsert(testUUID: String, cellInfo: List<CellInfoRecord>) {
        removeCellInfo(testUUID)
        insert(cellInfo)
    }
}