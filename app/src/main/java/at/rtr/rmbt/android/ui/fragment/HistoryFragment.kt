package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import at.rmbt.util.exception.HandledException
import at.rmbt.util.exception.NoConnectionException
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHistoryBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.ResultsActivity
import at.rtr.rmbt.android.ui.adapter.HistoryAdapter
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HistoryViewModel
import at.specure.data.entity.History

private const val CODE_ERROR = 1
class HistoryFragment : BaseFragment() {

    private val historyViewModel: HistoryViewModel by viewModelLazy()
    private val binding: FragmentHistoryBinding by bindingLazy()
    private val adapter: HistoryAdapter by lazy { HistoryAdapter() }

    override val layoutResId = R.layout.fragment_history

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = historyViewModel.state
        binding.recyclerViewHistoryItems.adapter = adapter

        adapter.setCallback { history ->
            ResultsActivity.start(requireContext(), history.testUUID)
        }

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

    override fun onHandledException(exception: HandledException) {

        val messageTextResId = when (exception) {
            is NoConnectionException -> {
                R.string.no_internet_connection_not_load_data
            }
            else -> {
                R.string.test_dialog_error_text
            }
        }

        fragmentManager?.let {
            SimpleDialog.Builder()
                .messageText(messageTextResId)
                .positiveText(R.string.input_setting_dialog_ok)
                .cancelable(true)
                .show(it, CODE_ERROR)
        }
    }
    override fun onStart() {
        super.onStart()

        historyViewModel.historyLiveData.value?.let {
            if (it.isEmpty()) {
                historyViewModel.clearHistory()
            }
        }
    }
}
interface ActionCallback {
    fun onClick(history: History)
}