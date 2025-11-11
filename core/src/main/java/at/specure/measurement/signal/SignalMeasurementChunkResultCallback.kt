package at.specure.measurement.signal

import at.specure.data.entity.CoverageMeasurementSession

interface SignalMeasurementChunkResultCallback {

    fun newUUIDSent(respondedUuid: String, info: CoverageMeasurementSession)
}