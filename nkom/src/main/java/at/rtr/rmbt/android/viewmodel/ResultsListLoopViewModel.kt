package at.rtr.rmbt.android.viewmodel

import at.specure.data.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class ResultsListLoopViewModel @Inject constructor(private val repository: HistoryRepository) : BaseViewModel() {
    fun loadLoopMeasurements(loopId: String) = repository.loadLoopHistoryItems(loopId).flowOn(Dispatchers.IO)
}