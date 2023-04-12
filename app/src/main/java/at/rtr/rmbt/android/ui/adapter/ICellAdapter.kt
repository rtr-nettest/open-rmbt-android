package at.rtr.rmbt.android.ui.adapter

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemCellInfo2gBinding
import at.rtr.rmbt.android.databinding.ItemCellInfo3gBinding
import at.rtr.rmbt.android.databinding.ItemCellInfoLteBinding
import at.rtr.rmbt.android.databinding.ItemCellInfoNrBinding
import at.rtr.rmbt.android.databinding.ItemHistoryBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.info.cell.CellTechnology
import at.specure.util.getEuBand
import at.specure.util.toTechnologyClass
import cz.mroczis.netmonster.core.model.cell.CellCdma
import cz.mroczis.netmonster.core.model.cell.CellGsm
import cz.mroczis.netmonster.core.model.cell.CellLte
import cz.mroczis.netmonster.core.model.cell.CellNr
import cz.mroczis.netmonster.core.model.cell.CellTdscdma
import cz.mroczis.netmonster.core.model.cell.CellWcdma
import cz.mroczis.netmonster.core.model.cell.ICell

private const val CELL_NR_NETWORK_INFO_TYPE = 1
private const val CELL_LTE_NETWORK_INFO_TYPE = 2
private const val CELL_3G_NETWORK_INFO_TYPE = 3
private const val CELL_2G_NETWORK_INFO_TYPE = 4

class ICellAdapter : RecyclerView.Adapter<ICellAdapter.Holder>() {

    var items: List<ICell> = emptyList()
        set(value) {
            Handler(Looper.getMainLooper())
                .post {
                    field = value
                    notifyDataSetChanged()
                }
        }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        val itemTechnology = items[position].toTechnologyClass()
        return when (itemTechnology) {
            CellTechnology.CONNECTION_5G -> CELL_NR_NETWORK_INFO_TYPE
            CellTechnology.CONNECTION_4G -> CELL_LTE_NETWORK_INFO_TYPE
            CellTechnology.CONNECTION_3G -> CELL_3G_NETWORK_INFO_TYPE
            else -> CELL_2G_NETWORK_INFO_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return when (viewType) {
            CELL_NR_NETWORK_INFO_TYPE -> CellNrHolder(parent.bindWith(R.layout.item_cell_info_nr))
            CELL_LTE_NETWORK_INFO_TYPE -> CellLteHolder(parent.bindWith(R.layout.item_cell_info_lte))
            CELL_3G_NETWORK_INFO_TYPE -> Cell3GHolder(parent.bindWith(R.layout.item_cell_info_3g))
            CELL_2G_NETWORK_INFO_TYPE -> Cell2GHolder(parent.bindWith(R.layout.item_cell_info_2g))
            else -> Cell2GHolder(parent.bindWith(R.layout.item_cell_info_2g))
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.bind(position, item)
    }

    class Cell2GHolder(val binding: ItemCellInfo2gBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: ICell?
        ) {
            if (item is ICell) {
                binding.subscriptionId2g = item.subscriptionId.toString() + " (${item.network?.toPlmn("-")})"
                binding.band2g = item.band?.name ?: ""
                binding.arfcn2g = item.band?.channelNumber?.toString()

                binding.cid2g = null
                binding.networkType2g = null
                binding.rxl2g = null
                binding.bsic2g = null
                binding.lac2g = null
                binding.ta2g = null

                if (item is CellCdma) {
                    val rawCellInfo = item as CellCdma
                    binding.cid2g = rawCellInfo.bid?.toString()
                    binding.networkType2g = "2G (CDMA)"

                    rawCellInfo.signal.cdmaRssi?.let {
                        binding.rxl2g = "$it dBm"
                    }
                } else if (item is CellGsm) {
                    val rawCellInfo = item as CellGsm
                    binding.networkType2g = "2G (GSM)"
                    binding.bsic2g = rawCellInfo.bsic?.toString()
                    binding.cid2g = rawCellInfo.cid?.toString()
                    binding.lac2g = rawCellInfo.lac?.toString()
                    rawCellInfo.signal.rssi?.let {
                        binding.rxl2g = "$it dBm"
                    }
                    rawCellInfo.signal.timingAdvance?.let {
                        binding.ta2g = "$it (${it * 554}m)"
                    }
                }
            }
        }
    }

