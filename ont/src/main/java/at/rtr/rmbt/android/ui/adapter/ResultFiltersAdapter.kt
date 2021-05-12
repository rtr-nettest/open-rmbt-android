package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemFilterBinding
import at.rtr.rmbt.android.util.bindWith
import at.rtr.rmbt.android.viewmodel.ResultListFiltersViewModel

class ResultFiltersAdapter : RecyclerView.Adapter<ResultFiltersAdapter.Holder>() {

    var items = mutableListOf<ResultListFiltersViewModel.FilterOption>()
    set(value) {
        field.clear()
        field.addAll(value)
        selected.clear()
        selected.addAll(items.filter { it.selected })
        notifyDataSetChanged()
    }

    var selected: MutableSet<ResultListFiltersViewModel.FilterOption> = LinkedHashSet()
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(parent.bindWith(R.layout.item_filter))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.label.text = item.option
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

    class Holder(val binding: ItemFilterBinding) : RecyclerView.ViewHolder(binding.root)
}