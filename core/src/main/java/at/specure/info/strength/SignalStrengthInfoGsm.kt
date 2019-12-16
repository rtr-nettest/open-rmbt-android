package at.specure.info.strength

import at.specure.info.TransportType

class SignalStrengthInfoGsm(
    transport: TransportType,
    value: Int?,
    rsrq: Int?,
    signalLevel: Int,
    min: Int,
    max: Int,
    timestampNanos: Long,

    /**
     * Return the Bit Error Rate
     *
     * @return the bit error rate (0-7, 99) as defined in TS 27.007 8.5 or
     *         {@link android.telephony.CellInfo#UNAVAILABLE UNAVAILABLE}.
     */
    val bitErrorRate: Int?,

    /**
     * Get the GSM timing advance between 0..219 symbols (normally 0..63).
     * <p>{@link android.telephony.CellInfo#UNAVAILABLE UNAVAILABLE} is reported when there is no RR
     * connection. Refer to 3GPP 45.010 Sec 5.8.
     *
     * @return the current GSM timing advance, if available.
     */
    val timingAdvance: Int?

) : SignalStrengthInfo(transport, value, rsrq, signalLevel, min, max, timestampNanos)