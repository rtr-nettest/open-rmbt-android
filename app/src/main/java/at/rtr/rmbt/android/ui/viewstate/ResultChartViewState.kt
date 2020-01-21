package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import at.rtr.rmbt.android.ui.fragment.ResultChartType
import at.specure.data.entity.TestResultGraphItemRecord

private const val KEY_OPEN_TEST_UUID = "KEY_OPEN_TEST_UUID"
private const val KEY_CHART_TYPE = "KEY_CHART_TYPE"

class ResultChartViewState : ViewState {

    var graphItems: List<TestResultGraphItemRecord>? = null
    lateinit var chartType: ResultChartType
    lateinit var openTestUUID: String

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            openTestUUID = bundle.getString(KEY_OPEN_TEST_UUID, "")
            chartType = ResultChartType.fromValue(bundle.getInt(KEY_CHART_TYPE, -1))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_OPEN_TEST_UUID, openTestUUID)
            putInt(KEY_CHART_TYPE, chartType.typeValue)
        }
    }
}