package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.widget.ContentLoadingProgressBar
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.view.PingChart
import at.rtr.rmbt.android.ui.view.SpeedLineChart
import at.specure.data.entity.TestResultGraphItemRecord
import kotlinx.android.synthetic.main.fragment_result_chart.view.*


class ResultChartFragment : Fragment() {


    private lateinit var fragmentBinding: ViewDataBinding

    private lateinit var graphView: View
    private lateinit var progressLoadItems: ContentLoadingProgressBar
    private var items: List<TestResultGraphItemRecord>? = null




    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        fragmentBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_result_chart, container, false)
        return  fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressLoadItems = fragmentBinding.root.progressLoadItems

        graphView = when(arguments?.getInt(KEY_POSITION)) {
            0 -> {
                fragmentBinding.root.textChartType.text = getString(R.string.label_download)
                layoutInflater.inflate(R.layout.layout_speed_chart, null)
            }
            1 -> {
                fragmentBinding.root.textChartType.text = getString(R.string.label_upload)
                layoutInflater.inflate(R.layout.layout_speed_chart, null)
            }
            else -> {
                fragmentBinding.root.textChartType.text = getString(R.string.label_ping)
                layoutInflater.inflate(R.layout.layout_ping_chart, null)
            }
        }

        val params = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.BELOW, fragmentBinding.root.textChartType.id)
        fragmentBinding.root.relativeLayoutRoot.addView(graphView, params);

        graphView.visibility = View.INVISIBLE
        progressLoadItems.visibility = View.VISIBLE

    }

    override fun onResume() {
        super.onResume()
        items?.let {
            showGraphItems()
        }
    }
    fun setGraphItems(items: List<TestResultGraphItemRecord>) {
        this.items = items
        showGraphItems()
    }

    private fun showGraphItems() {
        graphView.visibility = View.VISIBLE
        progressLoadItems.visibility = View.GONE

        when(arguments?.getInt(KEY_POSITION)) {
            0,1 -> {
                if( graphView is SpeedLineChart) {
                    (graphView as SpeedLineChart).addResultGraphItems(items)
                }
            }
            else -> {
                if( graphView is PingChart) {
                    (graphView as PingChart).addGraphItems(items)
                }
            }
        }
    }
    companion object {

        private const val KEY_POSITION: String = "KEY_POSITION"
        fun newInstance(position: Int): ResultChartFragment {

            val args = Bundle()
            args.putInt(KEY_POSITION, position)

            val fragment = ResultChartFragment()
            fragment.arguments = args
            return fragment
        }
    }

}
