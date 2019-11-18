package at.specure.test

import at.specure.measurement.MeasurementState

interface TestProgressListener {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onPingChanged(pingMs: Long)

    fun onDownloadSpeedChanged(speedBps: Long)

    fun onUploadSpeedChanged(speedBps: Long)
}