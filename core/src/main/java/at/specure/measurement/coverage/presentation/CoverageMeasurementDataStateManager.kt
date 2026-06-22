package at.specure.measurement.coverage.presentation

import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.location.LocationInfo
import at.specure.measurement.coverage.domain.models.CoverageMeasurementData
import at.specure.measurement.coverage.domain.models.MobileSignalTechnologyTimestamp
import at.specure.measurement.coverage.domain.models.PingData
import at.specure.measurement.coverage.domain.models.state.CoverageMeasurementState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoverageMeasurementDataStateManager @Inject constructor(
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
            technologyMinSignalMapForCurrentFence = hashMapOf()
        )

    )

    val state: StateFlow<CoverageMeasurementData> = _state.asStateFlow()

    fun isInStateToAddNewFences(): Boolean {
        val state = _state.value.state
        return state != CoverageMeasurementState.FINISHED_LOOP_CORRECTLY &&
                state != CoverageMeasurementState.PAUSED
    }

    fun initData() {
        Timber.d("Session state updated to from init: ${CoverageMeasurementState.IDLE.name}")
        _state.value =
            CoverageMeasurementData(
                coverageMeasurementSettings = coverageMeasurementSettings,
                coverageMeasurementSession = null,
                signalMeasurementException = null,
                currentNetworkInfo = null,
                currentLocation = null,
                currentPingMs = null,
                currentPingStatus = null,
                technologyMinSignalMapForCurrentFence = hashMapOf()
            )
    }

    fun onSessionCreated(session: CoverageMeasurementSession) {
        update {
            copy(coverageMeasurementSession = session)
        }
        updateState(CoverageMeasurementState.CREATED)
        Timber.d("Session created: ${session.localMeasurementId}")
    }

    fun onSessionRegistered(session: CoverageMeasurementSession) {
        update {
            copy(coverageMeasurementSession = session)
        }
        Timber.d("Session registered: ${session.localMeasurementId}")
    }

    fun onSessionUpdate(session: CoverageMeasurementSession) {
        update {
            copy(coverageMeasurementSession = session)
        }
    }

    fun onUpdateCoverageDataState(newState: CoverageMeasurementState) {
        val currentState = _state.value.state
        when (newState) {
            CoverageMeasurementState.CREATED ->
                if (currentState == CoverageMeasurementState.IDLE) {
                    updateState(newState)
                }
            CoverageMeasurementState.RUNNING ->
                if (currentState == CoverageMeasurementState.PAUSED || currentState == CoverageMeasurementState.IDLE || currentState == CoverageMeasurementState.CREATED) {
                    updateState(newState)
                }
            CoverageMeasurementState.PAUSED ->
                if (currentState == CoverageMeasurementState.RUNNING || currentState == CoverageMeasurementState.IDLE || currentState == CoverageMeasurementState.CREATED) {
                    updateState(newState)
                }
            CoverageMeasurementState.FINISHED_LOOP_CORRECTLY,
            CoverageMeasurementState.IDLE, -> {
                updateState(newState)
            }
        }
    }

    fun updateLocationAndNetworkInfo(
            data: CoverageMeasurementData,
            location: LocationInfo?,
            networkInfo: NetworkInfo?,
        ) = update {
        copy(
            currentLocation = location,
            currentNetworkInfo = networkInfo,
            technologyMinSignalMapForCurrentFence = data.technologyMinSignalMapForCurrentFence
        )
    }

    fun onFenceExitClean(networkInfo: MobileSignalTechnologyTimestamp?) = update {
        Timber.d("updateSignalFenceOn signal map cleaning started" )
        val isNetworkInfoTooOldToPassForNewFence = (System.currentTimeMillis() - (networkInfo?.timestamp ?: 0) > 1000)
        val newMap: HashMap<MobileNetworkType, MobileSignalTechnologyTimestamp?> = if (isNetworkInfoTooOldToPassForNewFence) {
            hashMapOf()
        } else {
            networkInfo?.let { hashMapOf(it.type to it) } ?: hashMapOf()
        }
        copy(
            technologyMinSignalMapForCurrentFence = newMap
        )
    }

    fun updatePingData(pingData: PingData?) = update {
        val newPingAverage = pingData?.pingStatistics?.average
        val oldPingAverage = currentPingMs
        if (newPingAverage == null && !this.pingNullSkipped) {
            copy(
                currentPingStatus = null,
                currentPingMs = oldPingAverage,
                pingNullSkipped = true
            )
        } else {
            copy(
                currentPingStatus = null,
                currentPingMs = newPingAverage,
                pingNullSkipped = false
            )
        }
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


    private fun updateState(newState: CoverageMeasurementState) {
        update {
            copy(state = newState)
        }
        Timber.d("Session state updated to: ${newState.name}")
    }

    private inline fun update(
        crossinline block: CoverageMeasurementData.() -> CoverageMeasurementData
    ) {
        _state.update { block(it) }
    }

    fun startSendingResults() {
        update {
            copy(sendingResults = true)
        }
    }

    fun onSignalResultSent(sentSuccessfully: Boolean) {
        update {
            copy(
                sendingResults = false,
                sendingResultsError = !sentSuccessfully
            )
        }
    }

    fun removeSendingResultError() {
        update {
            copy(
                sendingResultsError = false
            )
        }
    }
}
