package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables
import java.util.UUID

@Entity(tableName = Tables.SIGNAL_MEASUREMENT_SESSION)
data class SignalMeasurementSession(


    /**
     * internal id of a signal measurement session
     */
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),

    /**
     * server generated id of a signal measurement session, result will be sent with this UUID, also when loop - then it needs to be updated from coverageResultResponse
     *
     * Signal serverSessionId UUID. Related only for one network in the loop. Another [SignalMeasurementRecord] must be used for another network.
     * Must be filled with data from server.
     *
     */
    val serverSessionId: String? = null,

    /**
     * server serverSessionLoopId id of a signal measurement loop session
     */
    val serverSessionLoopId: String? = null,

    val pingServerHost: String? = null,

    val pingServerPort: Int? = null,

    val pingServerToken: String? = null,

    val ipVersion: Int? = null,

    /**
     * IP address of the client
     */
    val remoteIpAddress: String? = null,

    val provider: String? = null,

    /**
     * Local Timestamp of the signal measurement start
     */
    val startTimeMillis: Long = System.currentTimeMillis(),

    /**
     * Local Timestamp of the signal measurement response received from server to count relative time for fences (points)
     */
    val startResponseReceivedMillis: Long = System.currentTimeMillis(),

    /**
     * Was the measurement synced with the server - was coverageResult successfully sent?
     */
    val synced: Boolean = false
)