package at.rtr.rmbt.android.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemMarkerDetailsBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.MarkerMeasurementRecord

class MapMarkerDetailsAdapter(private val callback: MarkerDetailsCallback) : RecyclerView.Adapter<MapMarkerDetailsAdapter.Holder>() {

    var items: MutableList<MarkerMeasurementRecord> = mutableListOf()
        set(value) {
            _items = value
            field = value
            notifyDataSetChanged()
        }
    private var _items = mutableListOf<MarkerMeasurementRecord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding: ItemMarkerDetailsBinding = parent.bindWith(R.layout.item_marker_details)
        binding.root.layoutParams.width = (parent.measuredWidth * WIDTH_COEF).toInt()
        if (viewType == TYPE_ONLY) {
            (binding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart = (parent.measuredWidth * (1 - WIDTH_COEF) / 2).toInt()
            binding.iconPrevious.visibility = View.GONE
            binding.iconNext.visibility = View.GONE
        }
        if (viewType == TYPE_FIRST) {
            (binding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.marginStart = (parent.measuredWidth * (1 - WIDTH_COEF) / 2).toInt()
            binding.iconPrevious.visibility = View.GONE
            binding.iconNext.visibility = View.VISIBLE
        }
        if (viewType == TYPE_LAST) {
            (binding.root.layoutParams as? ViewGroup.MarginLayoutParams)?.marginEnd = (parent.measuredWidth * (1 - WIDTH_COEF) / 2).toInt()
            binding.iconNext.visibility = View.GONE
            binding.iconPrevious.visibility = View.VISIBLE
        }
        if (viewType == TYPE_REGULAR) {
            binding.iconPrevious.visibility = View.VISIBLE
            binding.iconNext.visibility = View.VISIBLE
        }
        return Holder(binding)
    }

    override fun getItemViewType(position: Int): Int {
        if (itemCount <= 1) return TYPE_ONLY
        return when (position) {
            0 -> TYPE_FIRST
            itemCount - 1 -> TYPE_LAST
            else -> TYPE_REGULAR
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.item = _items[position]
        holder.binding.iconClose.setOnClickListener { callback.onCloseMarkerDetails() }
        holder.binding.iconNext.setOnClickListener {
            callback.moveToItem((position + 1).coerceAtMost(_items.size - 1))
        }
        holder.binding.iconPrevious.setOnClickListener {
            callback.moveToItem((position - 1).coerceAtLeast(0))
        }
        holder.binding.moreDetails.setOnClickListener { callback.onMoreDetailsClicked(_items[position].openTestUUID) }
    }

    override fun getItemCount() = _items.size

    fun getItem(position: Int): MarkerMeasurementRecord = _items[position]

    class Holder(val binding: ItemMarkerDetailsBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val WIDTH_COEF = 0.9

        const val TYPE_FIRST = 0
        const val TYPE_REGULAR = 1
        const val TYPE_LAST = 2
        const val TYPE_ONLY = 3
    }

    interface MarkerDetailsCallback {
        fun onCloseMarkerDetails()
        fun moveToItem(childIndex: Int)
        fun onMoreDetailsClicked(openTestUUID: String)
    }
}