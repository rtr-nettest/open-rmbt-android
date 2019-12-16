package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemHistoryBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.History

class HistoryAdapter : PagedListAdapter<History, HistoryAdapter.Holder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(parent.bindWith(R.layout.item_history))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position) ?: return
        // TODO Remove next lines
        with(item) {
            holder.binding.textName.text = buildString {
                append(position)
                append(") ")
                append(networkType.stringValue).append(" ")
                append("Ping: ")
                append(ping).append(" ")
                append("D: ")
                append(speedDownload).append(" ")
                append("U: ")
                append(speedUpload).append(" ")
                append(timeString)
            }
        }
    }

    class Holder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<History>() {

            override fun areItemsTheSame(oldItem: History, newItem: History) =
                oldItem.testUUID == newItem.testUUID

            override fun areContentsTheSame(oldItem: History, newItem: History) =
                oldItem.model == newItem.model &&
                        oldItem.ping == newItem.ping &&
                        oldItem.pingShortest == newItem.pingShortest &&
                        oldItem.speedDownload == newItem.speedDownload &&
                        oldItem.speedUpload == newItem.speedUpload &&
                        oldItem.networkType == newItem.networkType &&
                        oldItem.timeString == newItem.timeString
        }
    }
}