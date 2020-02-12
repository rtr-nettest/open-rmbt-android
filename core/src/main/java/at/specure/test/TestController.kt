package at.specure.test

import at.rtr.rmbt.client.RMBTClientCallback

interface TestController {

    val testUUID: String?

    val isRunning: Boolean

    val testStartTimeNanos: Long

    fun start(deviceInfo: DeviceInfo, loopModeUUID: String?, loopTestCount: Int, listener: TestProgressListener, clientCallback: RMBTClientCallback)

    fun stop()

    /**
     * Stops all threads and clients. All values will be reset to default ones
     */
    fun reset()
}