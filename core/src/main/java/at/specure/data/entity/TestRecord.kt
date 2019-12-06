package at.specure.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.rtr.rmbt.client.helper.TestStatus
import at.specure.data.Columns
import at.specure.data.Tables
import at.specure.info.TransportType

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
    val threadNumber: Int,

    /**
     * Remote port of the measurement server to communicate through
     * test_port_remote
     */
    var portRemote: Int = 0,

    /**
     * Bytes downloaded during download phase in total (Application layer)
     * test_bytes_download
     */
    var bytesDownload: Long = 0,

    /**
     * Bytes uploaded during upload phase in total (Application layer)
     * test_bytes_upload
     */
    var bytesUpload: Long = 0,

    /**
     * Bytes downloaded during the whole test in total (Application layer)
     * test_total_bytes_download
     */
    var totalBytesDownload: Long = 0,

    /**
     * Bytes uploaded during the whole test in total (Application layer)
     * test_total_bytes_upload
     */
    var totalBytesUpload: Long = 0,

    /**
     * Type of the encryption
     * test_encryption
     */
    var encryption: String? = null,

    /**
     * Public ip address of the client
     * test_ip_local
     */
    var ipLocal: String? = null,

    /**
     * Public ip address of the server
     * test_ip_server
     */
    var ipServer: String? = null,

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
    var downloadSpeedBps: Long = 0,

    /**
     * Upload speed in Bps
     * test_speed_upload
     */
    var uploadSpeedBps: Long = 0,

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
    var uploadedBytesOnUploadInterfaceKb: Long = 0,

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
    var transportType: TransportType? = null // TODO
)