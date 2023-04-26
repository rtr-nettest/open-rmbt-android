package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.ResultViewState
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.QosCategoryRecord
import at.specure.data.entity.TestResultDetailsRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.repository.TestResultsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import javax.inject.Inject

class ResultViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val testResultsRepository: TestResultsRepository
) : BaseViewModel() {

    val state = ResultViewState(appConfig)

    val testServerResultLiveData: LiveData<TestResultRecord?>
        get() = testResultsRepository.getServerTestResult(state.testUUID)

    val qoeResultLiveData: LiveData<List<QoeInfoRecord>>
        get() = testResultsRepository.getQoEItems(state.testUUID)

    val testResultDetailsLiveData: LiveData<List<TestResultDetailsRecord>>
        get() = testResultsRepository.getTestDetailsResult(state.testUUID)

    val qosCategoryResultLiveData: LiveData<List<QosCategoryRecord>>
        get() = testResultsRepository.getQosTestCategoriesResult(state.testUUID)

    val loadingLiveData: LiveData<Boolean>
        get() = _loadingLiveData

    private val _loadingLiveData = MutableLiveData<Boolean>()

    init {
        addStateSaveHandler(state)
    }

    fun loadTestResults() = launch {
        testResultsRepository.loadTestResults(state.testUUID).zip(
            testResultsRepository.loadTestDetailsResult(state.testUUID)
        ) { a, b -> a && b }
            .flowOn(Dispatchers.IO)
            .catch {
                if (it is HandledException) {
                    emit(false)
                    postError(it)
                } else {
                    throw it
                }
            }
            .collect {
                _loadingLiveData.postValue(it)
            }
    }
}