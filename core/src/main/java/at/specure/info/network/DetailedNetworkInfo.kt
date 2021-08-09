package at.specure.info.network

import at.specure.info.cell.CellNetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import cz.mroczis.netmonster.core.model.cell.ICell

class DetailedNetworkInfo(
    val networkInfo: NetworkInfo?,
    val signalStrengthInfo: SignalStrengthInfo? = null,
    /**
     * Should contain all inactive cells available for primary data connection
     */

    val allCellInfos: List<ICell?>? = null,
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