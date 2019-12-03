package at.specure.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.database.Columns.TEST_UUID_PARENT_COLUMN
import at.specure.database.Tables.CELL_INFO
import at.specure.info.TransportType
import at.specure.info.cell.CellTechnology

@Entity(tableName = CELL_INFO)
data class CellInfoRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ForeignKey(entity = Test::class, parentColumns = [TEST_UUID_PARENT_COLUMN], childColumns = ["testUUID"], onDelete = ForeignKey.CASCADE)
    val testUUID: String,
    val active: Boolean,
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