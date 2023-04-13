package at.rtr.rmbt.android.ui.adapter

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemCellInfo2gBinding
import at.rtr.rmbt.android.databinding.ItemCellInfo3gBinding
import at.rtr.rmbt.android.databinding.ItemCellInfoLteBinding
import at.rtr.rmbt.android.databinding.ItemCellInfoNrBinding
import at.rtr.rmbt.android.databinding.ItemCellInfoWifiBinding
import at.rtr.rmbt.android.databinding.ItemHistoryBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.CellTechnology
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import cz.mroczis.netmonster.core.model.cell.CellCdma
import cz.mroczis.netmonster.core.model.cell.CellGsm
import cz.mroczis.netmonster.core.model.cell.CellLte
import cz.mroczis.netmonster.core.model.cell.CellNr
import cz.mroczis.netmonster.core.model.cell.CellTdscdma
import cz.mroczis.netmonster.core.model.cell.CellWcdma

private const val WIFI_NETWORK_INFO_TYPE = 0
private const val CELL_NR_NETWORK_INFO_TYPE = 1
private const val CELL_LTE_NETWORK_INFO_TYPE = 2
private const val CELL_3G_NETWORK_INFO_TYPE = 3
private const val CELL_2G_NETWORK_INFO_TYPE = 4

class NetworkInfoAdapter : RecyclerView.Adapter<NetworkInfoAdapter.Holder>() {

    private val _items = mutableListOf<NetworkInfo?>()

    var items: List<NetworkInfo?>
        get() = _items
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            _items.clear()
            _items.addAll(value)
            notifyDataSetChanged()
        }

    override fun getItemCount() = _items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is WifiNetworkInfo -> WIFI_NETWORK_INFO_TYPE
            is CellNetworkInfo -> {
                when ((items[position] as CellNetworkInfo).cellType) {
                    CellTechnology.CONNECTION_5G -> CELL_NR_NETWORK_INFO_TYPE
                    CellTechnology.CONNECTION_4G -> CELL_LTE_NETWORK_INFO_TYPE
                    CellTechnology.CONNECTION_3G -> CELL_3G_NETWORK_INFO_TYPE
                    else -> CELL_2G_NETWORK_INFO_TYPE
                }
            }
            else -> CELL_2G_NETWORK_INFO_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return when (viewType) {
            WIFI_NETWORK_INFO_TYPE -> WifiHolder(parent.bindWith(R.layout.item_cell_info_wifi))
            CELL_NR_NETWORK_INFO_TYPE -> CellNrHolder(parent.bindWith(R.layout.item_cell_info_nr))
            CELL_LTE_NETWORK_INFO_TYPE -> CellLteHolder(parent.bindWith(R.layout.item_cell_info_lte))
            // TODO: fix for 3G and 2G and unknown
            CELL_3G_NETWORK_INFO_TYPE -> Cell3GHolder(parent.bindWith(R.layout.item_cell_info_3g))
            CELL_2G_NETWORK_INFO_TYPE -> Cell2GHolder(parent.bindWith(R.layout.item_cell_info_2g))
            else -> Cell2GHolder(parent.bindWith(R.layout.item_cell_info_2g))
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = _items[position]
        holder.bind(position, item)
    }

    class Cell2GHolder(val binding: ItemCellInfo2gBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: NetworkInfo?
        ) {
            if (item is CellNetworkInfo) {
                binding.networkType2g =
                    "${item.cellType.displayName} (${item.networkType.displayName})"
                binding.band2g = item.band?.name ?: item.band?.informalName
                binding.arfcn2g = item.band?.channel?.toString()

                if (item.rawCellInfo is CellCdma) {
                    val rawCellInfo = item.rawCellInfo as CellCdma
                    binding.cid2g = rawCellInfo.bid?.toString()

                    rawCellInfo.signal.cdmaRssi?.let {
                        binding.rssi2g = "$it dBm"
                    }
                } else if (item.rawCellInfo is CellGsm) {
                    val rawCellInfo = item.rawCellInfo as CellGsm
                    binding.bsic2g = rawCellInfo.bsic?.toString()
                    binding.cid2g = rawCellInfo.cid?.toString()
                    binding.lac2g = rawCellInfo.lac?.toString()
                    rawCellInfo.signal.rssi?.let {
                        binding.rssi2g = "$it dBm"
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
            item: NetworkInfo?
        ) {
            if (item is CellNetworkInfo) {
                binding.networkType3g =
                    "${item.cellType.displayName} (${item.networkType.displayName})"
                binding.band3g = item.band?.name ?: item.band?.informalName
                binding.uarfcn3g = item.band?.channel?.toString()

                if (item.rawCellInfo is CellWcdma) {
                    val rawCellInfo = item.rawCellInfo as CellWcdma
//                    binding.bw3g = rawCellInfo.band?.toString()
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
                } else if (item.rawCellInfo is CellTdscdma) {
                    val rawCellInfo = item.rawCellInfo as CellTdscdma
//                    binding.bw3g = rawCellInfo.band?.toString()
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
            item: NetworkInfo?
        ) {
            if (item is CellNetworkInfo) {
                binding.networkTypeLTE = "${item.cellType.displayName} (${item.networkType.displayName})"
                binding.bandLTE = item.band?.name ?: item.band?.informalName
                binding.earfcnLTE = item.band?.channel?.toString()

                if (item.rawCellInfo is CellLte) {
                    val rawCellInfo = item.rawCellInfo as CellLte
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
            item: NetworkInfo?
        ) {
            if (item is CellNetworkInfo) {
                binding.bandNR = item.band?.name ?: item.band?.informalName
                binding.arfcnNR = item.band?.frequencyDL?.toInt().toString()
                binding.networkTypeNR = "${item.cellType.displayName} (${item.networkType.displayName})"

            }
        }
    }

    class WifiHolder(val binding: ItemCellInfoWifiBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: NetworkInfo?
        ) {
            if (item is WifiNetworkInfo) {
                val band = (item as WifiNetworkInfo).band
                binding.wifiFrequency = "${band.channelNumber} (${band.frequency} MHz)"
                binding.wifiBandName = band.informalName
                binding.wifiSignal = item.signal
                binding.wifiBssid = item.bssid
                binding.wifiSsid = item.ssid

                if (item.rxlinkSpeed != null) {
                    binding.wifiRxLinkSpeed = item.rxlinkSpeed.toString() + " MBit/s"
                }
                else
                    binding.wifiRxLinkSpeed = null

                if (item.txlinkSpeed != null) {
                    binding.wifiTxLinkSpeed = item.txlinkSpeed.toString() + " MBit/s"
                }
                else
                    binding.wifiTxLinkSpeed = null
            }
        }
    }

    abstract class Holder(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(
            position: Int,
            item: NetworkInfo?
        )
    }

    class HolderCellNR(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
    class HolderCellLTE(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
    class HolderCell3G(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
    class HolderCell2G(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}
