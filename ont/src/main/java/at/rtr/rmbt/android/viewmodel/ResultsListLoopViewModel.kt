package at.rtr.rmbt.android.viewmodel

import at.specure.data.repository.HistoryRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

class ResultsListLoopViewModel @Inject constructor(private val repository: HistoryRepository) : BaseViewModel() {
    fun loadLoopMeasurements(loopId: String) = repository.loadLoopHistoryItems(loopId).flowOn(Dispatchers.IO)
}