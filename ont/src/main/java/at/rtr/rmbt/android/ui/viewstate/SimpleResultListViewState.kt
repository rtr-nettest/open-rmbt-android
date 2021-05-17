package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean

private const val KEY_IS_LOADING_LIVEDATA = "KEY_IS_LOADING_LIVEDATA"
private const val KEY_ALLOW_OPEN_LIST_ITEM_DETAILS = "KEY_ALLOW_OPEN_LIST_ITEM_DETAILS"
private const val KEY_LOOP_UUID = "KEY_LOOP_UUID"

class SimpleResultsListViewState : ViewState {

    var loopUUID: String? = null
    var allowOpenListItemDetails: Boolean = true
    val isLoadingLiveData = ObservableBoolean()

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            isLoadingLiveData.set(bundle.getBoolean(KEY_IS_LOADING_LIVEDATA, false))
            allowOpenListItemDetails = bundle.getBoolean(KEY_ALLOW_OPEN_LIST_ITEM_DETAILS, true)
            loopUUID = bundle.getString(KEY_LOOP_UUID, null)
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putBoolean(KEY_IS_LOADING_LIVEDATA, isLoadingLiveData.get())
            putBoolean(KEY_ALLOW_OPEN_LIST_ITEM_DETAILS, allowOpenListItemDetails)
            putString(KEY_LOOP_UUID, loopUUID)
        }
    }
}