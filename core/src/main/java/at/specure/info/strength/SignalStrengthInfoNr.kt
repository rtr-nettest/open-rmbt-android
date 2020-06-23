package at.specure.info.strength

import android.os.Parcelable
import at.specure.info.TransportType
import kotlinx.android.parcel.Parcelize

@Parcelize
class SignalStrengthInfoNr(
    override val transport: TransportType,
    override val value: Int?,
    override val rsrq: Int?,
    override val signalLevel: Int,
    override val min: Int,
    override val max: Int,
    override val timestampNanos: Long,

    /**
     * Reference: 3GPP TS 38.215.
     * Range: -140 dBm to -44 dBm.
     * @return CSI reference signal  received power
     */
    val csiRsrp: Int?,

    /**
     * Reference: 3GPP TS 38.215.
     * Range: -20 dB to -3 dB.
     * @return CSI reference signal received quality
     */
    val csiRsrq: Int?,

    /**
     * Reference: 3GPP TS 38.215 Sec 5.1.*, 3GPP TS 38.133 10.1.16.1
     * Range: -23 dB to 23 dB
     * @return CSI signal-to-noise and interference ratio
     */
    val csiSinr: Int?,

    /**
     * Reference: 3GPP TS 38.215.
     * Range: -140 dBm to -44 dBm.
     * @return SS reference signal received power
     */
    val ssRsrp: Int?,

    /**
     * Reference: 3GPP TS 38.215.
     * Range: -20 dB to -3 dB.
     * @return SS reference signal received quality
     */
    val ssRsrq: Int?,

    /**
     * Reference: 3GPP TS 38.215 Sec 5.1.*, 3GPP TS 38.133 10.1.16.1
     * Range: -23 dB to 40 dB
     * @return SS signal-to-noise and interference ratio
     */
    val ssSinr: Int?
) : SignalStrengthInfo(), Parcelable