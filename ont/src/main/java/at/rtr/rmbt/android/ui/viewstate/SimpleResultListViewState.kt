package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean

private const val KEY_IS_LOADING_LIVEDATA = "KEY_IS_LOADING_LIVEDATA"
private const val KEY_LOOP_UUID = "KEY_LOOP_UUID"

class SimpleResultsListViewState : ViewState {

    var loopUUID: String? = null
    val isLoadingLiveData = ObservableBoolean()

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            isLoadingLiveData.set(bundle.getBoolean(KEY_IS_LOADING_LIVEDATA, false))
            loopUUID = bundle.getString(KEY_LOOP_UUID, null)
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putBoolean(KEY_IS_LOADING_LIVEDATA, isLoadingLiveData.get())
            putString(KEY_LOOP_UUID, loopUUID)
        }
    }
}