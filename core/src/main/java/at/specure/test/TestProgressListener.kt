package at.specure.test

import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.specure.measurement.MeasurementState

interface TestProgressListener {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onPingChanged(pingNanos: Long)

    fun onJitterChanged(jitterNanos: Long)

    fun onPacketLossChanged(packetLossPercent: Int)

    fun onDownloadSpeedChanged(progress: Int, speedBitPerSec: Long)

    fun onUploadSpeedChanged(progress: Int, speedBitPerSec: Long)

    /** Final-algorithm value for the displayed download number (graph keeps the sliding value). */
    fun onDownloadSpeedFinalChanged(speedBitPerSec: Long) {}

    /** Final-algorithm value for the displayed upload number (graph keeps the sliding value). */
    fun onUploadSpeedFinalChanged(speedBitPerSec: Long) {}

    fun onFinish()

    /**
     * Called when test is finished and client destroyed
     */
    fun onPostFinish()

    fun onError()

    fun onClientReady(testUUID: String, loopUUID: String?, loopLocalUUID: String?, testStartTimeNanos: Long)

    fun onQoSTestProgressUpdate(tasksPassed: Int, tasksTotal: Int, progressMap: Map<QoSTestResultEnum, Int>)
}