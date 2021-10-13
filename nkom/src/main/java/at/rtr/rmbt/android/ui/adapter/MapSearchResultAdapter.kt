package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemMapSearchResultBinding
import at.rtr.rmbt.android.util.bindWith
import at.rtr.rmbt.android.util.model.MapSearchResult

class MapSearchResultAdapter : RecyclerView.Adapter<MapSearchResultAdapter.Holder>() {

    var items: MutableList<MapSearchResult> = mutableListOf()
        set(value) {
            _items = value
            field = value
            notifyDataSetChanged()
        }
    private var _items = mutableListOf<MapSearchResult>()
    var actionCallback: ((MapSearchResult) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(parent.bindWith(R.layout.item_map_search_result))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = _items[position]
        holder.binding.item = item
        holder.binding.root.setOnClickListener {
            actionCallback?.invoke(item)
        }
    }

    override fun getItemCount() = _items.size

    class Holder(val binding: ItemMapSearchResultBinding) : RecyclerView.ViewHolder(binding.root)
}