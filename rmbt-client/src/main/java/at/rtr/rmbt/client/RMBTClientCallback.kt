package at.rtr.rmbt.client

import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.client.v2.task.result.QoSResultCollector

interface RMBTClientCallback {

    fun onClientReady(testUUID: String, loopUUID: String?, testToken: String, testStartTimeNanos: Long, threadNumber: Int)

    fun onSpeedDataChanged(threadId: Int, bytes: Long, timestampNanos: Long, isUpload: Boolean)

    fun onPingDataChanged(clientPing: Long, serverPing: Long, timeNs: Long)

    fun onTestCompleted(result: TotalTestResult, waitQoSResults: Boolean)

    fun onQoSTestCompleted(qosResult: QoSResultCollector?)

    fun onTestStatusUpdate(status: TestStatus?)
}
