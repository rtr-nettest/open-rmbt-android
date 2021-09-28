package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables

@Entity(
    tableName = Tables.PING,
    foreignKeys = [
        ForeignKey(
            entity = TestRecord::class,
            parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
            childColumns = ["testUUID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PingRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testUUID: String,
    val value: Long,
    val valueServer: Long,
    val testTimeNanos: Long
)