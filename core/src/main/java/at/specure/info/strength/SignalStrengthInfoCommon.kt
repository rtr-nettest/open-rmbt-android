package at.specure.info.strength

import android.os.Parcelable
import at.specure.info.TransportType
import kotlinx.parcelize.Parcelize

@Parcelize
class SignalStrengthInfoCommon(
    override val transport: TransportType,
    override val value: Int?,
    override val rsrq: Int?,
    override val signalLevel: Int,
    override val min: Int,
    override val max: Int,
    override val timestampNanos: Long,
    override val source: SignalSource
) : SignalStrengthInfo(), Parcelable