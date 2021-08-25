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
import at.rtr.rmbt.android.databinding.ItemQosResultBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.QosCategoryRecord

class QosResultAdapter : ListAdapter<QosCategoryRecord, QosResultAdapter.Holder>(DIFF_CALLBACK) {

    var actionCallback: ((QosCategoryRecord) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(parent.bindWith(R.layout.item_qos_result))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position) ?: return
        holder.binding.item = item
        holder.binding.root.setOnClickListener {
            actionCallback?.invoke(item)
        }
    }

    class Holder(val binding: ItemQosResultBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<QosCategoryRecord>() {

            override fun areItemsTheSame(oldItem: QosCategoryRecord, newItem: QosCategoryRecord): Boolean {
                return oldItem.category == newItem.category
            }

            override fun areContentsTheSame(oldItem: QosCategoryRecord, newItem: QosCategoryRecord): Boolean {
                return oldItem.testUUID == newItem.testUUID &&
                        oldItem.category == newItem.category &&
                        oldItem.categoryName == newItem.categoryName &&
                        oldItem.categoryDescription == newItem.categoryDescription &&
                        oldItem.language == newItem.language &&
                        oldItem.successCount == newItem.successCount &&
                        oldItem.failedCount == newItem.failedCount
            }
        }
    }
}