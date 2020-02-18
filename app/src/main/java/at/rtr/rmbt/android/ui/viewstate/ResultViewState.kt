package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableField
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.QosCategoryRecord
import at.specure.data.entity.TestResultRecord

private const val KEY_TEST_UUID = "KEY_TEST_UUID"

class ResultViewState : ViewState {

    val testResult = ObservableField<TestResultRecord?>()
    val qoeRecords = ObservableField<List<QoeInfoRecord>>()
    val qosCategoryRecords = ObservableField<List<QosCategoryRecord>>()
    lateinit var testUUID: String

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            testUUID = bundle.getString(KEY_TEST_UUID, "")
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_TEST_UUID, testUUID)
        }
    }
}