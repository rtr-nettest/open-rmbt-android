package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables

@Entity(
    tableName = Tables.CAPABILITIES,
    foreignKeys = [
        ForeignKey(
            entity = TestRecord::class,
            parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
            childColumns = ["testUUID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CapabilitiesRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testUUID: String,
    val classificationCount: Int,
    val qosSupportInfo: Boolean,
    val rmbtHttpStatus: Boolean
)