package at.rtr.rmbt.android.viewmodel

import at.rtr.rmbt.android.ui.viewstate.StatisticsViewState
import at.specure.data.ControlServerSettings
import javax.inject.Inject

class StatisticsViewModel @Inject constructor(private val controlServerSettings: ControlServerSettings) : BaseViewModel() {

    val state = StatisticsViewState(controlServerSettings)

    init {
        addStateSaveHandler(state)
    }
}