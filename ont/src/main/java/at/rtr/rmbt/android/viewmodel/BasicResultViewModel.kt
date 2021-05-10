package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.rtr.rmbt.android.ui.viewstate.BasicResultViewState
import at.rtr.rmbt.android.util.TestUuidType
import at.specure.data.HistoryLoopMedian
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.repository.HistoryRepository
import at.specure.data.repository.TestResultsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import javax.inject.Inject

class BasicResultViewModel @Inject constructor(
    private val testResultsRepository: TestResultsRepository,
    private val historyRepository: HistoryRepository
) : BaseViewModel() {

    val state = BasicResultViewState()

    val testServerResultLiveData: LiveData<TestResultRecord?>
        get() = testResultsRepository.getServerTestResult(state.testUUID)

    val loopResultLiveData: LiveData<HistoryLoopMedian?>
        get() = _loopMedianValuesLiveData

    val downloadGraphLiveData: LiveData<List<TestResultGraphItemRecord>>
        get() = testResultsRepository.getGraphDataLiveData(state.testUUID, TestResultGraphItemRecord.Type.DOWNLOAD)

    val uploadGraphLiveData: LiveData<List<TestResultGraphItemRecord>>
        get() = testResultsRepository.getGraphDataLiveData(state.testUUID, TestResultGraphItemRecord.Type.UPLOAD)

    val loadingLiveData: LiveData<Boolean>
        get() = _loadingLiveData

    private val _loadingLiveData = MutableLiveData<Boolean>()
    private val _loopMedianValuesLiveData = MutableLiveData<HistoryLoopMedian>()

    init {
        addStateSaveHandler(state)
    }

    fun loadTestResults() {
        when (state.uuidType) {
            TestUuidType.TEST_UUID -> launch {
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
            TestUuidType.LOOP_UUID ->
                io {
                    if (state.useLatestResults) {
                        historyRepository.loadHistoryItems(0, 100, true).onSuccess {
                            historyRepository.loadLoopMedianValues(state.testUUID).onCompletion {
                                _loadingLiveData.postValue(true)
                            }.collect {
                                _loopMedianValuesLiveData.postValue(it)
                            }
                        }
                    } else {
                        historyRepository.loadLoopMedianValues(state.testUUID).onCompletion {
                            _loadingLiveData.postValue(true)
                        }.collect {
                            _loopMedianValuesLiveData.postValue(it)
                        }
                    }
                }
        }
    }
}