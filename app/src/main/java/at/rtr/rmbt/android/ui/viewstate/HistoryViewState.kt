package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean

private const val KEY_IS_LOADING_LIVEDATA = "KEY_IS_LOADING_LIVEDATA"
class HistoryViewState : ViewState {

    // Defaults to true so no placeholder text is shown until the first load resolves.
    val isLoadingLiveData = ObservableBoolean(true)
    val isHistoryEmpty = ObservableBoolean()
    val isActiveFiltersEmpty = ObservableBoolean()
    val isLoadingFailed = ObservableBoolean()

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            isLoadingLiveData.set(bundle.getBoolean(KEY_IS_LOADING_LIVEDATA, false))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putBoolean(KEY_IS_LOADING_LIVEDATA, isLoadingLiveData.get())
        }
    }
}