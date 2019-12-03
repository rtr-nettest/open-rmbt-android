package at.specure.measurement

interface MeasurementProducer {

    fun addClient(client: MeasurementClient)

    fun removeClient(client: MeasurementClient)

    val measurementState: MeasurementState

    val measurementProgress: Int

    val downloadSpeedBps: Long

    val uploadSpeedBps: Long

    val pingNanos: Long

    val isTestsRunning: Boolean

    val testUUID: String?

    fun startTests()

    fun stopTests()
}