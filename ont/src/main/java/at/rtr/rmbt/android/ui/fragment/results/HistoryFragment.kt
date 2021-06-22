package at.rtr.rmbt.android.ui.fragment.results

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHistoryBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.LoopMeasurementResultsActivity
import at.rtr.rmbt.android.ui.activity.ResultsActivity
import at.rtr.rmbt.android.ui.adapter.HistoryLoopAdapter
import at.rtr.rmbt.android.ui.fragment.BaseFragment
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HistoryViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

class HistoryFragment : BaseFragment() {

    private val historyViewModel: HistoryViewModel by viewModelLazy()
    private val binding: FragmentHistoryBinding by bindingLazy()
    private val adapter: HistoryLoopAdapter by lazy { HistoryLoopAdapter() }

    override val layoutResId = R.layout.fragment_history

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = historyViewModel.state
        binding.recyclerViewHistoryItems.adapter = adapter

        adapter.simpleClickChannel.receiveAsFlow().onEach {
            ResultsActivity.start(requireContext(), it, ResultsActivity.ReturnPoint.HISTORY)
        }.launchIn(lifecycleScope)

        adapter.loopClickChannel.receiveAsFlow().onEach {
            LoopMeasurementResultsActivity.start(requireContext(), it)
        }.launchIn(lifecycleScope)

        binding.recyclerViewHistoryItems.apply {
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(context, R.drawable.history_item_divider)?.let {
                itemDecoration.setDrawable(it)
            }
            binding.recyclerViewHistoryItems.addItemDecoration(itemDecoration)
        }

        historyViewModel.historyLiveData.listen(this) {
            binding.swipeRefreshLayoutHistoryItems.isRefreshing = false
            historyViewModel.state.isHistoryEmpty.set(it.isEmpty())
            (parentFragment as? ResultsFragment)?.onDataLoaded(it.isEmpty())
            adapter.submitList(it)
        }

        binding.swipeRefreshLayoutHistoryItems.setOnRefreshListener {
            refreshHistory()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshHistory()
    }

    private fun refreshHistory() {
        historyViewModel.refreshHistory()
        binding.swipeRefreshLayoutHistoryItems.isRefreshing = true
    }
}