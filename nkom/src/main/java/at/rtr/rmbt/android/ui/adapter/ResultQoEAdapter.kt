package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemQoeBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.QoeInfoRecord

class ResultQoEAdapter : ListAdapter<QoeInfoRecord, ResultQoEAdapter.Holder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(parent.bindWith(R.layout.item_qoe))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position) ?: return
        holder.binding.item = item
    }

    class Holder(val binding: ItemQoeBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<QoeInfoRecord>() {

            override fun areItemsTheSame(oldItem: QoeInfoRecord, newItem: QoeInfoRecord): Boolean {
                return oldItem.category == newItem.category
            }

            override fun areContentsTheSame(oldItem: QoeInfoRecord, newItem: QoeInfoRecord): Boolean {
                return oldItem.category == newItem.category &&
                        oldItem.classification == newItem.classification &&
                        oldItem.percentage == newItem.percentage &&
                        oldItem.testUUID == oldItem.testUUID
            }
        }
    }
}