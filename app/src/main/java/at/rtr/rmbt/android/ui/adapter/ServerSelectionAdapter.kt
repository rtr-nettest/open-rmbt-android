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
import at.rmbt.client.control.Server
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemServerSelectionBinding
import at.rtr.rmbt.android.util.bindWith

class ServerSelectionAdapter : RecyclerView.Adapter<ServerSelectionAdapter.Holder>() {

    private var items = mutableListOf<Server>()
    var selectedServer: String? = null
    var actionCallback: ((Server) -> Unit)? = null

    fun init(items: List<Server>?, selectedServer: String?) {
        items?.let {
            this.items.clear()
            this.items.addAll(it)
        }
        this.selectedServer = selectedServer
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(parent.bindWith(R.layout.item_server_selection))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.item = item

        if (selectedServer == null && position == 0) {
            holder.binding.radioButtonServerName.isChecked = true
        } else holder.binding.radioButtonServerName.isChecked = item.uuid.equals(selectedServer)

        holder.binding.root.setOnClickListener {
            actionCallback?.invoke(item)
        }
    }
    class Holder(val binding: ItemServerSelectionBinding) : RecyclerView.ViewHolder(binding.root)
}
