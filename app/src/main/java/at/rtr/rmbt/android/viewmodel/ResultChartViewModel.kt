package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.ui.fragment.ResultChartType
import at.rtr.rmbt.android.ui.viewstate.ResultChartViewState
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.repository.TestResultsRepository
import javax.inject.Inject

class ResultChartViewModel @Inject constructor(
    private val testResultsRepository: TestResultsRepository
) : BaseViewModel() {

    val state = ResultChartViewState()

    init {
        addStateSaveHandler(state)
    }

    fun loadGraphItems(): LiveData<List<TestResultGraphItemRecord>?> {

        return when (state.chartType) {
            ResultChartType.DOWNLOAD -> {
                testResultsRepository.getServerTestResultDownloadGraphItems(state.openTestUUID)
            }
            ResultChartType.UPLOAD -> {
                testResultsRepository.getServerTestResultUploadGraphItems(state.openTestUUID)
            }
            else -> {
                testResultsRepository.getServerTestResultPingGraphItems(state.openTestUUID)
            }
        }
    }
}