package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.widget.ContentLoadingProgressBar
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentResultChartBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.view.PingChart
import at.rtr.rmbt.android.ui.view.SpeedLineChart
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.ResultChartViewModel

class ResultChartFragment : BaseFragment() {

    private val viewModel: ResultChartViewModel by viewModelLazy()

    private lateinit var graphView: View
    private lateinit var progressLoadItems: ContentLoadingProgressBar

    private val binding: FragmentResultChartBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_result_chart

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val openTestUUID = arguments?.getString(KEY_OPEN_TEST_UUID)
        if (openTestUUID == null) {
            throw IllegalArgumentException("Please pass open test UUID")
        } else {
            viewModel.state.openTestUUID = openTestUUID
            viewModel.state.chartType = ResultChartType.fromValue(arguments?.getInt(KEY_CHART_TYPE))
        }

        progressLoadItems = binding.progressLoadItems

        graphView = when (viewModel.state.chartType) {
            ResultChartType.DOWNLOAD -> {
                binding.textChartType.text = getString(R.string.label_download)
                layoutInflater.inflate(R.layout.layout_speed_chart, null)
            }
            ResultChartType.UPLOAD -> {
                binding.textChartType.text = getString(R.string.label_upload)
                layoutInflater.inflate(R.layout.layout_speed_chart, null)
            }
            else -> {
                binding.textChartType.text = getString(R.string.label_ping)
                layoutInflater.inflate(R.layout.layout_ping_chart, null)
            }
        }

        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.BELOW, binding.textChartType.id)
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
            viewModel.loadGraphItems().listen(this) {
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
        when (viewModel.state.chartType) {
            ResultChartType.DOWNLOAD, ResultChartType.UPLOAD -> {
                if (graphView is SpeedLineChart) {
                    (graphView as SpeedLineChart).addResultGraphItems(viewModel.state.graphItems)
                }
            }
            ResultChartType.PING -> {
                if (graphView is PingChart) {
                    (graphView as PingChart).addGraphItems(viewModel.state.graphItems)
                }
            }
        }
    }
    companion object {

        private const val KEY_CHART_TYPE: String = "KEY_CHART_TYPE"
        private const val KEY_OPEN_TEST_UUID: String = "KEY_OPEN_TEST_UUID"
        fun newInstance(chartType: Int, openTestUUID: String): ResultChartFragment {

            val args = Bundle()
            args.putInt(KEY_CHART_TYPE, chartType)
            args.putString(KEY_OPEN_TEST_UUID, openTestUUID)

            val fragment = ResultChartFragment()
            fragment.arguments = args
            return fragment
        }
    }
}

enum class ResultChartType(val typeValue: Int) {

    DOWNLOAD(0),
    UPLOAD(1),
    PING(2);

    companion object {

        fun fromValue(typeValue: Int?): ResultChartType {
            values().forEach {
                if (it.typeValue == typeValue) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown chart type value $typeValue")
        }
    }
}
