package at.specure.test

interface TestController {

    val testUUID: String?

    val isRunning: Boolean

    fun start(listener: TestProgressListener, deviceInfo: DeviceInfo)

    fun stop()
}