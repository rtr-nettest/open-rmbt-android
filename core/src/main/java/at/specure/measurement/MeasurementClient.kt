package at.specure.measurement

interface MeasurementClient {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onMeasurementFinish()

    fun onMeasurementError()

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long)

    fun onUploadSpeedChanged(progress: Int, speedBps: Long)

    fun onPingChanged(pingNanos: Long)

    fun onClientReady(testUUID: String)
}