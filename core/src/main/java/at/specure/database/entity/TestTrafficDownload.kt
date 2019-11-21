package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Columns.TEST_UUID_PARENT_COLUMN
import at.specure.database.Tables.TEST_TRAFFIC_DOWNLOAD_ITEM

@Entity(tableName = TEST_TRAFFIC_DOWNLOAD_ITEM)
open class TestTrafficDownload(

    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ForeignKey(entity = Test::class, parentColumns = [TEST_UUID_PARENT_COLUMN], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,
    val threadNumber: Int,
    val time: Long,
    val bytes: Long
)