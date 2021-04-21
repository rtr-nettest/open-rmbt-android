package cz.mroczis.netmonster.core.util

import cz.mroczis.netmonster.core.model.cell.CellCdma
import cz.mroczis.netmonster.core.model.cell.CellGsm
import cz.mroczis.netmonster.core.model.cell.CellLte
import cz.mroczis.netmonster.core.model.cell.CellNr
import cz.mroczis.netmonster.core.model.cell.CellTdscdma
import cz.mroczis.netmonster.core.model.cell.CellWcdma
import cz.mroczis.netmonster.core.model.cell.ICell
import cz.mroczis.netmonster.core.model.cell.ICellProcessor

/**
 * Changes subscription id to desired one
 */
class SubscriptionModifier(
    private val targetSub: Int
) : ICellProcessor<ICell> {
    override fun processCdma(cell: CellCdma): ICell = cell.copy(subscriptionId = targetSub)
    override fun processGsm(cell: CellGsm): ICell = cell.copy(subscriptionId = targetSub)
    override fun processLte(cell: CellLte): ICell = cell.copy(subscriptionId = targetSub)
    override fun processNr(cell: CellNr): ICell = cell.copy(subscriptionId = targetSub)
    override fun processTdscdma(cell: CellTdscdma): ICell =
        cell.copy(subscriptionId = targetSub)

    override fun processWcdma(cell: CellWcdma): ICell = cell.copy(subscriptionId = targetSub)
}