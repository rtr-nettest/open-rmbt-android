package at.specure.info.cell

import android.telephony.CellInfo
import at.specure.info.network.NRConnectionState

data class ActiveDataCellInfo(
    val dualSimDecision: String = "",
    val activeDataNetwork: CellNetworkInfo? = null,
    val activeDataNetworkCellInfo: CellInfo? = null,
    val nrConnectionState: NRConnectionState = NRConnectionState.NOT_AVAILABLE,
    val isConsistent: Boolean = true
)