package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemFilterConfirmationBinding
import at.rtr.rmbt.android.util.bindWith

class HistoryFilterConfirmationAdapter : RecyclerView.Adapter<HistoryFilterConfirmationAdapter.Holder>() {

    private var items = mutableListOf<String>()

    var selected: MutableSet<String> = LinkedHashSet()
        private set

    fun init(items: MutableList<String>?, selected: MutableSet<String>?) {
        items?.let {
            this.items.clear()
            this.items.addAll(it)
        }
        if (selected.isNullOrEmpty()) {
            this.selected.clear()
            this.selected.addAll(this.items.toSet())
        } else {
            this.selected = selected
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(parent.bindWith(R.layout.item_filter_confirmation))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.label.text = item
        holder.binding.selected = selected.contains(item)

        holder.binding.root.setOnClickListener {
            if (selected.contains(item)) {
                selected.remove(item)
            } else {
                selected.add(item)
            }
            notifyDataSetChanged()
        }
    }

    class Holder(val binding: ItemFilterConfirmationBinding) : RecyclerView.ViewHolder(binding.root)
}