package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.ui.viewstate.QosTestDetailViewState
import at.specure.data.entity.QosTestGoalRecord
import at.specure.data.repository.TestResultsRepository
import javax.inject.Inject

class QosTestDetailViewModel @Inject constructor(
    private val testResultsRepository: TestResultsRepository
) : BaseViewModel() {

    val state = QosTestDetailViewState()

    val qosGoalsResultLiveData: LiveData<List<QosTestGoalRecord>>
        get() = testResultsRepository.getQosGoalsResult(state.testUUID, state.testItemId.get())

    init {
        addStateSaveHandler(state)
    }
}
