package at.specure.info.strength

import at.specure.info.TransportType

class SignalStrengthInfoNr(
    transport: TransportType,
    value: Int?,
    rsrq: Int?,
    signalLevel: Int,
    min: Int,
    max: Int,

    /**
     * Reference: 3GPP TS 38.215.
     * Range: -140 dBm to -44 dBm.
     * @return CSI reference signal received power, {@link CellInfo#UNAVAILABLE} means unreported
     * value.
     */
    val csiRsrp: Int,

    /**
     * Reference: 3GPP TS 38.215.
     * Range: -20 dB to -3 dB.
     * @return CSI reference signal received quality, {@link CellInfo#UNAVAILABLE} means unreported
     * value.
     */
    val csiRsrq: Int,

    /**
     * Reference: 3GPP TS 38.215 Sec 5.1.*, 3GPP TS 38.133 10.1.16.1
     * Range: -23 dB to 23 dB
     * @return CSI signal-to-noise and interference ratio, {@link CellInfo#UNAVAILABLE} means
     * unreported value.
     */
    val csiSinr: Int,

    /**
     * Reference: 3GPP TS 38.215.
     * Range: -140 dBm to -44 dBm.
     * @return SS reference signal received power, {@link CellInfo#UNAVAILABLE} means unreported
     * value.
     */
    val ssRsrp: Int,

    /**
     * Reference: 3GPP TS 38.215.
     * Range: -20 dB to -3 dB.
     * @return SS reference signal received quality, {@link CellInfo#UNAVAILABLE} means unreported
     * value.
     */
    val ssRsrq: Int,

    /**
     * Reference: 3GPP TS 38.215 Sec 5.1.*, 3GPP TS 38.133 10.1.16.1
     * Range: -23 dB to 40 dB
     * @return SS signal-to-noise and interference ratio, {@link CellInfo#UNAVAILABLE} means
     * unreported value.
     */
    val ssSinr: Int
) : SignalStrengthInfo(transport, value, rsrq, signalLevel, min, max)