package at.rtr.rmbt.android.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHistoryBinding
import at.rtr.rmbt.android.databinding.FragmentResultsListBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.ResultsActivity
import at.rtr.rmbt.android.ui.adapter.FilterLabelAdapter
import at.rtr.rmbt.android.ui.adapter.HistoryLoopAdapter
import at.rtr.rmbt.android.ui.dialog.HistoryFiltersDialog
import at.rtr.rmbt.android.ui.dialog.SyncDevicesDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HistoryViewModel

class ResultsListFragment  : BaseFragment(), SyncDevicesDialog.Callback {

    private val historyViewModel: HistoryViewModel by viewModelLazy()
    private val binding: FragmentResultsListBinding by bindingLazy()
    private val adapter: HistoryLoopAdapter by lazy { HistoryLoopAdapter() }

    override val layoutResId = R.layout.fragment_results_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = historyViewModel.state
        binding.recyclerViewHistoryItems.adapter = adapter

        adapter.actionCallback = {
            ResultsActivity.start(requireContext(), it.testUUID, ResultsActivity.ReturnPoint.HISTORY)
        }

        binding.recyclerViewHistoryItems.apply {
            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(context, R.drawable.history_item_divider)?.let {
                itemDecoration.setDrawable(it)
            }
            binding.recyclerViewHistoryItems.addItemDecoration(itemDecoration)
        }

        historyViewModel.historyLiveData.listen(this) {
            historyViewModel.state.isHistoryEmpty.set(it.isEmpty())

            adapter.submitList(it)
        }

        historyViewModel.isLoadingLiveData.listen(this) {
            historyViewModel.state.isLoadingLiveData.set(it)
        }

        binding.swipeRefreshLayoutHistoryItems.setOnRefreshListener {
            refreshHistory()
        }

        historyViewModel.isLoadingLiveData.listen(this) {
            binding.swipeRefreshLayoutHistoryItems.isRefreshing = it
        }

        refreshHistory()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        refreshHistory()
    }

    private fun refreshHistory() {
        historyViewModel.refreshHistory()
        binding.swipeRefreshLayoutHistoryItems.isRefreshing = false
    }

    override fun onDevicesSynced() {
        refreshHistory()
    }
}