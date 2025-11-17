package at.rtr.rmbt.android.viewmodel

import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.CoverageSettingsViewState
import at.rtr.rmbt.android.ui.viewstate.HistoryFiltersViewState
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.repository.HistoryRepository
import javax.inject.Inject

class CoverageSettingsViewModel @Inject constructor(
    private val appConfig: AppConfig
) : BaseViewModel() {

    val state = CoverageSettingsViewState(appConfig)


    init {
        addStateSaveHandler(state)
    }
}