package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import at.specure.test.TestUuidType
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord

private const val KEY_TEST_UUID = "KEY_TEST_UUID"
private const val KEY_UUID_TYPE = "KEY_UUID_TYPE"
private const val KEY_RELOAD_HISTORY = "KEY_RELOAD_HISTORY"
private const val KEY_PLAY_SERVICES = "KEY_PLAY_SERVICES"

class BasicResultViewState : ViewState {

    var playServicesAvailable = ObservableBoolean(false)
    val testResult = ObservableField<TestResultRecord?>()
    var downloadGraphData: List<TestResultGraphItemRecord>? = null
    var uploadGraphData: List<TestResultGraphItemRecord>? = null
    lateinit var testUUID: String
    lateinit var uuidType: TestUuidType

    /**
     * When set to true, history is reloaded from the remote DB with latest results (offset is 0)
     */
    var useLatestResults: Boolean = false

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            testUUID = bundle.getString(KEY_TEST_UUID, "")
            uuidType = TestUuidType.values()[bundle.getInt(KEY_UUID_TYPE, TestUuidType.TEST_UUID.ordinal)]
            playServicesAvailable.set(bundle.getBoolean(KEY_PLAY_SERVICES))
            useLatestResults = bundle.getBoolean(KEY_RELOAD_HISTORY)
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putString(KEY_TEST_UUID, testUUID)
            putInt(KEY_UUID_TYPE, uuidType.ordinal)
            putBoolean(KEY_PLAY_SERVICES, playServicesAvailable.get())
            putBoolean(KEY_RELOAD_HISTORY, useLatestResults)
        }
    }
}