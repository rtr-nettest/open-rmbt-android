package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import at.rtr.rmbt.android.config.AppConfig

private const val KEY_IS_LOADING_LIVEDATA = "KEY_IS_LOADING_LIVEDATA"

class HistoryViewState constructor(
    private val appConfig: AppConfig
) : ViewState {

    val isLoadingLiveData = ObservableBoolean()
    val isHistoryEmpty = ObservableBoolean()
    val isActiveFiltersEmpty = ObservableBoolean()
    val isPersistentClientUUID
        get() = appConfig.persistentClientUUIDEnabled

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