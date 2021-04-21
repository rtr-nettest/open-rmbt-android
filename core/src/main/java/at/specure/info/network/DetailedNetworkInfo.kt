package at.specure.info.network

import android.telephony.CellInfo
import at.specure.info.strength.SignalStrengthInfo

class DetailedNetworkInfo(
    val networkInfo: NetworkInfo?,
    val signalStrengthInfo: SignalStrengthInfo?,
    val nrConnectionState: NRConnectionState?,
    val cellInfos: List<CellInfo>?
)