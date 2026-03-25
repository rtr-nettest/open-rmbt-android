package at.specure.measurement.coverage.data

import androidx.lifecycle.LiveData
import at.rmbt.util.io
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.DEFAULT_LEAVE_TIMESTAMP_MILLIS
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.info.network.NetworkInfo
import at.specure.test.DeviceInfo
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    suspend fun createSignalFenceAndUpdateLastOne(
        sessionId: String,
        location: DeviceInfo.Location,
        signalRecord: SignalRecord?,
        networkInfo: NetworkInfo?,
        radiusMeters: Double,
        entryTimestampMillis: Long,
        avgPingMillisForLastFence: Double?,
        lastSavedFence: CoverageMeasurementFenceRecord?,
        lastFenceMinTechSignal: Int?,
    ) {
        val point = CoverageMeasurementFenceRecord(
            sessionId = sessionId,
            sequenceNumber = Int.MIN_VALUE,
            location = location,
            signalRecordId = signalRecord?.signalMeasurementPointId, // todo: because of signal measurement it is removed when chunk is sent
            entryTimestampMillis = entryTimestampMillis,
            leaveTimestampMillis = DEFAULT_LEAVE_TIMESTAMP_MILLIS,
            radiusMeters = radiusMeters,
            technologyId = networkInfo.getMobileNetworkType().intValue,
            signalStrength = networkInfo.getSignalStrengthValue(),
            frequencyBand = networkInfo.getFrequencyBand(),
            avgPingMillis = null,
        )
        signalMeasurementRepository.createMeasurementPointRecordWithNewSequenceNumberAndUpdateLastOneTransaction(
            point,
            entryTimestampMillis,
            avgPingMillisForLastFence,
            networkInfo = networkInfo,
            lastFenceMinTechSignal = lastFenceMinTechSignal
        )
        Timber.d("createSignalFenceAndUpdateLastOne: $point")
    }

    // TODO: Take network info when leaving the point? - possible problem with changing the network type on map when created and when leaving
    suspend fun updateSignalFenceAndSaveOnLeaving(
        sessionId: String,
        leaveTimestampMillis: Long,
        avgPingMillis: Double?,
        networkInfo: NetworkInfo?,
        lastFenceMinTechSignal: Int?,
    ) {
        signalMeasurementRepository.updateSignalMeasurementOnLeavingTransaction(
            sessionId,
            leaveTimestampMillis,
            avgPingMillis,
            networkInfo,
            lastFenceMinTechSignal
        )
    }

    private fun getNextSequenceNumber(lastPoint: CoverageMeasurementFenceRecord?): Int {
        val lastPointNumber = lastPoint?.sequenceNumber ?: -1
        val nextPointNumber = lastPointNumber + 1
        return nextPointNumber
    }

    fun updateLastFenceRadius(
        lastFence: CoverageMeasurementFenceRecord?,
        newRadiusValue: Double
    ) = io {
        lastFence?.let {
            val updatedFence = it.copy(
                radiusMeters = newRadiusValue
            )
            signalMeasurementRepository.updateSignalMeasurementFence(updatedFence)
        }
    }
}