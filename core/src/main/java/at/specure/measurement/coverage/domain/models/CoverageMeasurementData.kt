package at.specure.measurement.coverage.domain.models

import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.CoverageMeasurementSession
import at.specure.info.network.NetworkInfo
import at.specure.location.LocationInfo
import at.specure.measurement.coverage.domain.models.state.CoverageMeasurementState

data class CoverageMeasurementData(
    val coverageMeasurementSession: CoverageMeasurementSession,
    val coverageMeasurementSettings: CoverageMeasurementSettings, // TODO: maybe remove from here
    val points: List<CoverageMeasurementFenceRecord> = mutableListOf(),
    val signalMeasurementException: Exception? = null,
    val currentNetworkInfo: NetworkInfo? = null,
    val currentLocation: LocationInfo? = null,
    val currentPingMs: Double? = null,
    val currentPingStatus: String? = null,
    val state: CoverageMeasurementState = CoverageMeasurementState.INITIALIZING
)