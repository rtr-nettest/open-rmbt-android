package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import at.specure.data.NetworkTypeCompat
import at.specure.data.entity.TestResultGraphItemRecord

private const val KEY_TEST_UUID = "KEY_TEST_UUID"
private const val KEY_CHART_TYPE = "KEY_CHART_TYPE"
private const val KEY_NETWORK_TYPE = "KEY_NETWORK_TYPE"

class ResultChartViewState : ViewState {

    var graphItems: List<TestResultGraphItemRecord>? = null
    lateinit var chartType: TestResultGraphItemRecord.Type
    lateinit var testUUID: String
    lateinit var networkType: NetworkTypeCompat

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            testUUID = bundle.getString(KEY_TEST_UUID, "")
            chartType = TestResultGraphItemRecord.Type.fromValue(bundle.getInt(KEY_CHART_TYPE, TestResultGraphItemRecord.Type.DOWNLOAD.typeValue))
            networkType = NetworkTypeCompat.values()[bundle.getInt(KEY_NETWORK_TYPE)]
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_TEST_UUID, testUUID)
            putInt(KEY_CHART_TYPE, chartType.typeValue)
            putInt(KEY_NETWORK_TYPE, networkType.ordinal)
        }
    }
}