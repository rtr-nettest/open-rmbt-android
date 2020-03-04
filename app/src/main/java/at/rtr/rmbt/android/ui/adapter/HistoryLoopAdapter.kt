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

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewPropertyAnimator
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemHistoryBinding
import at.rtr.rmbt.android.databinding.ItemHistoryLoopBinding
import at.rtr.rmbt.android.util.bindWith
import at.rtr.rmbt.android.util.gone
import at.rtr.rmbt.android.util.visible
import at.specure.data.entity.History
import at.specure.data.entity.HistoryContainer

private const val ITEM_LOOP = 0
private const val ITEM_HISTORY = 1
private const val KEY_STATE = "KEY_STATE"

class HistoryLoopAdapter : PagedListAdapter<HistoryContainer, HistoryLoopAdapter.Holder>(DIFF_CALLBACK) {

    private val expandedItemsMap = mutableMapOf<Int, Boolean>()

    var actionCallback: ((History) -> Unit)? = null
    var pendingAnimationCallback: (() -> Unit)? = null

    override fun getItemViewType(position: Int): Int {
        val size = getItem(position)?.items?.size ?: 1
        return if (size == 1) {
            ITEM_HISTORY
        } else {
            ITEM_LOOP
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = if (viewType == ITEM_LOOP) {
        LoopHolder(parent.bindWith(R.layout.item_history_loop))
    } else {
        HistoryHolder(parent.bindWith(R.layout.item_history))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        getItem(position)?.let { item ->
            holder.bind(position, item, expandedItemsMap, actionCallback, pendingAnimationCallback)
        }
    }

    @Suppress("UseSparseArrays")
    fun onSaveInstanceState(outState: Bundle) {
        val map = HashMap<Int, Boolean>()
        map.putAll(expandedItemsMap)
        outState.putSerializable(KEY_STATE, map)
    }

    @Suppress("UNCHECKED_CAST")
    fun onRestoreState(inState: Bundle) {
        inState.getSerializable(KEY_STATE)?.let {
            expandedItemsMap.putAll(it as HashMap<Int, Boolean>)
        }
    }

    fun onClearState() {
        expandedItemsMap.clear()
    }

    class LoopHolder(val binding: ItemHistoryLoopBinding) : Holder(binding.root) {

        private var animation: ViewPropertyAnimator? = null
        private val adapter = HistoryAdapter()

        init {
            binding.recyclerView.layoutManager = LinearLayoutManager(binding.recyclerView.context)
            binding.recyclerView.adapter = adapter
        }

        override fun bind(
            position: Int,
            item: HistoryContainer,
            expandedItemsMap: MutableMap<Int, Boolean>,
            actionCallback: ((History) -> Unit)?,
            pendingAnimationCallback: (() -> Unit)?
        ) {
            if (item.items.isEmpty()) {
                return
            }
            binding.item = item.items.last()

            animation?.cancel()

            adapter.items = item.items
            adapter.actionCallback = actionCallback

            val isExpanded = expandedItemsMap[position] ?: false

            if (isExpanded) {
                binding.imageExpand.rotation = 180f
                binding.recyclerView.visible()
            } else {
                binding.imageExpand.rotation = 0f
                binding.recyclerView.gone()
            }

            binding.root.setOnClickListener {
                val expanded = expandedItemsMap[position] ?: false
                expandedItemsMap[position] = !expanded

                val anim = binding.imageExpand.animate()
                if (expanded) {
                    anim.rotation(0f)
                    binding.recyclerView.gone()
                } else {
                    anim.rotation(180f)
                    binding.recyclerView.visible()
                }
                pendingAnimationCallback?.invoke()
                animation = anim
                anim.start()
            }
        }
    }

    class HistoryHolder(val binding: ItemHistoryBinding) : Holder(binding.root) {

        override fun bind(
            position: Int,
            item: HistoryContainer,
            expandedItemsMap: MutableMap<Int, Boolean>,
            actionCallback: ((History) -> Unit)?,
            pendingAnimationCallback: (() -> Unit)?
        ) {
            binding.item = item.items.first()
            binding.root.setOnClickListener {
                actionCallback?.invoke(item.items.first())
            }
        }
    }

    abstract class Holder(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(
            position: Int,
            item: HistoryContainer,
            expandedItemsMap: MutableMap<Int, Boolean>,
            actionCallback: ((History) -> Unit)?,
            pendingAnimationCallback: (() -> Unit)?
        )
    }

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<HistoryContainer>() {

            override fun areItemsTheSame(oldItem: HistoryContainer, newItem: HistoryContainer) =
                oldItem.reference.uuid == newItem.reference.uuid

            override fun areContentsTheSame(oldItem: HistoryContainer, newItem: HistoryContainer) =
                oldItem.reference.uuid == newItem.reference.uuid &&
                        oldItem.reference.time == newItem.reference.time &&
                        oldItem.items.size == newItem.items.size
        }
    }
}