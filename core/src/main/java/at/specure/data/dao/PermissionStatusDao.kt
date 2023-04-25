package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.PermissionStatusRecord

@Dao
interface PermissionStatusDao {

    @Query("SELECT * from ${Tables.PERMISSIONS_STATUS} WHERE ((testUUID IS :testUUID) AND (signalChunkId IS :signalChunkId))")
    fun get(testUUID: String?, signalChunkId: String?): List<PermissionStatusRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(permissionStatus: PermissionStatusRecord)

    @Query("DELETE FROM ${Tables.PERMISSIONS_STATUS} WHERE ((testUUID IS :testUUID) AND (signalChunkId IS :signalChunkId))")
    fun remove(testUUID: String?, signalChunkId: String?)
}