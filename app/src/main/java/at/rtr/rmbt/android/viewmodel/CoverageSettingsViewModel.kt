package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.CoverageSettingsViewState
import at.rtr.rmbt.android.ui.viewstate.HistoryFiltersViewState
import at.rtr.rmbt.android.util.addOnPropertyChanged
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.repository.HistoryRepository
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.measurement.coverage.RtrCoverageMeasurementProcessor
import at.specure.measurement.coverage.domain.models.CoverageMeasurementData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CoverageSettingsViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val signalMeasurementRepository: SignalMeasurementRepository,
    private val coverageMeasurementSettings: CoverageMeasurementSettings,
    private val rtrCoverageMeasurementProcessor: RtrCoverageMeasurementProcessor,
) : BaseViewModel() {

    val state = CoverageSettingsViewState(appConfig)

    val coverageMeasurementDataLiveData : LiveData<CoverageMeasurementData?>
        get() = _coverageMeasurementDataLiveData

    private val _coverageMeasurementDataLiveData: LiveData<CoverageMeasurementData?> =
        rtrCoverageMeasurementProcessor.stateManager.state.asLiveData()

    /** The two extra segment infos in the overlay are only shown to experts. */
    val isExpertModeEnabled: Boolean
        get() = appConfig.expertModeEnabled

    /**
     * Live count of not-yet-submitted previous segments (coverage sessions) of the current loop,
     * excluding the current ongoing segment. Re-subscribes to the DB query whenever the current
     * session/loop changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val unsubmittedPreviousSegmentsCountLiveData: LiveData<Int> =
        rtrCoverageMeasurementProcessor.stateManager.state
            .map { it.coverageMeasurementSession }
            .distinctUntilChangedBy { it?.localLoopId to it?.localMeasurementId }
            .flatMapLatest { session ->
                if (session == null) {
                    flowOf(0)
                } else {
                    signalMeasurementRepository.getUnsubmittedPreviousCoverageSegmentsCount(
                        session.localLoopId,
                        session.localMeasurementId
                    )
                }
            }
            .asLiveData()

    init {
        addStateSaveHandler(state)
    }
}