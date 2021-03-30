package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemHistoryBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.History

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.Holder>() {

    private val _items = mutableListOf<History>()
    var actionCallback: ((History) -> Unit)? = null

    var items: List<History>
        get() = _items
        set(value) {
            _items.clear()
            _items.addAll(value)
            notifyDataSetChanged()
        }

    override fun getItemCount() = _items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(parent.bindWith(R.layout.item_history))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = _items[position]
        holder.binding.item = item
        holder.binding.root.setOnClickListener {
            actionCallback?.invoke(item)
        }
    }

    class Holder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}
