package at.specure.measurement

import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum

interface MeasurementClient {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onMeasurementError()

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long)

    fun onUploadSpeedChanged(progress: Int, speedBps: Long)

    fun onPingChanged(pingNanos: Long)

    fun onClientReady(testUUID: String)

    fun isQoSEnabled(enabled: Boolean)

    fun onSubmitted()

    fun onSubmissionError(exception: HandledException)

    fun onQoSTestProgressUpdated(tasksPassed: Int, tasksTotal: Int, progressMap: Map<QoSTestResultEnum, Int>)
}