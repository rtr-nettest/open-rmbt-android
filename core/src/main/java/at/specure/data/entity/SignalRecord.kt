package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables
import at.specure.info.TransportType
import at.specure.info.network.MobileNetworkType

@Entity(tableName = Tables.SIGNAL)
data class SignalRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ForeignKey(
        entity = TestRecord::class,
        parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
        childColumns = ["testUUID"],
        onDelete = ForeignKey.CASCADE
    )
    val testUUID: String,
    val cellUuid: String,
    /**
     * difference between this update of the signal during the test and start time of the test
     */
    val timeNanos: Long,
    /**
     * difference between last update of the signal during the test and start time of the test
     */
    val timeNanosLast: Long?,

    val transportType: TransportType,

    val mobileNetworkType: MobileNetworkType?,

    // wifi
    val signal: Int?,
    val wifiLinkSpeed: Int?,
    // 2G/3G
    val bitErrorRate: Int?,
    // 4G
    val lteRsrp: Int?,
    val lteRsrq: Int?,
    val lteRssnr: Int?,
    val lteCqi: Int?,
    val timingAdvance: Int?,
    // 5G
    val nrCsiRsrp: Int?,
    val nrCsiRsrq: Int?,
    val nrCsiSinr: Int?,
    val nrSsRsrp: Int?,
    val nrSsRsrq: Int?,
    val nrSsSinr: Int?
)