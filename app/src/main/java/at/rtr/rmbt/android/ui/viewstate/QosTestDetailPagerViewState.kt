package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import at.specure.result.QoSCategory

private const val KEY_TEST_UUID = "KEY_TEST_UUID"
private const val KEY_QOS_CATEGORY = "KEY_QOS_CATEGORY"
private const val KEY_POSITION: String = "KEY_POSITION"
class QosTestDetailPagerViewState constructor() : ViewState {

    lateinit var testUUID: String
    lateinit var category: QoSCategory
    var position: Int = 0

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            testUUID = it.getString(KEY_TEST_UUID, "")
            category = it.getSerializable(KEY_QOS_CATEGORY) as QoSCategory
            position = it.getInt(KEY_POSITION, 0)
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_TEST_UUID, testUUID)
            putSerializable(KEY_QOS_CATEGORY, category)
            putInt(KEY_POSITION, position)
        }
    }
}