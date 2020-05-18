package at.specure.measurement

import at.specure.data.entity.LoopModeState

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

    val testUUID: String?

//    val loopUUID: String?

    val loopLocalUUID: String?

    fun startTests()

    fun stopTests()
}