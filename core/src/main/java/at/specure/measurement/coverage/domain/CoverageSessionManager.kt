package at.specure.measurement.coverage.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface CoverageSessionManager {

    /**
     * Returns a Flow emitting session events over time.
     */
    fun sessionFlow(): Flow<CoverageMeasurementEvent>

    fun createMeasurement(coroutineScope: CoroutineScope)

    suspend fun endMeasurement()
}