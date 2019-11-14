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

    val pingMs: Long

    val signalStrengthInfo: SignalStrengthInfo?

    val networkInfo: NetworkInfo?

    fun startTests()

    fun stopTests()
}