package at.rtr.rmbt.android.ui.adapter

import android.annotation.SuppressLint
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
import cz.mroczis.netmonster.core.model.connection.NoneConnection
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection
import cz.mroczis.netmonster.core.model.connection.SecondaryConnection

private const val CELL_NR_NETWORK_INFO_TYPE = 1
private const val CELL_LTE_NETWORK_INFO_TYPE = 2
private const val CELL_3G_NETWORK_INFO_TYPE = 3
private const val CELL_2G_NETWORK_INFO_TYPE = 4

class ICellAdapter : RecyclerView.Adapter<ICellAdapter.Holder>() {

    var items: List<ICell> = emptyList()
        @SuppressLint("NotifyDataSetChanged")
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
                // display PLMN ID if available
                if (item.network != null) {
                    binding.subscriptionId2g = item.subscriptionId.toString() + " (${item.network?.toPlmn("-")})"
                }
                else
                    binding.subscriptionId2g = item.subscriptionId.toString()

                binding.band2g = item.band?.name ?: null
                binding.arfcn2g = item.band?.channelNumber?.toString()

                binding.cid2g = null
                binding.networkType2g = null
                binding.rssi2g = null
                binding.bsic2g = null
                binding.lac2g = null
                binding.ta2g = null

                if (item is CellCdma) {
                    val rawCellInfo = item as CellCdma
                    binding.cid2g = rawCellInfo.bid?.toString()
                    binding.networkType2g = "CDMA"

                    rawCellInfo.signal.cdmaRssi?.let {
                        binding.rssi2g = "${it}dBm"
                    }
                } else if (item is CellGsm) {

                    if (item.band?.name != null) {
                        binding.networkType2g = "GSM "+item.band?.name
                    }
                    else
                        binding.networkType2g = "GSM"

                    // cell connection status
                    if (item.connectionStatus is PrimaryConnection)
                        binding.connectionStatus2g = "Primary"
                    else if (item.connectionStatus is SecondaryConnection)
                        binding.connectionStatus2g = "Secondary"
                    else if (item.connectionStatus is NoneConnection)
                        binding.connectionStatus2g = "Neighbor"
                    else
                        binding.connectionStatus2g = item.connectionStatus.toString()

                    val rawCellInfo = item as CellGsm
                    binding.bsic2g = rawCellInfo.bsic?.toString()
                    binding.cid2g = rawCellInfo.cid?.toString()
                    binding.lac2g = rawCellInfo.lac?.toString()
                    rawCellInfo.signal.rssi?.let {
                        binding.rssi2g = "${it}dBm"
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
                // display PLMN ID if available
                if (item.network != null) {

                    binding.subscriptionId3g = item.subscriptionId.toString() + " (${item.network?.toPlmn("-")})"
                }
                else
                    binding.subscriptionId3g = item.subscriptionId.toString()

                binding.band3g = item.band?.name ?: null


                // ignore null value
                if (item.band?.channelNumber != null) {
                    binding.uarfcn3g = item.band?.channelNumber?.toString()
                }
                else
                    binding.uarfcn3g = null;

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
                    // display PLMN ID if available
                    if (item.network != null) {
                        binding.subscriptionId3g = item.subscriptionId.toString() + " (${item.network?.toPlmn("-")})"
                    }
                    else
                        binding.subscriptionId3g = item.subscriptionId.toString()

                    if (item.band?.name != null) {
                        binding.networkType3g = "UMTS "+item.band?.name
                    }
                    else
                        binding.networkType3g = "UMTS"

                    // cell connection status
                    if (item.connectionStatus is PrimaryConnection)
                        binding.connectionStatus3g = "Primary"
                    else if (item.connectionStatus is SecondaryConnection)
                        binding.connectionStatus3g = "Secondary"
                    else if (item.connectionStatus is NoneConnection)
                        binding.connectionStatus3g = "Neighbor"
                    else
                        binding.connectionStatus3g = item.connectionStatus.toString()

                    val rawCellInfo = item as CellWcdma
                    binding.ci3g = rawCellInfo.ci?.toString()
                    binding.cid3g = rawCellInfo.cid?.toString()
                    binding.lac3g = rawCellInfo.lac?.toString()
                    binding.psc3g = rawCellInfo.psc?.toString()
                    rawCellInfo.signal.rscp?.let {
                        binding.rscp3g = "${it}dBm"
                    }
                    rawCellInfo.signal.rssi?.let {
                        binding.rssi3g = "${it}dBm"
                    }
                    binding.rnc3g = rawCellInfo.rnc?.toString()
                } else if (item is CellTdscdma) {
                    val rawCellInfo = item as CellTdscdma
                    binding.networkType3g = "TD-SCDMA"
                    binding.ci3g = rawCellInfo.ci?.toString()
                    binding.cid3g = rawCellInfo.cid?.toString()
                    binding.lac3g = rawCellInfo.lac?.toString()
                    rawCellInfo.signal.rscp?.let {
                        binding.rscp3g = "${it}dBm"
                    }
                    rawCellInfo.signal.rssi?.let {
                        binding.rssi3g = "${it}dBm"
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

                // display PLMN ID if available
                if (item.network != null) {
                    binding.subscriptionId4g = item.subscriptionId.toString() + " (${item.network?.toPlmn("-")})"
                }
                else
                    binding.subscriptionId4g = item.subscriptionId.toString()

                // cell connection status
                if (item.connectionStatus is PrimaryConnection)
                    binding.connectionStatusLTE = "Primary"
                else if (item.connectionStatus is SecondaryConnection)
                    binding.connectionStatusLTE = "Secondary"
                else if (item.connectionStatus is NoneConnection)
                    binding.connectionStatusLTE = "Neighbor"
                else
                    binding.connectionStatusLTE = item.connectionStatus.toString()

                binding.earfcnLTE = item.band?.channelNumber?.toString()

                if (item.band?.name != null) {
                    binding.networkTypeLTE = "LTE "+item.band?.name
                }
                else
                    binding.networkTypeLTE = "LTE"

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

                    // bandwidth in MHz (rawCellInfo provides kHz)
                    if (rawCellInfo.bandwidth != null) {
                        binding.bwLTE = ((rawCellInfo.bandwidth)?.div(1000))?.toString()+"MHz"
                    }
                    else
                        binding.bwLTE = null;

                    binding.ciLTE = rawCellInfo.eci?.toString()
                    binding.cidLTE = rawCellInfo.cid?.toString()
                    binding.enbLTE = rawCellInfo.enb?.toString()

                    binding.pciLTE = rawCellInfo.pci?.toString()

                    rawCellInfo.signal.rsrp?.let {
                        binding.rsrpLTE = "${it.toInt()}dBm"
                    }

                    rawCellInfo.signal.rsrq?.let {
                        binding.rsrqLTE = "${it.toInt()}dB"
                    }

                    rawCellInfo.signal.rssi?.let {
                        binding.rssiLTE = "${it}dBm"
                    }

                    rawCellInfo.signal.snr?.let {
                        binding.snrLTE = "${it.toInt()}dB"
                    }

                    rawCellInfo.signal.timingAdvance?.let {
                        if (it > 0) {
                            binding.taLTE = "$it (${it * 78}m)"
                        }
                    }

                    binding.tacLTE = rawCellInfo.tac?.toString()

                    rawCellInfo.aggregatedBands.toString()


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
                binding.bandNR = bandNrEu?.name ?: null
                val arfcnNR = bandNrEu?.channelNumber
                binding.arfcnNR = arfcnNR?.toString()

                // workaround for N78 if band is not recognised by bandNREu
                // NR 78 - 620000 to 653333
                if (arfcnNR != null) {
                    if ((binding.bandNR == null) && (binding.arfcnNR != null) && (arfcnNR >= 620000) and (arfcnNR <= 653333)) {
                        binding.bandNR = "3600"
                    }
                }


                // display PLMN ID if available
                if (item.network != null) {
                    binding.subscriptionId5g = item.subscriptionId.toString() + " (${item.network?.toPlmn("-")})"
                }
                else
                    binding.subscriptionId5g = item.subscriptionId.toString()


                //binding.arfcnNR = item.band?.channelNumber?.toString()

                // 4-bit Tracking Area Code
                if (item.tac != null) {
                    binding.pciNR = item.tac.toString()
                }
                else
                    binding.pciNR = null

                // 36-bit NR Cell Identity
                if (item.nci != null) {
                    binding.nciNR = item.nci.toString()
                }
                else
                    binding.nciNR = null

                // 10-bit Physical Cell Id
                if (item.pci != null) {
                    binding.tacNR = item.pci.toString()
                }
                else
                    binding.tacNR = null

                if (item.band?.name != null) {
                    binding.networkTypeNR = "NR "+item.band?.name
                }
                else
                    binding.networkTypeNR = "NR"

                // signal strength
                if (item.signal.ssRsrp != null) {
                    binding.rsrpNR = item.signal.ssRsrp.toString() + "dBm"
                }
                // fallback to CSI when SS is not available
                else if (item.signal.csiRsrp != null) {
                    binding.rsrpNR = item.signal.csiRsrp.toString() + "dBm"
                }
                else
                    binding.rsrpNR = null

                // signal quality
                if (item.signal.ssRsrq != null) {
                    binding.rsrqNR = item.signal.ssRsrq.toString()+ "dB"
                }
                // fallback to CSI when SS is not available
                else if (item.signal.csiRsrq != null) {
                    binding.rsrqNR = item.signal.csiRsrq.toString()+ "dB"
                }
                else
                    binding.rsrqNR = null

                // cell connection status
                if (item.connectionStatus is PrimaryConnection)
                    binding.connectionStatusNR = "Primary"
                else if (item.connectionStatus is SecondaryConnection)
                    binding.connectionStatusNR = "Secondary"
                else if (item.connectionStatus is NoneConnection)
                    binding.connectionStatusNR = "Neighbor"
                else
                    binding.connectionStatusNR = item.connectionStatus.toString()

                // TODO implement NR timing advance
                // https://developer.android.com/reference/android/telephony/CellSignalStrengthNr#getTimingAdvanceMicros()
                // binding.taNR = ...

            }
        }
    }

    abstract class Holder(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(
            position: Int,
            item: ICell?
        )
    }

    //class HolderCellNR(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
    //class HolderCellLTE(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
    //class HolderCell3G(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
    //class HolderCell2G(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}
