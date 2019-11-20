package at.specure.database.dao

import androidx.lifecycle.MutableLiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.CellInfo

@Dao
interface CellInfoDao {

    @Query("SELECT * from ${Tables.CELL_INFO} WHERE testUUID == :testUUID")
    fun getCellInfosForTest(testUUID: String): MutableLiveData<List<CellInfo>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cellInfo: CellInfo)
}
