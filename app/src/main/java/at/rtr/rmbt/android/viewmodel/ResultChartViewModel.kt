package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.ui.viewstate.ResultChartViewState
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.repository.TestResultsRepository
import javax.inject.Inject

class ResultChartViewModel @Inject constructor(
    private val testResultsRepository: TestResultsRepository
) : BaseViewModel() {

    val state = ResultChartViewState()

    val graphData: LiveData<List<TestResultGraphItemRecord>>
        get() = testResultsRepository.getGraphDataLiveData(state.openTestUUID, state.chartType)

    init {
        addStateSaveHandler(state)
    }
}