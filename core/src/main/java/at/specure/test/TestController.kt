package at.specure.test

import at.rtr.rmbt.client.RMBTClientCallback

interface TestController {

    val testUUID: String?

    val isRunning: Boolean

    val testStartTimeNanos: Long

    fun start(deviceInfo: DeviceInfo, listener: TestProgressListener, clientCallback: RMBTClientCallback)

    fun stop()
}