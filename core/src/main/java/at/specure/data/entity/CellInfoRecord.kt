package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables
import at.specure.info.TransportType
import at.specure.info.cell.CellTechnology

@Entity(tableName = Tables.CELL_INFO)
data class CellInfoRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ForeignKey(
        entity = TestRecord::class,
        parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
        childColumns = ["testUUID"],
        onDelete = ForeignKey.CASCADE
    )
    val testUUID: String,
    val isActive: Boolean,
    val uuid: String,
    val channelNumber: Int?,
    val registered: Boolean,
    val transportType: TransportType,
    // another more for mobile cell
    val cellTechnology: CellTechnology?,
    val areaCode: Int?,
    val locationId: Int?,
    val mcc: Int?,
    val mnc: Int?,
    val primaryScramblingCode: Int?
)