package at.specure.measurement.coverage.domain

import at.specure.data.entity.CoverageMeasurementSession

sealed class CoverageMeasurementEvent {
    object MeasurementInitializing : CoverageMeasurementEvent()
    data class MeasurementCreated(val session: CoverageMeasurementSession) : CoverageMeasurementEvent()
    data class MeasurementRegistered(val session: CoverageMeasurementSession) : CoverageMeasurementEvent()
    data class MeasurementCreationError(val error: Exception) : CoverageMeasurementEvent()

    data class MeasurementRegistrationRetrying(
        val session: CoverageMeasurementSession,
        val attempt: Int,
        val maxAttempts: Int,
        val delayMs: Long
    ) : CoverageMeasurementEvent()

    data class MeasurementRegistrationFailed(
        val session: CoverageMeasurementSession,
        val error: Exception? = null
    ) : CoverageMeasurementEvent()

    object MeasurementEnded : CoverageMeasurementEvent()
}