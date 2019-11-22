package at.specure.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.database.Columns.TEST_UUID_PARENT_COLUMN
import at.specure.database.Tables.TEST
import at.specure.info.TransportType
import at.specure.measurement.MeasurementState

@Entity(tableName = TEST)
data class Test(

    /**
     * uuid of the test obtained from control server from /testRequest
     */
    @PrimaryKey
    @ColumnInfo(name = TEST_UUID_PARENT_COLUMN)
    val uuid: String,
    /**
     * token of the test obtained from control server from /testRequest to use with measurement server
     */
    val token: String?,
    /**
     * Remote port of the measurement server to communicate through
     */
    val portRemote: String?,
    /**
     * Bytes downloaded during download phase in total (Application layer)
     */
    val downloadedBytes: Long?,
    /**
     * Bytes uploaded during upload phase in total (Application layer)
     */
    val uploadedBytes: Long?,
    /**
     * Bytes downloaded during the whole test in total (Application layer)
     */
    val totalDownloadedBytes: Long?,
    /**
     * Bytes uploaded during the whole test in total (Application layer)
     */
    val totalUploadedBytes: Long?,
    /**
     * Type of the encryption
     */
    val encryptionType: String?,
    /**
     * Public ip address of the client
     */
    val ipPublicClient: String?,
    /**
     * Public ip address of the server
     */
    val ipPublicServer: String?,
    /**
     * Duration of the download phase in ns
     */
    val downloadPhaseDurationNs: Long?,
    /**
     * Duration of the upload phase in ns
     */
    val uploadPhaseDurationNs: Long?,
    /**
     * Number of threads used during the upload and download phase (there is only intended number of threads and no if the fallback to 1 thread was triggered)
     */
    val threadNumber: Int?,
    /**
     * Download speed in Bps
     */
    val downloadSpeedBps: Long?,
    /**
     * Upload speed in Bps
     */
    val uploadSpeedBps: Long?,
    /**
     * Shortest ping in ns
     */
    val pingShortestNs: Long?,
    /**
     * Total downloaded bytes on the interface
     */
    val downloadedBytesOnInterface: Long?,
    /**
     * Total uploaded bytes on the interface
     */
    val uploadedBytesOnInterface: Long?,
    /**
     * Total downloaded bytes on the interface during download phase
     */
    val downloadedBytesOnDownloadInterface: Long?,
    /**
     * Total uploaded bytes on the interface during download phase
     */
    val uploadedBytesOnDownloadInterface: Long?,
    /**
     * Total downloaded bytes on the interface during upload phase
     */
    val downloadedBytesOnUploadInterface: Long?,
    /**
     * Total uploaded bytes on the interface during upload phase
     */
    val uploadedBytesOnUploadInterfaceKb: Long?,
    /**
     * Start time of the download phase
     */
    val timeDownloadOffsetNs: Long?,
    /**
     * Start time of the upload phase
     */
    val timeUploadOffsetNs: Long?,
    /**
     * State of the test
     */
    val state: MeasurementState,
    /**
     * Type of the network
     */
    val transportType: TransportType,
    /**
     * Timestamp of the test start
     */
    val time: Long?
)