package at.specure.test

interface TestController {

    val isRunning: Boolean

    fun start(listener: TestProgressListener, deviceInfo: DeviceInfo)

    fun stop()
}