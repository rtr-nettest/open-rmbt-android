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
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemTestResultDetailBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.TestResultDetailsRecord

class TestResultDetailAdapter : RecyclerView.Adapter<TestResultDetailAdapter.Holder>() {

    var items: MutableList<TestResultDetailsRecord> = mutableListOf()
        set(value) {
            _items = value
            field = value
            notifyDataSetChanged()
        }
    private var _items = mutableListOf<TestResultDetailsRecord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(parent.bindWith(R.layout.item_test_result_detail))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.item = _items[position]
    }

    override fun getItemCount() = _items.size

    class Holder(val binding: ItemTestResultDetailBinding) : RecyclerView.ViewHolder(binding.root)
}