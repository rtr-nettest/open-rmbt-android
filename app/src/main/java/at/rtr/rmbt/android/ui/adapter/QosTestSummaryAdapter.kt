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
import at.rtr.rmbt.android.databinding.ItemQosTestSummaryBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.QosTestItemRecord

class QosTestSummaryAdapter : ListAdapter<QosTestItemRecord, QosTestSummaryAdapter.Holder>(DIFF_CALLBACK) {

    var actionCallback: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(parent.bindWith(R.layout.item_qos_test_summary))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position) ?: return
        holder.binding.item = item
        holder.binding.root.setOnClickListener {
            actionCallback?.invoke(position)
        }
    }

    class Holder(val binding: ItemQosTestSummaryBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<QosTestItemRecord>() {

            override fun areItemsTheSame(oldItem: QosTestItemRecord, newItem: QosTestItemRecord): Boolean {
                return oldItem.qosTestId == newItem.qosTestId
            }

            override fun areContentsTheSame(oldItem: QosTestItemRecord, newItem: QosTestItemRecord): Boolean {
                return oldItem.qosTestId == newItem.qosTestId &&
                        oldItem.category == newItem.category &&
                        oldItem.testNumber == newItem.testNumber &&
                        oldItem.testDescription == newItem.testDescription &&
                        oldItem.language == newItem.language &&
                        oldItem.success == newItem.success &&
                        oldItem.durationNanos == newItem.durationNanos &&
                        oldItem.startTimeNanos == newItem.startTimeNanos
            }
        }
    }
}