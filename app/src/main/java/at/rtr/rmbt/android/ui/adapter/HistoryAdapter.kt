package at.rtr.rmbt.android.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemHistoryBinding
import at.rtr.rmbt.android.databinding.ItemHistoryFencesBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.History
import at.specure.data.entity.HistoryContainer

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

    override fun getItemViewType(position: Int): Int {
        val historyItem = items[position]
        val isCoverage = historyItem.isCoverageResult == true
        return if (isCoverage) {
                ITEM_COVERAGE_HISTORY
            } else {
                ITEM_HISTORY
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryAdapter.Holder {
        if (viewType == ITEM_COVERAGE_HISTORY) {
         return CoverageHolder(parent.bindWith(R.layout.item_history_fences))
        }
        return SpeedHolder(parent.bindWith(R.layout.item_history))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        item.let { currentItem ->
            holder.bind(position, currentItem, actionCallback)
        }
    }

    abstract class Holder(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(
            position: Int,
            item: History,
            actionCallback: ((History) -> Unit)?,
        )
    }


    class SpeedHolder(val binding: ItemHistoryBinding) : Holder(binding.root) {

        override fun bind(position: Int, item: History, actionCallback: ((History) -> Unit)?) {
            binding.item = item
            binding.root.setOnClickListener {
                actionCallback?.invoke(item)
            }
        }
    }

    class CoverageHolder(val binding: ItemHistoryFencesBinding) : Holder(binding.root) {

        override fun bind(position: Int, item: History, actionCallback: ((History) -> Unit)?) {
            binding.item = item
            binding.root.setOnClickListener {
                actionCallback?.invoke(item)
            }
        }
    }
}
