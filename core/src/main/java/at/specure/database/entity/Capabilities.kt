package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Tables.CAPABILITIES

@Entity(tableName = CAPABILITIES)
data class Capabilities(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ForeignKey(entity = Test::class, parentColumns = ["testUUID"], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String?,
    val classificationCount: Int?,
    val qosSupportInfo: Boolean?,
    val rmbtHttpStatus: Boolean?
)