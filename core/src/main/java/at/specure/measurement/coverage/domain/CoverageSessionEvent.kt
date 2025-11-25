package at.specure.measurement.coverage.domain

import at.specure.data.entity.CoverageMeasurementSession

sealed class CoverageSessionEvent {
    object SessionInitializing : CoverageSessionEvent()
    data class SessionCreated(val session: CoverageMeasurementSession) : CoverageSessionEvent()
    data class SessionRegistered(val session: CoverageMeasurementSession) : CoverageSessionEvent()
    data class SessionCreationError(val error: Exception) : CoverageSessionEvent()

    data class SessionRegistrationRetrying(
        val session: CoverageMeasurementSession,
        val attempt: Int,
        val maxAttempts: Int,
        val delayMs: Long
    ) : CoverageSessionEvent()

    data class SessionRegistrationFailed(
        val session: CoverageMeasurementSession,
        val error: Exception? = null
    ) : CoverageSessionEvent()

    object SessionEnded : CoverageSessionEvent()
}