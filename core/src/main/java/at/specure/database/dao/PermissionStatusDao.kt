package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.PermissionStatusRecord

@Dao
interface PermissionStatusDao {

    @Query("SELECT * from ${Tables.PERMISSIONS_STATUS} WHERE testUUID == :testUUID")
    fun getPermissionStatusForTest(testUUID: String): List<PermissionStatusRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(permissionStatus: PermissionStatusRecord)
}