package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemFilterLabelBinding
import at.rtr.rmbt.android.util.bindWith

class FilterLabelAdapter(private val onRemove: (String) -> Unit) : RecyclerView.Adapter<FilterLabelAdapter.Holder>() {

    private val _items = mutableListOf<String>()

    var items: Set<String>
        get() = _items.toSet()
        set(value) {
            _items.clear()
            _items.addAll(value)
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(parent.bindWith(R.layout.item_filter_label))

    override fun getItemCount() = _items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = _items[position]
        holder.binding.label.text = item
        holder.binding.remove.setOnClickListener { onRemove.invoke(item) }
    }

    class Holder(val binding: ItemFilterLabelBinding) : RecyclerView.ViewHolder(binding.root)
}