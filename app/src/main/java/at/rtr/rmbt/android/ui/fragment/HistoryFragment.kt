package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHistoryBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.HistoryAdapter
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HistoryViewModel

class HistoryFragment : BaseFragment() {

    private val historyViewModel: HistoryViewModel by viewModelLazy()
    private val binding: FragmentHistoryBinding by bindingLazy()
    private val adapter: HistoryAdapter by lazy { HistoryAdapter() }

    override val layoutResId = R.layout.fragment_history

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = historyViewModel.state
        binding.recyclerViewHistoryItems.adapter = adapter

        binding.recyclerViewHistoryItems.apply {

            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(context, R.drawable.history_item_divider)?.let {
                itemDecoration.setDrawable(it)
            }
            binding.recyclerViewHistoryItems.addItemDecoration(itemDecoration)
        }

        historyViewModel.historyLiveData.listen(this) {
            adapter.submitList(it)
        }

        historyViewModel.isLoadingLiveData.listen(this) {
            historyViewModel.state.isLoadingLiveData.set(it)
        }

        historyViewModel.isHistoryEmpty.listen(this) {
            historyViewModel.state.isHistoryEmpty.set(it)
        }

        binding.swipeRefreshLayoutHistoryItems.setOnRefreshListener {
            historyViewModel.refreshHistory()
            binding.swipeRefreshLayoutHistoryItems.isRefreshing = false
        }
        activity?.window?.changeStatusBarColor(ToolbarTheme.WHITE)
    }

    override fun onStart() {
        super.onStart()

        historyViewModel.refreshHistory()
    }
}