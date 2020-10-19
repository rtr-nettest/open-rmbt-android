package at.specure.info.cell

import android.telephony.CellInfo

interface ActiveDataCellInfoExtractor {

    fun extractActiveCellInfo(cellInfo: MutableList<CellInfo>): ActiveDataCellInfo
}