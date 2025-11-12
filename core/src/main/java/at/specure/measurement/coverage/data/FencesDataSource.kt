package at.specure.measurement.coverage.data

import androidx.lifecycle.LiveData
import at.rmbt.util.io
import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.location.LocationInfo
import at.specure.test.toDeviceInfoLocation
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
        location: LocationInfo,
        signalRecord: SignalRecord?,
        radiusMeters: Int,
        entryTimestampMillis: Long,
        avgPingMillisForLastFence: Double?,
        lastSavedFence: CoverageMeasurementFenceRecord?
    ) {
        val point = CoverageMeasurementFenceRecord(
            sessionId = sessionId,
            sequenceNumber = getNextSequenceNumber(lastSavedFence),
            location = location.toDeviceInfoLocation(),
            signalRecordId = signalRecord?.signalMeasurementPointId, // todo: because of signal measurement it is removed when chunk is sent
            entryTimestampMillis = entryTimestampMillis,
            leaveTimestampMillis = 0,
            radiusMeters = radiusMeters,
            technologyId = signalRecord?.mobileNetworkType?.intValue,
            signalStrength = signalRecord?.signal, // todo: extract signal value correctly
            avgPingMillis = null,
        )
        signalMeasurementRepository.saveMeasurementPointRecord(point)
        updateSignalFenceAndSaveOnLeaving(
            lastSavedFence,
            entryTimestampMillis,
            avgPingMillisForLastFence
        )

    }

    // TODO: Take network info when leaving the point - possible problem with changing the network type on map when created and when leaving
    fun updateSignalFenceAndSaveOnLeaving(
        lastPoint: CoverageMeasurementFenceRecord?,
        leaveTimestampMillis: Long,
        avgPingMillis: Double?,
    ) = io {
        val updatedPoint = lastPoint?.copy(
            leaveTimestampMillis = leaveTimestampMillis,
            avgPingMillis = avgPingMillis
        )
        updatedPoint?.let {
            signalMeasurementRepository.updateSignalMeasurementPoint(updatedPoint)
        }
    }

    private fun getNextSequenceNumber(lastPoint: CoverageMeasurementFenceRecord?): Int {
        val lastPointNumber = lastPoint?.sequenceNumber ?: -1
        val nextPointNumber = lastPointNumber + 1
        return nextPointNumber
    }
}