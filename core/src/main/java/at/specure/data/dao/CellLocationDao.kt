package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import at.specure.data.Tables
import at.specure.data.entity.CellLocationRecord
import timber.log.Timber

@Dao
interface CellLocationDao {

    @Query("SELECT * from ${Tables.CELL_LOCATION} WHERE ((testUUID IS :testUUID) AND (signalChunkId IS :signalChunkId))")
    fun get(testUUID: String?, signalChunkId: String?): List<CellLocationRecord>

    @Upsert
    fun insert(cellLocation: CellLocationRecord): Long

    @Query("DELETE FROM ${Tables.CELL_LOCATION} WHERE ((testUUID IS :testUUID) AND (signalChunkId IS :signalChunkId))")
    fun remove(testUUID: String?, signalChunkId: String?)

    @Query("SELECT * FROM ${Tables.CELL_LOCATION} WHERE ((testUUID IS :testUUID) AND (signalChunkId IS :signalChunkId)) AND scramblingCode==:scramblingCode")
    fun getSingleCellLocation(testUUID: String?, signalChunkId: String?, scramblingCode: Int): List<CellLocationRecord>

    @Transaction
    fun insertNew(testUUID: String?, signalChunkId: String?, cellLocationList: List<CellLocationRecord>) {
        val cellLocationListDistinct = cellLocationList.distinct()
        cellLocationListDistinct.forEach { newCellLocation ->
            val cellLocationsExist = getSingleCellLocation(testUUID, signalChunkId, newCellLocation.scramblingCode)
            val sameCellLocationList = cellLocationsExist.filter { oldCellLocation ->
                (oldCellLocation.areaCode == newCellLocation.areaCode && oldCellLocation.locationId == newCellLocation.locationId)
            }
            if (sameCellLocationList.isEmpty()) {
                Timber.d("Inserting cell location true: ${newCellLocation.areaCode}, ${newCellLocation.locationId}, ${newCellLocation.scramblingCode}")
                insert(newCellLocation)
            } else {
                Timber.d("Inserting cell location false: ${newCellLocation.areaCode}, ${newCellLocation.locationId}, ${newCellLocation.scramblingCode}")
            }
        }
    }
}