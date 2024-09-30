package at.specure.data.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables
import java.util.UUID

@Keep
@Entity(tableName = Tables.SIGNAL_MEASUREMENT_POINT)
data class SignalMeasurementPointRecord(

    @PrimaryKey
    @ColumnInfo(name = Columns.SIGNAL_MEASUREMENT_POINT_ID_PARENT_COLUMN)
    val id: String = UUID.randomUUID().toString(),

    val measurementId: String,
    val measurementRecordId: String,
    val sequenceNumber: Int,
    val timestamp: Long
)