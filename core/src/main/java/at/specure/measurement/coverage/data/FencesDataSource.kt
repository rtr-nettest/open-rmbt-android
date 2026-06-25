package at.specure.measurement.coverage.data

import androidx.lifecycle.LiveData
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.info.network.MobileNetworkType
import at.specure.measurement.coverage.domain.models.MobileSignalTechnologyTimestamp
import at.specure.test.DeviceInfo
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FencesDataSource @Inject constructor(
    private val signalMeasurementRepository: SignalMeasurementRepository
) {
    fun loadCoverageLoopFences(localLoopSessionId: String): LiveData<List<CoverageMeasurementFenceRecord>> {
        return signalMeasurementRepository.loadSignalMeasurementPointRecordsForLoopMeasurement(localLoopSessionId = localLoopSessionId)
    }

    fun loadCoverageMeasurementFences(localSessionId: String): List<CoverageMeasurementFenceRecord> {
        return signalMeasurementRepository.loadSignalMeasurementPointRecordsForMeasurementList(measurementId = localSessionId)
    }

    suspend fun createSignalFence(
        sessionId: String,
        location: DeviceInfo.Location,
        signalRecord: SignalRecord?,
        radiusMeters: Double,
        entryTimestampMillis: Long,
        avgPingMillisForLastFence: Double?,
        lastFenceMinTechSignal: MobileSignalTechnologyTimestamp?,
    ) {
        val point = CoverageMeasurementFenceRecord(
            sessionId = sessionId,
            sequenceNumber = Int.MIN_VALUE,
            location = location,
            signalRecordId = signalRecord?.signalMeasurementPointId, // todo: because of signal measurement it is removed when chunk is sent
            entryTimestampMillis = entryTimestampMillis,
            leaveTimestampMillis = 0L,
            radiusMeters = radiusMeters,
            technologyId = lastFenceMinTechSignal?.type?.intValue ?: MobileNetworkType.UNKNOWN.intValue,
            signalStrength = null,
            frequencyBand = null,
            avgPingMillis = null,
        )
        signalMeasurementRepository.createMeasurementPointRecordWithNewSequenceNumber(
            point,
            entryTimestampMillis,
            avgPingMillisForLastFence,
            lastFenceMinTechSignal
        )
        Timber.d("createSignalFenceAndUpdateLastOne: $point")
    }

    // TODO: Take network info when leaving the point? - possible problem with changing the network type on map when created and when leaving
    suspend fun updateSignalFenceAndSaveOnLeaving(
        sessionId: String,
        leaveTimestampMillis: Long,
        avgPingMillis: Double?,
        lastFenceMinTechSignal: MobileSignalTechnologyTimestamp?,
    ) {
        signalMeasurementRepository.updateSignalMeasurementOnLeavingTransaction(
            sessionId,
            leaveTimestampMillis,
            avgPingMillis,
            lastFenceMinTechSignal
        )
    }
}