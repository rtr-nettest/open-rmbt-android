package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.TestTrafficDownload
import at.specure.database.entity.TestTrafficUpload

@Dao
interface TestTrafficItemDao {

    @Query("SELECT * from ${Tables.TEST_TRAFFIC_UPLOAD_ITEM} WHERE testUUID == :testUUID")
    fun getTestTrafficUploadItems(testUUID: String): List<TestTrafficUpload>

    @Query("SELECT * from ${Tables.TEST_TRAFFIC_DOWNLOAD_ITEM} WHERE testUUID == :testUUID")
    fun getTestTrafficDownloadItems(testUUID: String): List<TestTrafficDownload>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDownloadItem(testTrafficItem: TestTrafficDownload)
}