package at.specure.measurement.coverage.domain

import at.specure.data.entity.CoverageMeasurementSession
import at.specure.measurement.coverage.domain.models.CoverageMeasurementTerminationCause

interface CoverageMeasurementProcessor {

    fun startCoverageSession(
        sessionCreated: ((session: CoverageMeasurementSession) -> Unit)?,
        sessionCreationError: ((e: Exception) -> Unit)?,
        sessionStopped: (() -> Unit)?,
    )

    fun stopCoverageSession(reasonToTerminate: CoverageMeasurementTerminationCause)

    fun pauseCoverageSession()

    fun resumeCoverageSession()

    fun getData(): CoverageMeasurementSession
}