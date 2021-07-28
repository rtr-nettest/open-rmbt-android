package at.specure.info.network

import android.telephony.CellInfo
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.strength.SignalStrengthInfo

class DetailedNetworkInfo(
    val networkInfo: NetworkInfo?,
    val signalStrengthInfo: SignalStrengthInfo? = null,
    /**
     * Should contain all inactive cells available for primary data connection
     */
    val inactiveCellInfos: List<CellNetworkInfo?>? = null,
    /**
     * Should contain all secondary cells available for primary data connection
     */
    val secondaryActiveCellNetworks: List<CellNetworkInfo?>? = null,
    /**
     * Should contain all secondary signals available for primary data connection
     */
    var secondaryActiveSignalStrengthInfos: List<SignalStrengthInfo?>? = null,
    /**
     * Should contain all 5G secondary cells available for primary data connection
     */
    val secondary5GActiveCellNetworks: List<CellNetworkInfo?>? = null,
    /**
     * Should contain all 5G secondary signals available for primary data connection
     */
    var secondary5GActiveSignalStrengthInfos: List<SignalStrengthInfo?>? = null
)