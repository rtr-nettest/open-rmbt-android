package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemFilterConfirmationBinding
import at.rtr.rmbt.android.util.bindWith

class FilterConfirmationAdapter : RecyclerView.Adapter<FilterConfirmationAdapter.Holder>() {

    var items = mutableListOf<String?>()
        set(value) {
            items.clear()
            items.addAll(value)
            notifyDataSetChanged()
        }

    var selected: Int = RecyclerView.NO_POSITION
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(parent.bindWith(R.layout.item_filter_confirmation))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.label.text = items[position]
        holder.binding.selected = selected == position

        holder.binding.root.setOnClickListener {
            selected = position
            notifyDataSetChanged()
        }
    }

    class Holder(val binding: ItemFilterConfirmationBinding) : RecyclerView.ViewHolder(binding.root)
}