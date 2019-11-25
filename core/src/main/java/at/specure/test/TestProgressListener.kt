package at.specure.test

import at.rtr.rmbt.client.RMBTClientCallback
import at.specure.measurement.MeasurementState

interface TestProgressListener : RMBTClientCallback {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onPingChanged(pingMs: Long)

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long)

    fun onUploadSpeedChanged(progress: Int, speedBps: Long)

    fun onFinish()

    fun onError()

    fun onClientReady(testUUID: String)
}