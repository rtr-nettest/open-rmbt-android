package at.rtr.rmbt.android.viewmodel

import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.LoopConfigurationViewState
import javax.inject.Inject

class LoopConfigurationViewModel @Inject constructor(config: AppConfig) : BaseViewModel() {

    val state = LoopConfigurationViewState(config)

    init {
        addStateSaveHandler(state)
    }

    fun isWaitingTimeValid(value: Int, minValue: Int, maxValue: Int) =
        if (value in minValue..maxValue) {
            state.waitingTime.set(value)
            true
        } else false

    fun isDistanceValid(value: Int, minValue: Int, maxValue: Int) =
        if (value in minValue..maxValue) {
            state.distance.set(value)
            true
        } else false

    fun isNumberValid(value: Int, minValue: Int, maxValue: Int) =
        if (value in minValue..maxValue) {
            state.numberOfTests.set(value)
            true
        } else false
}