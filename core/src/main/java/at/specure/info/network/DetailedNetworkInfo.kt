package at.specure.info.network

import android.telephony.CellInfo
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.strength.SignalStrengthInfo

class DetailedNetworkInfo(
    val networkInfo: NetworkInfo?,
    val signalStrengthInfo: SignalStrengthInfo? = null,
    val cellInfos: List<CellInfo>? = null,
    val secondaryNetworks: List<CellNetworkInfo?>? = null,
    var secondarySignalStrengthInfos: List<SignalStrengthInfo?>? = null
)