package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Columns.TEST_UUID_PARENT_COLUMN
import at.specure.database.Tables.PING

@Entity(tableName = PING)
data class Ping(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ForeignKey(entity = TestRecord::class, parentColumns = [TEST_UUID_PARENT_COLUMN], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,
    val value: Long,
    val valueServer: Long,
    val testTimeNanos: Long
)