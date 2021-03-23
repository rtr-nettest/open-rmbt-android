package at.rtr.rmbt.android.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.widget.ContentLoadingProgressBar
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentResultChartBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.view.ResultChart
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.ResultChartViewModel
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord

class ResultChartFragment : BaseFragment() {

    private val viewModel: ResultChartViewModel by viewModelLazy()

    private lateinit var graphView: View
    private lateinit var progressLoadItems: ContentLoadingProgressBar

    private val binding: FragmentResultChartBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_result_chart

    @SuppressLint("InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.testUUID = arguments?.getString(KEY_TEST_UUID) ?: throw IllegalArgumentException("Please pass test UUID")
        val typeValue = arguments?.getInt(KEY_CHART_TYPE) ?: throw IllegalArgumentException("Graph type not passed")
        viewModel.state.chartType = TestResultGraphItemRecord.Type.fromValue(typeValue)
        val networkType = arguments?.getInt(KEY_NETWORK_TYPE) ?: throw IllegalArgumentException("Network type not passed")
        viewModel.state.networkType = NetworkTypeCompat.values()[networkType]

        progressLoadItems = binding.progressLoadItems

        graphView = when (viewModel.state.chartType) {
            TestResultGraphItemRecord.Type.DOWNLOAD -> {
                layoutInflater.inflate(R.layout.layout_speed_chart, null)
            }
            TestResultGraphItemRecord.Type.UPLOAD -> {
                layoutInflater.inflate(R.layout.layout_speed_chart, null)
            }
            TestResultGraphItemRecord.Type.PING -> {
                layoutInflater.inflate(R.layout.layout_ping_chart, null)
            }
            TestResultGraphItemRecord.Type.SIGNAL -> {
                layoutInflater.inflate(R.layout.layout_signal_chart, null)
            }
        }

        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.relativeLayoutRoot.addView(graphView, params)

        graphView.visibility = View.INVISIBLE
        progressLoadItems.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadGraphItems()
    }

    private fun loadGraphItems() {
        if (viewModel.state.graphItems.isNullOrEmpty()) {
            viewModel.graphData.listen(this) {
                viewModel.state.graphItems = it
                showGraphItems()
            }
        } else {
            showGraphItems()
        }
    }

    private fun showGraphItems() {
        graphView.visibility = View.VISIBLE
        progressLoadItems.visibility = View.GONE

        with(graphView as ResultChart) {
            addResultGraphItems(viewModel.state.graphItems, viewModel.state.networkType)
        }
    }

    companion object {

        private const val KEY_CHART_TYPE: String = "KEY_CHART_TYPE"
        private const val KEY_TEST_UUID: String = "KEY_TEST_UUID"
        private const val KEY_NETWORK_TYPE = "KEY_NETWORK_TYPE"

        fun newInstance(chartType: TestResultGraphItemRecord.Type, testUUID: String, networkType: NetworkTypeCompat): ResultChartFragment {

            val args = Bundle()
            args.putInt(KEY_CHART_TYPE, chartType.typeValue)
            args.putString(KEY_TEST_UUID, testUUID)
            args.putInt(KEY_NETWORK_TYPE, networkType.ordinal)

            val fragment = ResultChartFragment()
            fragment.arguments = args
            return fragment
        }
    }
}