package at.specure.measurement.coverage.domain

import at.specure.data.entity.CoverageMeasurementSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface CoverageLoopManager {

    /**
     * Returns a Flow emitting session events over time.
     */
    fun loopFlow(): Flow<CoverageMeasurementEvent>

    fun createNewMeasurementInLoop(lastCoverageMeasurementSession: CoverageMeasurementSession, coroutineScope: CoroutineScope)

    fun startOrContinueInLoop(coroutineScope: CoroutineScope)

    fun endMeasurementInLoop(lastCoverageMeasurementSession: CoverageMeasurementSession, coroutineScope: CoroutineScope)

    suspend fun endMeasurementLoop()
}