package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables
import at.specure.info.TransportType
import at.specure.info.network.MobileNetworkType
import at.specure.test.DeviceInfo
import at.specure.test.SignalMeasurementType
import java.util.UUID

@Entity(tableName = Tables.SIGNAL_MEASUREMENT)
data class SignalMeasurementRecord(

    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * Unique cell UUID
     */
    val networkUUID: String,

    /**
     * Test start time in millis
     */
    val startTimeMillis: Long = System.currentTimeMillis(),

    /**
     * Test start time in nanos
     */
    val startTimeNanos: Long = System.nanoTime(),

    /**
     * Location info on the start of signal measurement
     */
    var location: DeviceInfo.Location?,

    /**
     * Type of mobile network
     */
    var mobileNetworkType: MobileNetworkType? = null,

    /**
     * Type of the network
     */
    var transportType: TransportType?,

    /**
     * network capabilies obtained from android api - not modified
     */
    var rawCapabilitiesRecord: String?,

    /**
     * Type of the signal measurement [LOOP_WAITING, DEDICATED]
     */
    var signalMeasurementType: SignalMeasurementType
)