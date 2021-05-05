package at.rtr.rmbt.android.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentBasicResultBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.BasicResultViewModel
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord

class BasicResultFragment : BaseFragment() {

    private val viewModel: BasicResultViewModel by viewModelLazy()
    private val binding: FragmentBasicResultBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_basic_result

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = viewModel.state

        viewModel.state.testUUID = arguments?.getString(KEY_TEST_UUID) ?: throw IllegalArgumentException("Please pass test UUID")
        viewModel.testServerResultLiveData.listen(this) { result ->
            viewModel.state.testResult.set(result)

            loadGraphItems(viewModel.state.downloadGraphData, viewModel.downloadGraphLiveData, TestResultGraphItemRecord.Type.DOWNLOAD)
            loadGraphItems(viewModel.state.uploadGraphData, viewModel.uploadGraphLiveData, TestResultGraphItemRecord.Type.UPLOAD)
        }

        viewModel.loadingLiveData.listen(this) {
            if (viewModel.state.testResult.get() == null) {
                binding.textFailedToLoad.visibility = if (it) View.GONE else View.VISIBLE
            } else {
                binding.textFailedToLoad.visibility = View.GONE
            }
        }
        viewModel.loadTestResults()
    }

    private fun loadGraphItems(
        graphLoadedData: List<TestResultGraphItemRecord>?,
        graphLiveData: LiveData<List<TestResultGraphItemRecord>>,
        type: TestResultGraphItemRecord.Type
    ) {
        if (graphLoadedData.isNullOrEmpty()) {
            graphLiveData.listen(this) {
                when (type) {
                    TestResultGraphItemRecord.Type.DOWNLOAD -> viewModel.state.downloadGraphData = it
                    TestResultGraphItemRecord.Type.UPLOAD -> viewModel.state.uploadGraphData = it
                }
                showGraphItems(type)
            }
        } else {
            showGraphItems(type)
        }
    }

    private fun showGraphItems(type: TestResultGraphItemRecord.Type) {
        val isActivityInForeground = this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (isActivityInForeground) {
            when (type) {
                TestResultGraphItemRecord.Type.DOWNLOAD -> {
                    binding.downloadChart.addResultGraphItems(
                        viewModel.state.downloadGraphData,
                        viewModel.state.testResult.get()?.networkType ?: NetworkTypeCompat.TYPE_UNKNOWN
                    )
                }
                TestResultGraphItemRecord.Type.UPLOAD -> {
                    binding.uploadChart.addResultGraphItems(
                        viewModel.state.uploadGraphData,
                        viewModel.state.testResult.get()?.networkType ?: NetworkTypeCompat.TYPE_UNKNOWN
                    )
                }
            }
        }
    }

    companion object {

        private const val KEY_TEST_UUID: String = "KEY_TEST_UUID"
        fun newInstance(testUUID: String): BasicResultFragment {

            val args = Bundle()
            args.putString(KEY_TEST_UUID, testUUID)

            val fragment = BasicResultFragment()
            fragment.arguments = args
            return fragment
        }
    }
}