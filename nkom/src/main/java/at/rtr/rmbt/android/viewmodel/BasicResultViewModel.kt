package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.rtr.rmbt.android.ui.viewstate.BasicResultViewState
import at.specure.test.TestUuidType
import at.specure.data.Classification
import at.specure.data.HistoryLoopMedian
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.repository.HistoryRepository
import at.specure.data.repository.TestResultsRepository
import at.specure.result.QoECategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class BasicResultViewModel @Inject constructor(
    private val testResultsRepository: TestResultsRepository,
    private val historyRepository: HistoryRepository
) : BaseViewModel() {

    val state = BasicResultViewState()

    val testServerResultLiveData: LiveData<TestResultRecord?>
        get() = testResultsRepository.getServerTestResult(state.testUUID)

    val qoeResultLiveData: MutableLiveData<List<QoeInfoRecord>>
        get() = _qoeResultLiveData

    val qoeLoopResultLiveData: MutableLiveData<List<QoeInfoRecord>>
        get() = _qoeLoopResultLiveData

    val qoeSingleResultLiveData: LiveData<List<QoeInfoRecord>>
        get() = testResultsRepository.getQoEItems(state.testUUID)

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
    private val _qoeLoopResultLiveData = MutableLiveData<List<QoeInfoRecord>>()
    val _qoeResultLiveData = MutableLiveData<List<QoeInfoRecord>>()

    init {
        addStateSaveHandler(state)
    }

    fun loadTestResults() {
        when (state.uuidType) {
            TestUuidType.TEST_UUID -> launch {
                if (state.useLatestResults) {
                    delay(750)
                } // in case of currently finished test we need to wait for BE to prepare qos results to be sent back
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
                        delay(750) // added because of BE QOS part processing performance issue
                        historyRepository.loadHistoryItems(0, 100, true).onSuccess {
                            Timber.d("History Successfully loaded: ${it[0].loopUUID} ${it[0].speedDownload}  from size: ${it.size}")
                            historyRepository.loadLoopMedianValues(state.testUUID).onCompletion {
                                _loadingLiveData.postValue(true)
                            }.collect {
                                _loopMedianValuesLiveData.postValue(it)
                                it?.qosMedian?.let { qosMedian ->
                                    val qoeqos = listOf(
                                        QoeInfoRecord(
                                            testUUID = state.testUUID,
                                            category = QoECategory.QOE_QOS,
                                            classification = Classification.NONE,
                                            percentage = qosMedian,
                                            info = null,
                                            priority = 0
                                        )
                                    )
                                    qoeLoopResultLiveData.postValue(qoeqos)
                                }
                            }
                        }
                    } else {
                        historyRepository.loadLoopMedianValues(state.testUUID).onCompletion {
                            _loadingLiveData.postValue(true)
                        }.collect {
                            _loopMedianValuesLiveData.postValue(it)
                            it?.qosMedian?.let { qosMedian ->
                                val qoeqos = listOf(
                                    QoeInfoRecord(
                                        testUUID = state.testUUID,
                                        category = QoECategory.QOE_QOS,
                                        classification = Classification.NONE,
                                        percentage = qosMedian,
                                        info = null,
                                        priority = 0
                                    )
                                )
                                qoeLoopResultLiveData.postValue(qoeqos)
                            }
                        }
                    }
                }
        }
    }
}