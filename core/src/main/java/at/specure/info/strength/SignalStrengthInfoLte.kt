package at.specure.info.strength

import at.specure.info.TransportType

class SignalStrengthInfoLte(
    transport: TransportType,
    value: Int?,
    rsrq: Int?,
    signalLevel: Int,
    min: Int,
    max: Int,
    timestampNanos: Long,

    /**
     * Get channel quality indicator
     *
     * @return the CQI if available or
     *         {@link android.telephony.CellInfo#UNAVAILABLE UNAVAILABLE} if unavailable.
     */
    val cqi: Int,

    /**
     * Get reference signal received power in dBm
     *
     * @return the RSRP of the measured cell.
     */
    val rsrp: Int,

    /**
     * Get Received Signal Strength Indication (RSSI) in dBm
     *
     * The value range is [-113, -51] inclusively or {@link CellInfo#UNAVAILABLE} if unavailable.
     *
     * Reference: TS 27.007 8.5 Signal quality +CSQ
     *
     * @return the RSSI if available or {@link CellInfo#UNAVAILABLE} if unavailable.
     */
    val rssi: Int,

    /**
     * Get reference signal signal-to-noise ratio
     *
     * @return the RSSNR if available or
     *         {@link android.telephony.CellInfo#UNAVAILABLE UNAVAILABLE} if unavailable.
     */
    val rssnr: Int,

    /**
     * Get the timing advance value for LTE, as a value in range of 0..1282.
     * {@link android.telephony.CellInfo#UNAVAILABLE UNAVAILABLE} is reported when there is no
     * active RRC connection. Refer to 3GPP 36.213 Sec 4.2.3
     *
     * @return the LTE timing advance if available or
     *         {@link android.telephony.CellInfo#UNAVAILABLE UNAVAILABLE} if unavailable.
     */
    val timingAdvance: Int

) : SignalStrengthInfo(transport, value, rsrq, signalLevel, min, max, timestampNanos)