package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Columns
import at.specure.database.Tables

@Entity(tableName = Tables.CELL_LOCATION)
data class CellLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ForeignKey(
        entity = TestRecord::class,
        parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
        childColumns = ["testUUID"],
        onDelete = ForeignKey.CASCADE
    )
    val testUUID: String,
    val primaryScramblingCode: Int?,
    val areaCode: Int?,
    val locationId: String?,
    val timeMillis: Long?,
    val timeNanos: Long?
)