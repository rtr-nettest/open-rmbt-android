package at.specure.test

import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.specure.measurement.MeasurementState

interface TestProgressListener {

    fun onProgressChanged(state: MeasurementState, progress: Int)

    fun onPingChanged(pingNanos: Long)

    fun onDownloadSpeedChanged(progress: Int, speedBps: Long)

    fun onUploadSpeedChanged(progress: Int, speedBps: Long)

    fun onFinish()

    fun onError()

    fun onClientReady(testUUID: String, testStartTimeNanos: Long)

    fun onQoSTestProgressUpdate(tasksPassed: Int, tasksTotal: Int, progressMap: Map<QoSTestResultEnum, Int>)
}