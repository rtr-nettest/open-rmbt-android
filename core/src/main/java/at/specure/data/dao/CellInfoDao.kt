package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import at.specure.data.Tables
import at.specure.data.entity.CellInfoRecord
import at.specure.info.TransportType

@Dao
abstract class CellInfoDao {

    @Query("SELECT * FROM ${Tables.CELL_INFO} WHERE ((testUUID IS :testUUID) AND (signalChunkId IS :signalChunkId))")
    abstract fun get(testUUID: String?, signalChunkId: String?): List<CellInfoRecord>

    @Upsert
    abstract fun insert(cellInfo: List<CellInfoRecord>)

    @Query("DELETE FROM ${Tables.CELL_INFO} WHERE ((testUUID IS :testUUID) AND (signalChunkId IS :signalChunkId))")
    abstract fun removeAllCellInfo(testUUID: String?, signalChunkId: String?)

    @Query("DELETE FROM ${Tables.CELL_INFO} WHERE ((testUUID IS :testUUID) AND (signalChunkId IS :signalChunkId)) AND uuid==:cellInfoUUID")
    abstract fun removeSingleCellInfo(testUUID: String?, signalChunkId: String?, cellInfoUUID: String)

    @Transaction
    open fun clearInsert(testUUID: String?, signalChunkId: String?, cellInfo: List<CellInfoRecord>) {
        val filteredCellInfo = cellInfo.filter { it.cellTechnology != null || it.transportType == TransportType.WIFI }
        filteredCellInfo.forEach {
            if (it.cellTechnology != null || it.transportType == TransportType.WIFI) removeSingleCellInfo(testUUID, signalChunkId, it.uuid)
        }

        insert(filteredCellInfo)
    }
}