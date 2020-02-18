package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHistoryBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.ResultsActivity
import at.rtr.rmbt.android.ui.adapter.FilterLabelAdapter
import at.rtr.rmbt.android.ui.adapter.HistoryAdapter
import at.rtr.rmbt.android.ui.dialog.HistoryFiltersDialog
import at.rtr.rmbt.android.ui.dialog.SyncDevicesDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HistoryViewModel

private const val CODE_FILTERS = 13

class HistoryFragment : BaseFragment(), SyncDevicesDialog.Callback, HistoryFiltersDialog.Callback {

    private val historyViewModel: HistoryViewModel by viewModelLazy()
    private val binding: FragmentHistoryBinding by bindingLazy()
    private val adapter: HistoryAdapter by lazy { HistoryAdapter() }
    private lateinit var labelAdapter: FilterLabelAdapter

    override val layoutResId = R.layout.fragment_history

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = historyViewModel.state
        updateTransparentStatusBarHeight(binding.statusBarStub)
        binding.recyclerViewHistoryItems.adapter = adapter

        adapter.actionCallback = {
            ResultsActivity.start(requireContext(), it.testUUID)
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

        binding.buttonSync.setOnClickListener {
            SyncDevicesDialog.show(childFragmentManager)
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

        refreshHistory()
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