package at.specure.measurement.signal

import at.specure.data.entity.SignalMeasurementChunk

interface SignalMeasurementChunkReadyCallback {

    fun onSignalMeasurementChunkReadyCheckResult(isReady: Boolean, chunk: SignalMeasurementChunk?, postProcessing: ValidChunkPostProcessing)
}