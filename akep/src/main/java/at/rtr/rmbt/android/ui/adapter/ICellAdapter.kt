package at.rtr.rmbt.android.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemCellInfoBinding
import at.rtr.rmbt.android.databinding.ItemHistoryBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.info.cell.CellTechnology
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
            field = value
            notifyDataSetChanged()
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
            CELL_NR_NETWORK_INFO_TYPE -> CellNrHolder(parent.bindWith(R.layout.item_cell_info))
            CELL_LTE_NETWORK_INFO_TYPE -> CellLteHolder(parent.bindWith(R.layout.item_cell_info))
            CELL_3G_NETWORK_INFO_TYPE -> Cell3GHolder(parent.bindWith(R.layout.item_cell_info))
            CELL_2G_NETWORK_INFO_TYPE -> Cell2GHolder(parent.bindWith(R.layout.item_cell_info))
            else -> Cell2GHolder(parent.bindWith(R.layout.item_cell_info))
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.bind(position, item)
    }

    class Cell2GHolder(val binding: ItemCellInfoBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: ICell?
        ) {
            if (item is ICell) {
                binding.band = "${item.band?.name ?: ""} (${item.band?.channelNumber?.toString()})"

                if (item is CellCdma) {
                    val rawCellInfo = item as CellCdma
                    binding.technology = "2G (CDMA)"
                    rawCellInfo.signal.cdmaRssi?.let {
                        binding.signal = "$it dBm"
                    }
                } else if (item is CellGsm) {
                    val rawCellInfo = item as CellGsm
                    binding.technology = "2G (GSM)"
                    rawCellInfo.signal.rssi?.let {
                        binding.signal = "$it dBm"
                    }
                }
            }
        }
    }

    class Cell3GHolder(val binding: ItemCellInfoBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: ICell?
        ) {
            if (item is ICell) {
                binding.band = "${item.band?.name ?: ""} (${item.band?.channelNumber?.toString()})"

                if (item is CellWcdma) {
                    binding.technology = "3G (W-CDMA)"
                    val rawCellInfo = item as CellWcdma
                    rawCellInfo.signal.rssi?.let {
                        binding.signal = "$it dBm"
                    }
                } else if (item is CellTdscdma) {
                    val rawCellInfo = item as CellTdscdma
                    binding.technology = "3G (TDS-CDMA)"
                    rawCellInfo.signal.rssi?.let {
                        binding.signal = "$it dBm"
                    }
                }
            }
        }
    }

    class CellLteHolder(val binding: ItemCellInfoBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: ICell?
        ) {
            if (item is ICell) {
                binding.band = "${item.band?.name ?: ""} (${item.band?.channelNumber?.toString()})"
                binding.technology = "4G (LTE)"

                if (item is CellLte) {
                    val rawCellInfo = item as CellLte
                    rawCellInfo.signal.rsrp?.let {
                        binding.signal = "${it.toInt()} dBm"
                    }
                }
            }
        }
    }

    class CellNrHolder(val binding: ItemCellInfoBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: ICell?
        ) {
            if (item is ICell) {
                binding.band = "${(item.band?.name ?: "")} (${item.band?.channelNumber?.toString()})"
                binding.technology = "5G (NR)"

                if (item is CellNr) {
                    val rawCellInfo = item as CellNr
                    rawCellInfo.signal.ssRsrp?.let {
                        binding.signal = "$it dBm"
                    }
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
