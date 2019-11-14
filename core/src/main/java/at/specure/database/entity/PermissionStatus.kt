package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Tables.PERMISSIONS_STATUS

@Entity(tableName = PERMISSIONS_STATUS)
data class PermissionStatus(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ForeignKey(entity = Test::class, parentColumns = ["testUUID"], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String?,
    val permissionName: String?,
    val status: Boolean?
)