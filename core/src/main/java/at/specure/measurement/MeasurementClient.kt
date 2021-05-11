package at.specure.measurement

import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum

interface MeasurementClient {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onMeasurementError()

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long)

    fun onUploadSpeedChanged(progress: Int, speedBps: Long)

    fun onPingChanged(pingNanos: Long)

    fun onClientReady(testUUID: String, loopLocalUUID: String?)

    fun isQoSEnabled(enabled: Boolean)

    /**
     * This should inform about event when results were sent, but only if client should display them
     */
    fun onSubmitted()

    /**
     * This should inform about event when results were sent, no matter if client should display them
     */
    fun onResultSubmitted()

    fun onSubmissionError(exception: HandledException)

    fun onQoSTestProgressUpdated(tasksPassed: Int, tasksTotal: Int, progressMap: Map<QoSTestResultEnum, Int>)

    fun onLoopCountDownTimer(timePassedMillis: Long, timeTotalMillis: Long)

    fun onLoopDistanceChanged(distancePassed: Int, distanceTotal: Int, locationAvailable: Boolean)

    fun onMeasurementCancelled()
}