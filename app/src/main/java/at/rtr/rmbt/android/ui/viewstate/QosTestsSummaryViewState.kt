package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableField
import at.specure.data.entity.QosTestItemRecord
import at.specure.result.QoSCategory

private const val KEY_TEST_UUID = "KEY_TEST_UUID"
private const val KEY_QOS_CATEGORY = "KEY_QOS_CATEGORY"
private const val KEY_QOS_CATEGORY_DESCRIPTION = "KEY_QOS_CATEGORY_DESCRIPTION"

class QosTestsSummaryViewState constructor() : ViewState {

    lateinit var testUUID: String
    lateinit var category: QoSCategory
    val categoryDescription = ObservableField<String?>()
    val qosTestItemRecords = ObservableField<List<QosTestItemRecord>>()

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            testUUID = it.getString(KEY_TEST_UUID, "")
            category = it.getSerializable(KEY_QOS_CATEGORY) as QoSCategory
            categoryDescription.set(it.getString(KEY_QOS_CATEGORY_DESCRIPTION))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_TEST_UUID, testUUID)
            putString(KEY_QOS_CATEGORY_DESCRIPTION, categoryDescription.get())
            putSerializable(KEY_QOS_CATEGORY, category)
        }
    }
}