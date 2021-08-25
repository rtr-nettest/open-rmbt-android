package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableLong
import at.specure.data.entity.QosTestGoalRecord

private const val KEY_TEST_UUID = "KEY_TEST_UUID"
private const val KEY_TEST_ITEM_ID = "KEY_TEST_ITEM_ID"
private const val KEY_TEST_ITEM_DESCRIPTION = "KEY_TEST_ITEM_DESCRIPTION"
private const val KEY_TEST_ITEM_DETAILS = "KEY_TEST_ITEM_DETAILS"
private const val KEY_TEST_SUCCESS = "KEY_TEST_SUCCESS"

class QosTestDetailViewState constructor() : ViewState {

    lateinit var testUUID: String
    val testItemId = ObservableLong()
    val testItemDescription = ObservableField<String>()
    val testItemDetail = ObservableField<String>()
    val testSuccess = ObservableBoolean()
    val qosTestGoalRecord = ObservableField<List<QosTestGoalRecord>>()

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            testUUID = bundle.getString(KEY_TEST_UUID, "")
            testItemId.set(bundle.getLong(KEY_TEST_ITEM_ID, 0))
            testItemDescription.set(bundle.getString(KEY_TEST_ITEM_DESCRIPTION, ""))
            testItemDetail.set(bundle.getString(KEY_TEST_ITEM_DETAILS, ""))
            testSuccess.set(bundle.getBoolean(KEY_TEST_SUCCESS, false))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_TEST_UUID, testUUID)
            putLong(KEY_TEST_ITEM_ID, testItemId.get())
            putString(KEY_TEST_ITEM_DESCRIPTION, testItemDescription.get())
            putString(KEY_TEST_ITEM_DETAILS, testItemDetail.get())
            putBoolean(KEY_TEST_SUCCESS, testSuccess.get())
        }
    }
}