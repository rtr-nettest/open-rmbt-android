package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM ${Tables.CELL_LOCATION} WHERE testUUID=:testUUID AND areaCode==:areaCode AND locationId==:locationId AND scramblingCode==:scramblingCode")
    fun getSingleCellLocation(testUUID: String, areaCode: Int?, locationId: Int?, scramblingCode: Int): List<CellLocationRecord>

    @Transaction
    open fun insertNew(testUUID: String, cellLocationList: List<CellLocationRecord>) {
        cellLocationList.forEach {
            val cellLocationsExist = getSingleCellLocation(testUUID, it.areaCode, it.locationId, it.scramblingCode)
            if (cellLocationsExist.isEmpty()) {
                insert(it)
            }
        }
    }
}