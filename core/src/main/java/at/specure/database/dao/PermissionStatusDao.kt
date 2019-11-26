package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.PermissionStatus

@Dao
interface PermissionStatusDao {

    @Query("SELECT * from ${Tables.PERMISSIONS_STATUS} WHERE testUUID == :testUUID")
    fun getPermissionStatusForTest(testUUID: String): List<PermissionStatus>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(permissionStatus: PermissionStatus)
}