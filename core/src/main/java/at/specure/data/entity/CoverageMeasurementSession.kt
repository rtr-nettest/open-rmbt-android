package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables
import java.util.UUID

@Entity(tableName = Tables.COVERAGE_MEASUREMENT_SESSION)
data class CoverageMeasurementSession(


    /**
     * internal id of a single coverage measurement in a loop
     */
    @PrimaryKey
    val localMeasurementId: String = UUID.randomUUID().toString(),

    /**
     * internal id of loop, which groups all measurements
     */
    val localLoopId: String = UUID.randomUUID().toString(),

    /**
     * server generated id of a signal measurement, result will be sent with this UUID, also when loop - then it needs to be updated from coverageResultResponse
     *
     * Signal serverSessionId UUID. Related only for one network in the loop. Another [SignalMeasurementRecord] must be used for another network.
     * Must be filled with data from server.
     *
     */
    val serverMeasurementId: String? = null,

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
    val synced: Boolean = false,

    /**
     * How many times app tried to send the coverage measurement session results to the server.
     */
    val retryCount: Int = 0,

    /**
     * Number of session item order in the loop.
     */
    val sequenceNumber: Int = 0,
)