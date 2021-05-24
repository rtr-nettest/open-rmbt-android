/*
 *
 *  Licensed under the Apache License, Version 2.0 (the “License”);
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an “AS IS” BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemHistoryBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.History

class LoopMeasurementAdapter(private val allowOpenDetails: Boolean = true) : ListAdapter<History, LoopMeasurementAdapter.HistoryHolder>(DIFF_CALLBACK) {
    var actionCallback: ((History) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder = HistoryHolder(parent.bindWith(R.layout.item_history), allowOpenDetails)

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        getItem(position)?.let { item ->
            holder.bind(item, actionCallback)
        }
    }

    class HistoryHolder(val binding: ItemHistoryBinding, private val allowOpenDetails: Boolean) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: History, actionCallback: ((History) -> Unit)?) {
            binding.item = item
            if (allowOpenDetails) {
                binding.root.setOnClickListener {
                    actionCallback?.invoke(item)
                }
            }
        }
    }

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<History>() {

            override fun areItemsTheSame(oldItem: History, newItem: History) =
                oldItem.testUUID == newItem.testUUID

            override fun areContentsTheSame(oldItem: History, newItem: History) =
                oldItem.testUUID == newItem.testUUID
        }
    }
}