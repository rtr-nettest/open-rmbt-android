package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.ui.activity.ResultsActivity
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.QosCategoryRecord
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord

private const val KEY_TEST_UUID = "KEY_TEST_UUID"
private const val KEY_RETURN_POINT = "KEY_RETURN_POINT"
private const val KEY_PLAY_SERVICES = "KEY_PLAY_SERVICES"

class ResultViewState : ViewState {

    var playServicesAvailable = ObservableBoolean(false)
    val testResult = ObservableField<TestResultRecord?>()
    val qoeRecords = ObservableField<List<QoeInfoRecord>>()
    val qosCategoryRecords = ObservableField<List<QosCategoryRecord>>()
    var downloadGraphData: List<TestResultGraphItemRecord>? = null
    var uploadGraphData: List<TestResultGraphItemRecord>? = null
    lateinit var testUUID: String
    lateinit var returnPoint: ResultsActivity.ReturnPoint

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            testUUID = bundle.getString(KEY_TEST_UUID, "")
            returnPoint = ResultsActivity.ReturnPoint.valueOf(bundle.getString(KEY_RETURN_POINT, ResultsActivity.ReturnPoint.HOME.name))
            playServicesAvailable.set(bundle.getBoolean(KEY_PLAY_SERVICES))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_TEST_UUID, testUUID)
            putString(KEY_RETURN_POINT, returnPoint.name)
            putBoolean(KEY_PLAY_SERVICES, playServicesAvailable.get())
        }
    }
}