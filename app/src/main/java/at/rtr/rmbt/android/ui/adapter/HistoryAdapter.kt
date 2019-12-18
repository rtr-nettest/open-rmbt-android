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
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemHistoryBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.History

class HistoryAdapter : PagedListAdapter<History, HistoryAdapter.Holder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(parent.bindWith(R.layout.item_history))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position) ?: return
        holder.binding.item = item
    }

    class Holder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<History>() {

            override fun areItemsTheSame(oldItem: History, newItem: History) =
                oldItem.testUUID == newItem.testUUID

            override fun areContentsTheSame(oldItem: History, newItem: History) =
                oldItem.model == newItem.model &&
                        oldItem.ping == newItem.ping &&
                        oldItem.pingShortest == newItem.pingShortest &&
                        oldItem.speedDownload == newItem.speedDownload &&
                        oldItem.speedUpload == newItem.speedUpload &&
                        oldItem.networkType == newItem.networkType &&
                        oldItem.timeString == newItem.timeString
        }
    }
}