package at.specure.test

import at.specure.measurement.MeasurementState

interface TestProgressListener {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onPingChanged(pingMs: Long)

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long)

    fun onUploadSpeedChanged(progress: Int, speedBps: Long)

    fun onFinish()

    fun onError()

    fun onClientReady(testUUID: String)
}