package at.rtr.rmbt.android.ui.viewstate

import android.os.Bundle
import androidx.databinding.ObservableBoolean
import at.specure.data.ControlServerSettings

private const val KEY_IS_LOADING = "KEY_IS_LOADING"

class StatisticsViewState(private val controlServerSettings: ControlServerSettings) : ViewState {

    val statisticsURL: String?
        get() = controlServerSettings.statisticsUrl
    val isLoading = ObservableBoolean(true)

    override fun onRestoreState(bundle: Bundle?) {
        bundle?.let {
            isLoading.set(it.getBoolean(KEY_IS_LOADING, false))
        }
    }

    override fun onSaveState(bundle: Bundle?) {
        bundle?.putBoolean(KEY_IS_LOADING, isLoading.get())
    }
}