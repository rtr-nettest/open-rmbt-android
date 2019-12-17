package at.specure.measurement

import at.rmbt.util.exception.HandledException

interface MeasurementClient {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onMeasurementError()

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long)

    fun onUploadSpeedChanged(progress: Int, speedBps: Long)

    fun onPingChanged(pingNanos: Long)

    fun onClientReady(testUUID: String)

    fun isQoSEnabled(enabled: Boolean)

    fun onSubmitted()

    fun onSubmissionError(exception: HandledException)
}