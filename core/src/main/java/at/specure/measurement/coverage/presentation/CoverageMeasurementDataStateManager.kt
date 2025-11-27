package at.specure.measurement.coverage.presentation

import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.info.network.NetworkInfo
import at.specure.location.LocationInfo
import at.specure.measurement.coverage.domain.models.CoverageMeasurementData
import at.specure.measurement.coverage.domain.models.PingData
import at.specure.measurement.coverage.domain.models.state.CoverageMeasurementState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class CoverageMeasurementDataStateManager(
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
    private val scope: CoroutineScope
) {

    private val _state = MutableStateFlow(
        CoverageMeasurementData(
            coverageMeasurementSettings = coverageMeasurementSettings,
            coverageMeasurementSession = null,
            signalMeasurementException = null,
            currentNetworkInfo = null,
            currentLocation = null,
            currentPingMs = null,
            currentPingStatus = null,
        )
    )

    val state: StateFlow<CoverageMeasurementData> = _state.asStateFlow()

    fun isInStateToAddNewFences(): Boolean {
        val state = _state.value.state
        return state != CoverageMeasurementState.FINISHED_LOOP_CORRECTLY &&
                state != CoverageMeasurementState.PAUSED
    }

    fun getLastFence(): CoverageMeasurementFenceRecord? =
        _state.value.fences.lastOrNull()

    fun initData() {
        _state.value =
            CoverageMeasurementData(
                coverageMeasurementSettings = coverageMeasurementSettings,
                coverageMeasurementSession = null,
                signalMeasurementException = null,
                currentNetworkInfo = null,
                currentLocation = null,
                currentPingMs = null,
                currentPingStatus = null,
            )
    }

    fun onSessionCreated(session: CoverageMeasurementSession) {
        update {
            copy(coverageMeasurementSession = session)
        }
        Timber.d("Session created: ${session.sessionId}")
    }

    fun onUpdateCoverageDataState(newState: CoverageMeasurementState) {
        update {
            copy(state = newState)
        }
        Timber.d("Session state updated to: ${newState.name}")
    }

    fun updateLocation(location: LocationInfo?) = update {
        copy(currentLocation = location)
    }

    fun updateNetworkInfo(networkInfo: NetworkInfo?) = update {
        copy(currentNetworkInfo = networkInfo)
    }

    fun updatePingData(pingData: PingData?) = update {
        copy(
            currentPingStatus = null,
            currentPingMs = pingData?.pingStatistics?.average
        )
    }

    fun onException(exception: Exception) = update {
        copy(signalMeasurementException = exception)
    }

    fun updatePoints(loadedFences: List<CoverageMeasurementFenceRecord>) {
        update {
            copy(fences = loadedFences)
        }
        Timber.d("New fences loaded ${loadedFences.size}")
    }

    private inline fun update(
        crossinline block: CoverageMeasurementData.() -> CoverageMeasurementData
    ) {
        _state.update { block(it) }
    }
}
