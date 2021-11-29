package at.specure.info.network

import at.specure.info.cell.CellNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import cz.mroczis.netmonster.core.model.cell.ICell

class DetailedNetworkInfo(
    val networkInfo: NetworkInfo?,
    val signalStrengthInfo: SignalStrengthInfo? = null,
    /**
     * Contains network types for subscriptionId to optimize requests to netmonster lib (determining for each ICell is costly)
     */
    val networkTypes: HashMap<Int, MobileNetworkType> = HashMap(),
    /**
     * Should contain all inactive cells available for primary data connection
     */

    val allCellInfos: List<ICell>? = null,
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
    var secondary5GActiveSignalStrengthInfos: List<SignalStrengthInfo?>? = null,

    /**
     * Current primary data subscription ID, -1 when unknown for some reason (wrong API, denied permissions)
     */
    val dataSubscriptionId: Int
)