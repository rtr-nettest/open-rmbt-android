package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.DownloadTrafficRecord
import at.specure.database.entity.UploadTrafficRecord

@Dao
interface TestTrafficItemDao {

    @Query("SELECT * from ${Tables.UPLOAD_TRAFFIC} WHERE testUUID == :testUUID")
    fun getTestTrafficUploadItems(testUUID: String): List<UploadTrafficRecord>

    @Query("SELECT * from ${Tables.DOWNLOAD_TRAFFIC} WHERE testUUID == :testUUID")
    fun getTestTrafficDownloadItems(testUUID: String): List<DownloadTrafficRecord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDownloadItem(testTrafficItem: DownloadTrafficRecord)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertUploadItem(testTrafficItem: UploadTrafficRecord)
}