    class Cell3GHolder(val binding: ItemCellInfo3gBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: ICell?
        ) {
            if (item is ICell) {
                binding.subscriptionId3g = item.subscriptionId.toString() + " (${item.network?.toPlmn("-")})"
                binding.band3g = item.band?.name ?: ""
                binding.uarfcn3g = item.band?.channelNumber?.toString()
                binding.subscriptionId3g = item.subscriptionId.toString()

                binding.networkType3g = null
                binding.ci3g = null
                binding.cid3g = null
                binding.lac3g = null
                binding.psc3g = null
                binding.rscp3g = null
                binding.rssi3g = null
                binding.rnc3g = null

                if (item is CellWcdma) {
                    binding.networkType3g = "3G (UMTS)"
                    val rawCellInfo = item as CellWcdma
                    binding.ci3g = rawCellInfo.ci?.toString()
                    binding.cid3g = rawCellInfo.cid?.toString()
                    binding.lac3g = rawCellInfo.lac?.toString()
                    binding.psc3g = rawCellInfo.psc?.toString()
                    rawCellInfo.signal.rscp?.let {
                        binding.rscp3g = "$it dBm"
                    }
                    rawCellInfo.signal.rssi?.let {
                        binding.rssi3g = "$it dBm"
                    }
                    binding.rnc3g = rawCellInfo.rnc?.toString()
                } else if (item is CellTdscdma) {
                    val rawCellInfo = item as CellTdscdma
                    binding.networkType3g = "3G (TDS-CDMA)"
                    binding.ci3g = rawCellInfo.ci?.toString()
                    binding.cid3g = rawCellInfo.cid?.toString()
                    binding.lac3g = rawCellInfo.lac?.toString()
                    rawCellInfo.signal.rscp?.let {
                        binding.rscp3g = "$it dBm"
                    }
                    rawCellInfo.signal.rssi?.let {
                        binding.rssi3g = "$it dBm"
                    }

                    binding.rnc3g = rawCellInfo.rnc?.toString()
                }
            }
        }
    }

    class CellLteHolder(val binding: ItemCellInfoLteBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: ICell?
        ) {
            if (item is ICell) {
                binding.subscriptionId4g = item.subscriptionId.toString() + " (${item.network?.toPlmn("-")})"
                binding.bandLTE = item.band?.name ?: ""
                binding.earfcnLTE = item.band?.channelNumber?.toString()
                binding.networkTypeLTE = "4G (LTE)"

                binding.bwLTE = null
                binding.ciLTE = null
                binding.cidLTE = null
                binding.enbLTE = null
                binding.pciLTE = null
                binding.rsrpLTE = null
                binding.rsrqLTE = null
                binding.rssiLTE = null
                binding.snrLTE = null
                binding.taLTE = null
                binding.tacLTE = null

                if (item is CellLte) {
                    val rawCellInfo = item as CellLte
                    binding.bwLTE = rawCellInfo.bandwidth?.toString()
                    binding.ciLTE = rawCellInfo.eci?.toString()
                    binding.cidLTE = rawCellInfo.cid?.toString()
                    binding.enbLTE = rawCellInfo.enb?.toString()

                    binding.pciLTE = rawCellInfo.pci?.toString()

                    rawCellInfo.signal.rsrp?.let {
                        binding.rsrpLTE = "${it.toInt()} dBm"
                    }

                    rawCellInfo.signal.rsrq?.let {
                        binding.rsrqLTE = "${it.toInt()} dB"
                    }

                    rawCellInfo.signal.rssi?.let {
                        binding.rssiLTE = "$it dBm"
                    }

                    rawCellInfo.signal.snr?.let {
                        binding.snrLTE = "${it.toInt()} dB"
                    }

                    rawCellInfo.signal.timingAdvance?.let {
                        if (it > 0) {
                            binding.taLTE = "$it (${it * 78}m)"
                        }
                    }

                    binding.tacLTE = rawCellInfo.tac?.toString()
                }
            }
        }
    }

    class CellNrHolder(val binding: ItemCellInfoNrBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: ICell?
        ) {
            if (item is CellNr) {
                val bandNrEu = item.getEuBand()
                binding.bandNameNr = bandNrEu?.name ?: ""
                binding.frequencyNr = bandNrEu?.channelNumber?.toString()
                binding.subscriptionId5g = item.subscriptionId.toString() + " (${item.network?.toPlmn("-")})"
                binding.networkTypeNR = "5G (NR)"

                binding.signalSsrsrpNr = null
                binding.signalSsrsrqNr = null

                val rawCellInfo = item as CellNr
                rawCellInfo.signal.ssRsrp?.let {
                    binding.signalSsrsrpNr = "$it dBm"
                }
                rawCellInfo.signal.ssRsrq?.let {
                    binding.signalSsrsrqNr = "$it dB"
                }
            }
        }
    }

    abstract class Holder(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(
            position: Int,
            item: ICell?
        )
    }

    class HolderCellNR(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
    class HolderCellLTE(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
    class HolderCell3G(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
    class HolderCell2G(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}
