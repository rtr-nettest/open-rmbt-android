package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean

private const val KEY_IS_DOWNLOADING_LIVEDATA = "KEY_IS_DOWNLOADING_LIVEDATA"
class HistoryDownloadViewState : ViewState {

    val isHistoryEmpty = ObservableBoolean()
    val isDownloadingLiveData = ObservableBoolean(false)

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            isDownloadingLiveData.set(bundle.getBoolean(KEY_IS_DOWNLOADING_LIVEDATA, false))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.apply {
            putBoolean(KEY_IS_DOWNLOADING_LIVEDATA, isDownloadingLiveData.get())
        }
    }
}