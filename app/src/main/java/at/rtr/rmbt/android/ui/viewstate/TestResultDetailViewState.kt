package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle

private const val KEY_TEST_UUID = "KEY_TEST_UUID"
class TestResultDetailViewState constructor() : ViewState {

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