package at.specure.info.strength

import android.os.Parcelable
import at.specure.info.TransportType
import kotlinx.parcelize.Parcelize

@Parcelize
class SignalStrengthInfoLte(
    override val transport: TransportType,
    override val value: Int?,
    override val rsrq: Int?,
    override val signalLevel: Int,
    override val min: Int,
    override val max: Int,
    override val timestampNanos: Long,
    override val source: SignalSource,

    /**
     * Get channel quality indicator
     *
     * @return the CQI if available or null if unavailable.
     */
    val cqi: Int?,

    /**
     * Get reference signal received power in dBm
     *
     * @return the RSRP of the measured cell.
     */
    val rsrp: Int?,

    /**
     * Get Received Signal Strength Indication (RSSI) in dBm
     *
     * The value range is [-113, -51] inclusively or null if unavailable.
     *
     * Reference: TS 27.007 8.5 Signal quality +CSQ
     *
     * @return the RSSI if available or null if unavailable.
     */
    val rssi: Int?,

    /**
     * Get reference signal signal-to-noise ratio
     *
     * @return the RSSNR if available or null if unavailable.
     */
    val rssnr: Int?,

    /**
     * Get the timing advance value for LTE, as a value in range of 0..1282.
     * null is reported when there is no
     * active RRC connection. Refer to 3GPP 36.213 Sec 4.2.3
     *
     * @return the LTE timing advance if available
     */
    val timingAdvance: Int?

) : SignalStrengthInfo(), Parcelable