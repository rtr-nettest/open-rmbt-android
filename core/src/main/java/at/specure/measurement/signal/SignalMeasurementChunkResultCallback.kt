package at.specure.measurement.signal

import at.specure.data.entity.SignalMeasurementInfo

interface SignalMeasurementChunkResultCallback {

    fun newUUIDSent(respondedUuid: String, info: SignalMeasurementInfo)
}