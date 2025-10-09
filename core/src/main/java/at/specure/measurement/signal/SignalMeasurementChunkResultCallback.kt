package at.specure.measurement.signal

import at.specure.data.entity.SignalMeasurementSession

interface SignalMeasurementChunkResultCallback {

    fun newUUIDSent(respondedUuid: String, info: SignalMeasurementSession)
}