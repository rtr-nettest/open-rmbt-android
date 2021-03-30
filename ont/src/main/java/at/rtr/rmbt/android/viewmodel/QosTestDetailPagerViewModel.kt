package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import at.rtr.rmbt.android.ui.viewstate.QosTestDetailPagerViewState
import at.specure.data.entity.QosTestItemRecord
import at.specure.data.repository.TestResultsRepository
import javax.inject.Inject

class QosTestDetailPagerViewModel @Inject constructor(
    private val testResultsRepository: TestResultsRepository
) : BaseViewModel() {

    val state = QosTestDetailPagerViewState()

    var qosTestItemsLiveData: LiveData<List<QosTestItemRecord>>
        get() = testResultsRepository.getQosItemsResult(state.testUUID, state.category)
        set(value) {
            this.qosTestItemsLiveData = value
        }

    init {
        addStateSaveHandler(state)
    }
}
