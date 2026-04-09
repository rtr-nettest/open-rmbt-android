package at.specure.measurement.coverage.presentation

import android.R.attr.value
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.location.LocationInfo
import at.specure.measurement.coverage.data.getFrequencyBand
import at.specure.measurement.coverage.data.getMobileNetworkType
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

    fun updateLocation(location: LocationInfo?) = update {
        copy(currentLocation = location)
    }

    fun updateNetworkInfo(networkInfo: NetworkInfo?) = update {
        copy(
            technologyMinSignalMapForCurrentFence = updateTechnologyMinSignalMap(
                data = this,
                networkInfo = networkInfo
            ),
            currentNetworkInfo = networkInfo
        )
    }

    private fun updateTechnologyMinSignalMap(data: CoverageMeasurementData, networkInfo: NetworkInfo?): HashMap<MobileNetworkType, MobileSignalTechnologyTimestamp?> {
        if (networkInfo == null) {
            return data.technologyMinSignalMapForCurrentFence
        }
        if (networkInfo is CellNetworkInfo) {
            val newTechnologySignalPair = getTechnologySignalPair(networkInfo)
            if (newTechnologySignalPair == null) {
                return data.technologyMinSignalMapForCurrentFence
            } else {
                val lastMinSignalValueForTechnology = data.technologyMinSignalMapForCurrentFence.get(networkInfo.networkType)?.signalValueDbm
                val isLastSignalForTechnologyNull = lastMinSignalValueForTechnology == null
                val newSignalValueForTechnology = newTechnologySignalPair.second?.signalValueDbm
                if (isLastSignalForTechnologyNull || (newSignalValueForTechnology != null && newSignalValueForTechnology <= (lastMinSignalValueForTechnology ?: Int.MAX_VALUE))) {
                    data.technologyMinSignalMapForCurrentFence.put(
                        networkInfo.networkType,
                        newTechnologySignalPair.second
                    )
                }
                return data.technologyMinSignalMapForCurrentFence
            }
        } else {
            return data.technologyMinSignalMapForCurrentFence
        }
    }

    private fun getTechnologySignalPair(networkInfo: NetworkInfo?): Pair<MobileNetworkType, MobileSignalTechnologyTimestamp?>? {
        if (networkInfo == null) {
            return null
        }
        if (networkInfo is CellNetworkInfo) {
            val newSignalValueForTechnology = networkInfo.signalStrength?.value
            val networkType = networkInfo.networkType
            val technologySignalTimestamp = MobileSignalTechnologyTimestamp(
                type = networkType,
                signalValueDbm = newSignalValueForTechnology,
                frequencyBand = networkInfo.getFrequencyBand(),
                timestamp = System.currentTimeMillis(),
            )
            Timber.d("Creating 1 signal technology timestamp: $technologySignalTimestamp")
            return Pair(
                networkType,
                technologySignalTimestamp,
            )
        } else {
            return null
        }
    }

    fun getMinSignalForTechnologyForCurrentFence(): MobileSignalTechnologyTimestamp? {
        Timber.d("CurrentFence minimal signals: ${state.value.technologyMinSignalMapForCurrentFence}")
        val latestTechnology = pickLatestLoggedMobileTechnology(state.value.technologyMinSignalMapForCurrentFence)
        return latestTechnology
    }

    private fun pickLatestLoggedMobileTechnology(technologyMinSignalMapForCurrentFence: HashMap<MobileNetworkType, MobileSignalTechnologyTimestamp?>): MobileSignalTechnologyTimestamp? {
        val latestSignal = technologyMinSignalMapForCurrentFence.values.maxBy { it?.timestamp ?: 0 }
        return latestSignal
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
