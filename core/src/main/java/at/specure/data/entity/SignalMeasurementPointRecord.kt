package at.specure.data.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables
import at.specure.test.DeviceInfo
import java.util.UUID

@Keep
@Entity(tableName = Tables.SIGNAL_MEASUREMENT_POINT)
data class SignalMeasurementPointRecord(

    @PrimaryKey
    @ColumnInfo(name = Columns.SIGNAL_MEASUREMENT_POINT_ID_PARENT_COLUMN)
    val id: String = UUID.randomUUID().toString(),

    /**
     *  FK for SignalMeasurementSession.sessionId
     */
    val sessionId: String,

    /**
     * FK for SignalRecord.id
     */
    val signalRecordId: String?,

    /**
     * Order in which it was logged during the measurement
     */
    val sequenceNumber: Int,

    /**
     * Location info on the start of signal measurement
     */
    var location: DeviceInfo.Location?,

    /**
     * Timestamp of information obtained
     */
    val timestamp: Long = System.currentTimeMillis()
)