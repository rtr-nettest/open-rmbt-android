package at.specure.measurement

import at.specure.info.network.NetworkInfo

interface MeasurementClient {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onMeasurementFinish()

    fun onMeasurementError()

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long)

    fun onUploadSpeedChanged(progress: Int, speedBps: Long)

    fun onPingChanged(pingNanos: Long)

    fun onActiveNetworkChanged(networkInfo: NetworkInfo?)

    fun onClientReady(testUUID: String)
}