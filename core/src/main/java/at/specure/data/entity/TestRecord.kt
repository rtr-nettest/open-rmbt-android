package at.specure.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.rmbt.client.control.data.TestFinishReason
import at.rtr.rmbt.client.helper.TestStatus
import at.specure.data.Columns
import at.specure.data.Tables
import at.specure.info.TransportType
import at.specure.info.network.MobileNetworkType

@Entity(tableName = Tables.TEST)
data class TestRecord(

    /**
     * uuid of the test obtained from control server from /testRequest
     */
    @PrimaryKey
    @ColumnInfo(name = Columns.TEST_UUID_PARENT_COLUMN)
    val uuid: String,

    val loopUUID: String?,

    /**
     * token of the test obtained from control server from /testRequest to use with measurement server
     */
    val token: String,

    /**
     * Timestamp of the test start
     * test_token
     */
    val testStartTimeMillis: Long,

    /**
     * Number of threads used during the upload and download phase (there is only intended number of threads and no if the fallback to 1 thread was triggered)
     * test_num_threads
     */
    val threadCount: Int,

    /**
     * Remote port of the measurement server to communicate through
     * test_port_remote
     */
    var portRemote: Int = 0,

    /**
     * Bytes downloaded during download phase in total (Application layer)
     * test_bytes_download
     */
    var bytesDownloaded: Long = 0,

    /**
     * Bytes uploaded during upload phase in total (Application layer)
     * test_bytes_upload
     */
    var bytesUploaded: Long = 0,

    /**
     * Bytes downloaded during the whole test in total (Application layer)
     * test_total_bytes_download
     */
    var totalBytesDownloaded: Long = 0,

    /**
     * Bytes uploaded during the whole test in total (Application layer)
     * test_total_bytes_upload
     */
    var totalBytesUploaded: Long = 0,

    /**
     * Type of the encryption
     * test_encryption
     */
    var encryption: String? = null,

    /**
     * Public ip address of the client
     * test_ip_local
     */
    var clientPublicIp: String? = null,

    /**
     * Public ip address of the server
     * test_ip_server
     */
    var serverPublicIp: String? = null,

    /**
     * Duration of the download phase in ns
     * test_nsec_download
     */
    var downloadDurationNanos: Long = 0,

    /**
     * Duration of the upload phase in ns
     * test_nsec_upload
     */
    var uploadDurationNanos: Long = 0,

    /**
     * Download speed in Bps
     * test_speed_download
     */
    var downloadSpeedKps: Long = 0,

    /**
     * Upload speed in Bps
     * test_speed_upload
     */
    var uploadSpeedKps: Long = 0,

    /**
     * Shortest ping in ns
     * test_ping_shortest
     */
    var shortestPingNanos: Long = 0,

    /**
     * Total downloaded bytes on the interface
     * test_if_bytes_download
     */
    var downloadedBytesOnInterface: Long = 0,

    /**
     * Total uploaded bytes on the interface
     * test_if_bytes_upload
     */
    var uploadedBytesOnInterface: Long = 0,

    /**
     * Total downloaded bytes on the interface during download phase
     * testdl_if_bytes_download
     */
    var downloadedBytesOnDownloadInterface: Long = 0,

    /**
     * Total uploaded bytes on the interface during download phase
     * testdl_if_bytes_upload
     */
    var uploadedBytesOnDownloadInterface: Long = 0,

    /**
     * Total downloaded bytes on the interface during upload phase
     * testul_if_bytes_download
     */
    var downloadedBytesOnUploadInterface: Long = 0,

    /**
     * Total uploaded bytes on the interface during upload phase
     * testul_if_bytes_upload
     */
    var uploadedBytesOnUploadInterface: Long = 0,

    /**
     * Start time of the download phase
     * time_dl_ns
     */
    var timeDownloadOffsetNanos: Long? = null,

    /**
     * Start time of the upload phase
     * time_ul_ns
     */
    var timeUploadOffsetNanos: Long? = null,

    /**
     * State of the test
     */
    var status: TestStatus? = null,

    /**
     * Type of the network
     */
    var transportType: TransportType? = null,

    /**
     * Type of mobile network
     */
    var mobileNetworkType: MobileNetworkType? = null,

    /**
     * Time of test in milliseconds
     */
    var testTimeMillis: Long = 0,

    /**
     * Count of unsuccessful submissions
     */
    var submissionRetryCount: Long = 0,

    /**
     * Reason of test finished
     */
    var testFinishReason: TestFinishReason = TestFinishReason.ERROR,

    /**
     * Phase which was the last done by the client
     */
    var lastClientStatus: TestStatus = TestStatus.WAIT,

    /**
     * Stacktrace of IllegalNetworkChangeException exception grabbed from RMBTClient which was happened during the test.
     * May be null if test was success or cancelled
     */
    var testErrorCause: String? = null // todo add an catch IllegalNetworkChangeException
)