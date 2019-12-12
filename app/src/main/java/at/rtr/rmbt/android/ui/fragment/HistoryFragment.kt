package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHistoryBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HistoryViewModel

class HistoryFragment : BaseFragment() {

    private val historyViewModel: HistoryViewModel by viewModelLazy()
    private val binding: FragmentHistoryBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_history

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = historyViewModel.state

        historyViewModel.text.listen(this) {
            historyViewModel.state.text.set(it)
        }
    }

    override fun onStart() {
        super.onStart()

        historyViewModel.refreshHistory()
    }
}