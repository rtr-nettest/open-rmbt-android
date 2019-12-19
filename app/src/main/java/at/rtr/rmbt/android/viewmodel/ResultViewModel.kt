package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.ui.viewstate.ResultViewState
import at.rtr.rmbt.android.util.liveDataOf
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.repository.TestResultsRepository
import javax.inject.Inject

class ResultViewModel @Inject constructor(
    private val testResultsRepository: TestResultsRepository
) : BaseViewModel() {

    val state = ResultViewState()

    val testServerResultLiveData: LiveData<TestResultRecord?>
        get() = testResultsRepository.getServerTestResult(state.testUUID)

    val qoeResultLiveData: LiveData<List<QoeInfoRecord>>
        get() = testResultsRepository.getQoEItems(state.testUUID)

    init {
        addStateSaveHandler(state)
    }

    fun loadTestResults() = liveDataOf<Boolean> { liveData ->
        testResultsRepository.loadTestResults(state.testUUID) { result ->
            result.onSuccess { liveData.postValue(it) }
            result.onFailure { postError(it) }
        }
    }
}