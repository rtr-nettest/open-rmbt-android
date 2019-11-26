package at.specure.measurement

import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo

interface MeasurementClient {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onMeasurementFinish()

    fun onMeasurementError()

    fun onSignalChanged(signalStrengthInfo: SignalStrengthInfo?)

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long)

    fun onUploadSpeedChanged(progress: Int, speedBps: Long)

    fun onPingChanged(pingNanos: Long)

    fun onActiveNetworkChanged(networkInfo: NetworkInfo?)
}