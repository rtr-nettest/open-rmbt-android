package at.specure.measurement.coverage.domain

import at.specure.data.entity.CoverageMeasurementSession
import kotlinx.coroutines.CoroutineScope

interface CoverageSessionManager {

    fun createSession(
        onSessionCreated: ((CoverageMeasurementSession) -> Unit)?,
        onSessionRegistered: ((CoverageMeasurementSession) -> Unit)?,
        onSessionCreationError: ((Exception) -> Unit)?,
        coroutineScope: CoroutineScope
    )

    fun endSession(resultsSent: (() -> Unit)?, resultsSendError: ((e: Exception) -> Unit)?)

}