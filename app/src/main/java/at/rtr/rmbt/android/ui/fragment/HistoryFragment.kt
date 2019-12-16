package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHistoryBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.HistoryAdapter
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
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        historyViewModel.text.listen(this) {
            historyViewModel.state.text.set(it)
        }

        historyViewModel.historyLiveData.listen(this) {
            adapter.submitList(it)
        }

        historyViewModel.isLoadingLiveData.listen(this) {
            if (it) {
                Toast.makeText(requireContext(), "Loading", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        historyViewModel.refreshHistory()
    }
}