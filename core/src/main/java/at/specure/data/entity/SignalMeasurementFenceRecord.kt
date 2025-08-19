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
@Entity(tableName = Tables.SIGNAL_MEASUREMENT_FENCE)
data class SignalMeasurementFenceRecord(

    @PrimaryKey
    @ColumnInfo(name = Columns.SIGNAL_MEASUREMENT_FENCE_ID_PARENT_COLUMN)
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
     * Timestamp of creating a fence entry (center of the fence)
     */
    val entryTimestampMillis: Long,

    /**
     * Timestamp of leaving the area of the fence (when other new fence is created)
     */
    val leaveTimestampMillis: Long,

    /**
     * Radius in meters of the fence set in the app
     */
    val radiusMeters: Int,

    /**
     * Technology ID of the mobile network (for further details check MobileNetworkType.kt) from signal record)
     */
    val technologyId: Int?,

    /**
     * Average ping in milliseconds
     */
    val avgPingMillis: Double?,

)