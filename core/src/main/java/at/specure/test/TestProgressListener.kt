package at.specure.test

import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.specure.measurement.MeasurementState

interface TestProgressListener {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onPingChanged(pingNanos: Long)

    /**
     * Latest UDP ping (sampled every 100 ms during the test): round-trip time in milliseconds, or
     * null when the ping was lost; plus the running count of all pings so far (1-based).
     */
    fun onUdpPingChanged(pingMs: Float?, count: Int) {}

    fun onJitterChanged(jitterNanos: Long)

    fun onPacketLossChanged(packetLossPercent: Int)

    fun onDownloadSpeedChanged(progress: Int, speedBitPerSec: Long)

    fun onUploadSpeedChanged(progress: Int, speedBitPerSec: Long)

    fun onFinish()

    /**
     * Called when test is finished and client destroyed
     */
    fun onPostFinish()

    fun onError()

    fun onClientReady(testUUID: String, loopUUID: String?, loopLocalUUID: String?, testStartTimeNanos: Long)

    fun onQoSTestProgressUpdate(tasksPassed: Int, tasksTotal: Int, progressMap: Map<QoSTestResultEnum, Int>)
}