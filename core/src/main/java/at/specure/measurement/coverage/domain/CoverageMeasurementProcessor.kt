package at.specure.measurement.coverage.domain

import at.specure.data.entity.CoverageMeasurementSession

interface CoverageMeasurementProcessor {

    fun startCoverageSession(
        sessionCreated: ((session: CoverageMeasurementSession) -> Unit)?,
        sessionCreationError: ((e: Exception) -> Unit)?,
    )

    fun stopCoverageSession()

    fun pauseCoverageSession()

    fun resumeCoverageSession()

    fun getData(): CoverageMeasurementSession
}