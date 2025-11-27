package at.specure.measurement.coverage.data

import androidx.lifecycle.LiveData
import at.rmbt.util.io
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.info.network.NetworkInfo
import at.specure.test.DeviceInfo
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FencesDataSource @Inject constructor(
    private val signalMeasurementRepository: SignalMeasurementRepository
) {
    fun loadCoverageFences(sessionId: String): LiveData<List<CoverageMeasurementFenceRecord>> {
        return signalMeasurementRepository.loadSignalMeasurementPointRecordsForMeasurement(sessionId)
    }

    fun createSignalFenceAndUpdateLastOne(
        sessionId: String,
        location: DeviceInfo.Location,
        signalRecord: SignalRecord?,
        networkInfo: NetworkInfo?,
        radiusMeters: Double,
        entryTimestampMillis: Long,
        avgPingMillisForLastFence: Double?,
        lastSavedFence: CoverageMeasurementFenceRecord?
    ) {
        val point = CoverageMeasurementFenceRecord(
            sessionId = sessionId,
            sequenceNumber = getNextSequenceNumber(lastSavedFence),
            location = location,
            signalRecordId = signalRecord?.signalMeasurementPointId, // todo: because of signal measurement it is removed when chunk is sent
            entryTimestampMillis = entryTimestampMillis,
            leaveTimestampMillis = 0,
            radiusMeters = radiusMeters,
            technologyId = networkInfo.getMobileNetworkType().intValue,
            signalStrength = networkInfo.getSignalStrengthValue(),
            frequencyBand = networkInfo.getFrequencyBand(),
            avgPingMillis = null,
        )
        signalMeasurementRepository.saveMeasurementPointRecord(point)
        updateSignalFenceAndSaveOnLeaving(
            lastSavedFence,
            entryTimestampMillis,
            avgPingMillisForLastFence
        )
        Timber.d("createSignalFenceAndUpdateLastOne: $point")

    }

    // TODO: Take network info when leaving the point - possible problem with changing the network type on map when created and when leaving
    fun updateSignalFenceAndSaveOnLeaving(
        lastFence: CoverageMeasurementFenceRecord?,
        leaveTimestampMillis: Long,
        avgPingMillis: Double?,
    ) = io {
        val updatedFence = lastFence?.copy(
            leaveTimestampMillis = leaveTimestampMillis,
            avgPingMillis = avgPingMillis
        )
        updatedFence?.let {
            signalMeasurementRepository.updateSignalMeasurementFence(updatedFence)
        }
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