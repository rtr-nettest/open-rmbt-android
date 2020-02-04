package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import at.specure.result.QoSCategory

private const val KEY_TEST_UUID = "KEY_TEST_UUID"
private const val KEY_QOS_CATEGORY = "KEY_QOS_CATEGORY"
class QosTestDetailPagerViewState constructor() : ViewState {

    lateinit var testUUID: String
    lateinit var category: QoSCategory

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            testUUID = bundle.getString(KEY_TEST_UUID, "")
            category = it.getSerializable(KEY_QOS_CATEGORY) as QoSCategory
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_TEST_UUID, testUUID)
            putSerializable(KEY_QOS_CATEGORY, category)
        }
    }
}