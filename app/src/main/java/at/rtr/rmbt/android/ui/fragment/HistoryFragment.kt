package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHistoryBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.ResultsActivity
import at.rtr.rmbt.android.ui.adapter.FilterLabelAdapter
import at.rtr.rmbt.android.ui.adapter.HistoryLoopAdapter
import at.rtr.rmbt.android.ui.dialog.HistoryDownloadDialog
import at.rtr.rmbt.android.ui.dialog.HistoryFiltersDialog
import at.rtr.rmbt.android.ui.dialog.SyncDevicesDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HistoryViewModel

private const val CODE_FILTERS = 13
private const val CODE_DOWNLOAD = 14

class HistoryFragment : BaseFragment(), SyncDevicesDialog.Callback, HistoryFiltersDialog.Callback {

    private val historyViewModel: HistoryViewModel by viewModelLazy()
    private val binding: FragmentHistoryBinding by bindingLazy()
    private val adapter: HistoryLoopAdapter by lazy { HistoryLoopAdapter() }
    private lateinit var labelAdapter: FilterLabelAdapter

    override val layoutResId = R.layout.fragment_history

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            adapter.onRestoreState(it)
        }

        binding.state = historyViewModel.state
        binding.recyclerViewHistoryItems.adapter = adapter

        adapter.actionCallback = {
            ResultsActivity.start(requireContext(), it.testUUID, ResultsActivity.ReturnPoint.HISTORY)
        }

        adapter.pendingAnimationCallback = {
            TransitionManager.beginDelayedTransition(binding.recyclerViewHistoryItems, TransitionSet().apply { addTransition(ChangeBounds()) })
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
            adapter.onClearState()
            refreshHistory()
        }

        binding.buttonSync.setOnClickListener {
            SyncDevicesDialog.show(childFragmentManager)
        }

        binding.buttonDownload.setOnClickListener {
            if (adapter.itemCount > 0) {
                HistoryDownloadDialog.instance(this, CODE_DOWNLOAD).show(parentFragmentManager)
            }
        }


        binding.buttonMenu.setOnClickListener {
            if (adapter.itemCount > 0) {
                HistoryFiltersDialog.instance(this, CODE_FILTERS).show(parentFragmentManager)
            }
        }

        labelAdapter = FilterLabelAdapter { historyViewModel.removeFromFilters(it) }
        binding.activeFilters.adapter = labelAdapter

        activity?.window?.changeStatusBarColor(ToolbarTheme.WHITE)

        historyViewModel.activeFiltersLiveData.listen(this) { data ->
            data?.let { labelAdapter.items = it }
            historyViewModel.state.isActiveFiltersEmpty.set(data.isNullOrEmpty())
        }

        historyViewModel.isLoadingLiveData.listen(this) {
            binding.swipeRefreshLayoutHistoryItems.isRefreshing = it
        }

        refreshHistory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        adapter.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }


    private fun refreshHistory() {
        historyViewModel.refreshHistory()
        binding.swipeRefreshLayoutHistoryItems.isRefreshing = false
    }

    override fun onFiltersUpdated() {
        refreshHistory()
    }

    override fun onDevicesSynced() {
        refreshHistory()
    }
}