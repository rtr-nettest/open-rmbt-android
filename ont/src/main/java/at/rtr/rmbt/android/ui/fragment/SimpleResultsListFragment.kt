package at.rtr.rmbt.android.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentSimpleResultsListBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.ResultsActivity
import at.rtr.rmbt.android.ui.adapter.LoopMeasurementAdapter
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.SimpleResultsListViewModel

class SimpleResultsListFragment : BaseFragment() {

    private val simpleResultsListViewModel: SimpleResultsListViewModel by viewModelLazy()
    private val binding: FragmentSimpleResultsListBinding by bindingLazy()
    private val adapter: LoopMeasurementAdapter by lazy { LoopMeasurementAdapter() }

    override val layoutResId = R.layout.fragment_simple_results_list

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = simpleResultsListViewModel.state

        simpleResultsListViewModel.state.loopUUID = arguments?.getString(KEY_LOOP_UUID)

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

        simpleResultsListViewModel.historyLiveData.listen(this) {

            adapter.submitList(it)
        }

        simpleResultsListViewModel.isLoadingLiveData.listen(this) {
            simpleResultsListViewModel.state.isLoadingLiveData.set(it)
        }

        refreshHistory()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        refreshHistory()
    }

    private fun refreshHistory() {
        simpleResultsListViewModel.refreshHistory(binding.state?.loopUUID!!)
    }

    companion object {

        private const val KEY_LOOP_UUID: String = "KEY_LOOP_UUID"

        fun newInstance(loopUUID: String): SimpleResultsListFragment {

            val args = Bundle()
            args.putString(KEY_LOOP_UUID, loopUUID)

            val fragment = SimpleResultsListFragment()
            fragment.arguments = args
            return fragment
        }
    }
}