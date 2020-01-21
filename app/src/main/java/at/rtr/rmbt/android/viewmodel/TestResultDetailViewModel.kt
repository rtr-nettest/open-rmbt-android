package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.ui.viewstate.TestResultDetailViewState
import at.specure.data.entity.TestResultDetailsRecord
import at.specure.data.repository.TestResultsRepository
import javax.inject.Inject

class TestResultDetailViewModel @Inject constructor(
    private val testResultsRepository: TestResultsRepository
) : BaseViewModel() {

    val state = TestResultDetailViewState()

    val testResultDetailsLiveData: LiveData<List<TestResultDetailsRecord>>
        get() = testResultsRepository.getTestDetailsResult(state.testUUID)

    init {
        addStateSaveHandler(state)
    }
}
