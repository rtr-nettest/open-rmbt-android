package at.specure.measurement.coverage.domain

import at.specure.data.entity.CoverageMeasurementSession
import at.specure.eval.PingStats
import at.specure.measurement.coverage.domain.models.PingData
import kotlinx.coroutines.flow.Flow

interface PingProcessor {
    suspend fun startPing(coverageMeasurementSession: CoverageMeasurementSession): Flow<PingData>
    suspend fun stopPing(): PingStats?
    suspend fun getCurrentPingStats(): PingStats?
    suspend fun onNewFenceStarted(): PingStats?
}
