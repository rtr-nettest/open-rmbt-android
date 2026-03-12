package at.specure.measurement.coverage.domain

import at.specure.data.entity.CoverageMeasurementSession
import at.specure.measurement.coverage.domain.models.CoverageMeasurementTerminationCause
import kotlinx.coroutines.flow.Flow

interface CoverageLoopManager {

    /**
     * Returns a Flow emitting session events over time.
     */
    fun loopFlow(): Flow<CoverageMeasurementEvent>

    fun createNewMeasurementInLoop(lastCoverageMeasurementSession: CoverageMeasurementSession)

    fun startOrContinueInLoop()

    fun endMeasurementInLoop(lastCoverageMeasurementSession: CoverageMeasurementSession, reasonToTerminate: CoverageMeasurementTerminationCause)

    suspend fun endMeasurementLoop()
}