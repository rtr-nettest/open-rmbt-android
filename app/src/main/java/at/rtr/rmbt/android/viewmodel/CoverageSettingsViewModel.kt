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
        rtrCoverageMeasurementProcessor.stateManager.state.asLiveData(viewModelScope.coroutineContext)



    init {
        addStateSaveHandler(state)
    }
}