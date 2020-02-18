package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField

private const val KEY_STATISTICS_URL = "KEY_STATISTICS_URL"
private const val KEY_IS_LOADING = "KEY_IS_LOADING"

class StatisticsViewState : ViewState {

    val statisticsURL = ObservableField<String?>()
    val isLoading = ObservableBoolean(true)

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            statisticsURL.set(it.getString(KEY_STATISTICS_URL))
            isLoading.set(it.getBoolean(KEY_IS_LOADING, false))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.let {
            it.putString(KEY_STATISTICS_URL, statisticsURL.get())
            it.putBoolean(KEY_IS_LOADING, isLoading.get())
        }
    }
}