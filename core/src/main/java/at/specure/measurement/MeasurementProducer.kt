package at.specure.measurement

import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo

interface MeasurementProducer {

    fun addClient(client: MeasurementClient)

    fun removeClient(client: MeasurementClient)

    val measurementState: MeasurementState

    val measurementProgress: Int

    val downloadSpeedBps: Long

    val uploadSpeedBps: Long

    val pingNanos: Long

    val signalStrengthInfo: SignalStrengthInfo?

    val networkInfo: NetworkInfo?

    val isTestsRunning: Boolean

    val testUUID: String?

    fun startTests()

    fun stopTests()
}