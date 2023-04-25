package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables

@Entity(
    tableName = Tables.PERMISSIONS_STATUS,
    foreignKeys = [
        ForeignKey(
            entity = TestRecord::class,
            parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
            childColumns = ["testUUID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PermissionStatusRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testUUID: String?,
    val signalChunkId: String?,
    val permissionName: String,
    val status: Boolean
)