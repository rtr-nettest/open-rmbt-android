package at.specure.measurement.coverage.domain.models

import at.specure.data.CoverageMeasurementSettings
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.CoverageMeasurementSession

data class CoverageMeasurementData(
    val coverageMeasurementSession: CoverageMeasurementSession,
    val coverageMeasurementSettings: CoverageMeasurementSettings,
    val points: List<CoverageMeasurementFenceRecord> = mutableListOf(),
    val signalMeasurementException: Exception? = null,
    val currentNetworkType: String? = null,
    val currentPingMs: Double? = null,
    val currentPingStatus: String? = null,
)