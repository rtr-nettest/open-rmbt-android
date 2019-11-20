package at.specure.measurement

import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo

interface MeasurementClient {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onMeasurementFinish()

    fun onMeasurementError()

    fun onSignalChanged(signalStrengthInfo: SignalStrengthInfo?)

    fun onDownloadSpeedChanged(speedBps: Long)

    fun onUploadSpeedChanged(speedBps: Long)

    fun onPingChanged(pingMs: Long)

    fun onActiveNetworkChanged(networkInfo: NetworkInfo?)
}