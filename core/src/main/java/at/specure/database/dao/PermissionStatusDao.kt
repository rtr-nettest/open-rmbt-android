package at.specure.database.dao

import androidx.lifecycle.MutableLiveData
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.PermissionStatus

interface PermissionStatusDao {

    @Query("SELECT * from ${Tables.PERMISSIONS_STATUS} WHERE testUUID == :testUUID")
    fun getPermissionStatusForTest(testUUID: String): MutableLiveData<List<PermissionStatus>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(permissionStatus: PermissionStatus)
}