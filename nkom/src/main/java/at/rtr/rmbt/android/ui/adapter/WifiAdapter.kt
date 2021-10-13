package at.rtr.rmbt.android.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemCellInfoBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.info.TransportType
import at.specure.info.network.WifiNetworkInfo

private const val WIFI_NETWORK_TYPE = 1

class WifiAdapter : RecyclerView.Adapter<WifiAdapter.Holder>() {

    var items: List<WifiNetworkInfo> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return WIFI_NETWORK_TYPE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return when (viewType) {
            WIFI_NETWORK_TYPE -> WifiHolder(parent.bindWith(R.layout.item_cell_info))
            else -> WifiHolder(parent.bindWith(R.layout.item_cell_info))
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.bind(position, item)
    }

    class WifiHolder(val binding: ItemCellInfoBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: WifiNetworkInfo?
        ) {
            if (item is WifiNetworkInfo) {

                binding.technology = TransportType.WIFI.name
                item.rssi.let {
                    binding.signal = "$it dBm"
                }
                binding.band = "${item.band.channelNumber} (${item.band.frequency} MHz)"
            }
        }
    }

    abstract class Holder(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(
            position: Int,
            item: WifiNetworkInfo?
        )
    }
}
