package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentTestResultDetailBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.TestResultDetailAdapter
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.TestResultDetailViewModel
import at.specure.data.entity.TestResultDetailsRecord
import timber.log.Timber

class TestResultDetailFragment : BaseFragment() {

    private val testResultDetailViewModel: TestResultDetailViewModel by viewModelLazy()
    private val binding: FragmentTestResultDetailBinding by bindingLazy()
    private val adapter: TestResultDetailAdapter by lazy { TestResultDetailAdapter() }

    override val layoutResId = R.layout.fragment_test_result_detail

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //binding.state = testResultDetailViewModel.state
        binding.recyclerViewTestResultDetail.adapter = adapter

        binding.recyclerViewTestResultDetail.apply {

            val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(context, R.drawable.history_item_divider)?.let {
                itemDecoration.setDrawable(it)
            }
            binding.recyclerViewTestResultDetail.addItemDecoration(itemDecoration)
        }

        val testUUID = arguments?.getString(KEY_TEST_UUID)
        Timber.d("TestResultDetailFragment testUUID $testUUID")
        if (testUUID == null) {
            throw IllegalArgumentException("Please pass test UUID")
        } else {
            //viewModel.state.openTestUUID = openTestUUID
            //viewModel.state.chartType = ResultChartType.fromValue(arguments?.getInt(KEY_CHART_TYPE))
            testResultDetailViewModel.state.testUUID = testUUID
            testResultDetailViewModel.testResultDetailsLiveData.listen(this) {
                Timber.d("TestResultDetailFragment found ${it.size} rows of details")
                adapter.items = it as MutableList<TestResultDetailsRecord>
            }
        }

    }



    companion object {

        private const val KEY_TEST_UUID: String = "KEY_TEST_UUID"
        fun newInstance(testUUID: String): TestResultDetailFragment {

            val args = Bundle()
            args.putString(KEY_TEST_UUID, testUUID)

            val fragment = TestResultDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }
}