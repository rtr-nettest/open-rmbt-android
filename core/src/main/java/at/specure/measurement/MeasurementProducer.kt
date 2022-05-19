package at.specure.measurement

import at.specure.data.entity.LoopModeState
import at.specure.info.strength.SignalStrengthInfo

interface MeasurementProducer {

    fun addClient(client: MeasurementClient)

    fun removeClient(client: MeasurementClient)

    val measurementState: MeasurementState

    val loopModeState: LoopModeState

    val measurementProgress: Int

    val downloadSpeedBps: Long

    val uploadSpeedBps: Long

    val pingNanos: Long

    val isTestsRunning: Boolean

    /** checks active alarms which count time to next test **/
    val isLoopModeRunning: Boolean

    val testUUID: String?

    val lastMeasurementSignalInfo: SignalStrengthInfo?

    val loopLocalUUID: String?

    fun startTests()

    fun stopTests()
}