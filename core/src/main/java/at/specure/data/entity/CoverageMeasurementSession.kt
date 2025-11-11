package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables
import java.util.UUID

@Entity(tableName = Tables.SIGNAL_MEASUREMENT_SESSION)
data class CoverageMeasurementSession(


    /**
     * internal id of a signal measurement session
     */
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),

    /**
     * Id of measurement record related to this session
     */
    val measurementId: String? = null,

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
     * defines the maximum time in seconds for a single session (thus, is that timer expires a new /coverageRequest is needed. The timer is started
     * with the actual start of the measurement (this might, in case of no coverage be before response from the coverageRequest is received).
     *
     * After timer expiration a new measurement shall be started (as it is done when there is no coverage.
     */
    val maxCoverageSessionSeconds: Int? = null,

    /**
     * defines the maximum total measurement time. After this timeout the coverage measurement must end (thus, the user interface must switch to results
     */
    val maxCoverageMeasurementSeconds: Int? = null,

    /**
     * Was the measurement synced with the server - was coverageResult successfully sent?
     */
    val synced: Boolean = false
)