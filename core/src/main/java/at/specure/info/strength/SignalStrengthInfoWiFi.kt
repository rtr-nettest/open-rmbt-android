package at.specure.info.strength

import at.specure.info.TransportType

class SignalStrengthInfoWiFi(
    transport: TransportType,
    value: Int?,
    rsrq: Int?,
    signalLevel: Int,
    min: Int,
    max: Int,

    /**
     * The current link speed in Mbps.
     */
    val linkSpeed : Int
) :
    SignalStrengthInfo(transport, value, rsrq, signalLevel, min, max)