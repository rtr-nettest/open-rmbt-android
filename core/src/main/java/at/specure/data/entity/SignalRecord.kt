package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import at.specure.data.Columns
import at.specure.data.Tables
import at.specure.info.TransportType
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.strength.SignalSource

@Entity(
    tableName = Tables.SIGNAL,
    primaryKeys = ["timeNanos", "testUUID", "cellUuid"],
    foreignKeys = [
        ForeignKey(
            entity = TestRecord::class,
            parentColumns = [Columns.TEST_UUID_PARENT_COLUMN],
            childColumns = ["testUUID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SignalRecord(
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

    /**
     * NR connection state from netmonster magic during the signal obtaining, added because of 5G NSA (we have inactive NR cells found with signal
     * information, but it is still NSA mode, so we want to distinguish it somehow - problem is we do not know how to report pure 5G then, we will
     * need to debug it when 5G SA will be available in some country)
     */
    val nrConnectionState: NRConnectionState,

    /**
     * Indication of source of the signal information (CellInfo, onSignalStrengthChanged, not available)
     */
    val source: SignalSource,

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
) {
    fun hasNonNullSignal(): Boolean {
        return listOfNotNull(
            signal,
            wifiLinkSpeed,
            bitErrorRate,
            lteRsrp,
            lteRsrq,
            lteRssnr,
            lteCqi,
            nrCsiRsrp,
            nrCsiRsrq,
            nrCsiSinr,
            nrSsRsrp,
            nrSsRsrq,
            nrSsSinr
        ).isNotEmpty()
    }
}