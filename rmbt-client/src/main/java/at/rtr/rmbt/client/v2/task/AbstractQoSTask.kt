/*******************************************************************************
 * Copyright 2013-2015 alladin-IT GmbH
 * Copyright 2013-2015 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.rtr.rmbt.client.v2.task

import at.rtr.rmbt.client.AbstractRMBTTest
import at.rtr.rmbt.client.QualityOfServiceTest
import at.rtr.rmbt.client.v2.task.result.QoSTestResult
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.rtr.rmbt.client.v2.task.service.TestProgressListener.TestProgressEvent
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket
import java.util.Locale

/**
 * Superclass of all QoS tasks.
 * @author lb
 */
abstract class AbstractQoSTask : AbstractRMBTTest, QoSTask {

    private val priority: Int
    private val serverPort: Int
    private val concurrencyGroup: Int
    private val serverAddress: String?
    private val qoSTestObjectiveUid: Long

    private var testStartTimestampNs: Long = 0
    private var testEndTimestampNs: Long = 0
    private var hasFinishedFlag = false
    private var hasStartedFlag = false

    // @JvmField required (not for Java interop): these are exposed both as fields (subclasses
    // access them directly) and via getter methods below (getTaskDesc() is a QoSTask interface
    // override; getQoSTest()/getId()/getControlConnection() are called as functions). Without
    // @JvmField the property's auto-generated getter would clash with those methods.
    @JvmField
    protected val taskDesc: TaskDesc

    @JvmField
    protected val qoSTest: QualityOfServiceTest

    @JvmField
    protected val id: Int

    @JvmField
    protected var controlConnection: QoSControlConnection? = null

    /**
     * this constructor sets the priority to max
     */
    constructor(nnTest: QualityOfServiceTest, taskDesc: TaskDesc, threadId: Int, id: Int) :
        this(nnTest, taskDesc, threadId, id, Integer.MAX_VALUE)

    /**
     * @param priority the higher the value, the higher the priority
     */
    constructor(nnTest: QualityOfServiceTest, taskDesc: TaskDesc, threadId: Int, id: Int, priority: Int) :
        super(nnTest.getRMBTClient(), taskDesc, threadId) {
        this.qoSTest = nnTest
        this.taskDesc = taskDesc
        this.priority = priority
        this.id = id

        var value = taskDesc.getParams()[PARAM_QOS_TEST_OBJECTIVE_ID] as String?
        this.qoSTestObjectiveUid = value!!.toLong()

        value = taskDesc.getParams()[PARAM_QOS_TEST_OBJECTIVE_PORT] as String?
        this.serverPort = value!!.toInt()

        value = taskDesc.getParams()[PARAM_QOS_CONCURRENCY_GROUP] as String?
        this.concurrencyGroup = if (value != null) value.toInt() else 0

        value = taskDesc.getParams()[PARAM_QOS_TEST_OBJECTIVE_ADDRESS] as String?
        this.serverAddress = value
    }

    abstract fun initTask()

    override fun getPriority(): Int = priority

    override fun getTestServerPort(): Int = serverPort

    override fun getTestServerAddr(): String? = serverAddress

    override fun getQoSObjectiveTestId(): Long = qoSTestObjectiveUid

    override fun getConcurrencyGroup(): Int = concurrencyGroup

    override fun getTaskDesc(): TaskDesc = taskDesc

    override fun compareTo(o: QoSTask): Int = priority.compareTo(o.getPriority())

    fun sendMessage(socket: Socket, message: String) {
        val fos = FilterOutputStream(socket.getOutputStream())
        val send = String.format(Locale.US, message)
        fos.write(send.toByteArray(Charsets.US_ASCII))
        fos.flush()
    }

    fun readLine(socket: Socket): String? {
        val fis = BufferedInputStream(socket.getInputStream())
        val r = BufferedReader(InputStreamReader(fis, "US-ASCII"), 4096)
        return r.readLine()
    }

    fun initQoSTestResult(testType: QoSTestResultEnum): QoSTestResult {
        val nnResult = QoSTestResult(testType, this)
        nnResult.resultMap[PARAM_QOS_TEST_OBJECTIVE_ID] = qoSTestObjectiveUid
        return nnResult
    }

    fun getQoSTest(): QualityOfServiceTest = qoSTest

    fun onStart(result: QoSTestResult) {
        this.testStartTimestampNs = System.nanoTime() - getQoSTest().getTestSettings()!!.startTimeNs
        this.hasStartedFlag = true
        result.resultMap[PARAM_QOS_RESULT_START_TIME] = this.testStartTimestampNs
        getQoSTest().getTestSettings()!!.dispatchTestProgressEvent(TestProgressEvent.ON_START, this)
    }

    fun onEnd(result: QoSTestResult) {
        this.testEndTimestampNs = System.nanoTime() - getQoSTest().getTestSettings()!!.startTimeNs
        result.resultMap[PARAM_QOS_RESULT_DURATION_NS] = this.testEndTimestampNs - this.testStartTimestampNs
        this.hasFinishedFlag = true
        getQoSTest().getTestSettings()!!.dispatchTestProgressEvent(TestProgressEvent.ON_END, this)
    }

    fun getTestStartTimestampNs(): Long = this.testStartTimestampNs

    fun getTestEndTimestampNs(): Long = this.testEndTimestampNs

    fun hasFinished(): Boolean = this.hasFinishedFlag

    fun hasStarted(): Boolean = this.hasStartedFlag

    fun getRelativeDurationNs(timeStampNs: Long): Long =
        timeStampNs - getQoSTest().getTestSettings()!!.startTimeNs - this.testStartTimestampNs

    fun getControlConnection(): QoSControlConnection? = controlConnection

    fun setControlConnection(controlConnection: QoSControlConnection?) {
        this.controlConnection = controlConnection
    }

    fun getId(): Int = id

    fun hasConnectionError(): Boolean {
        if (needsQoSControlConnection()) {
            return getControlConnection() == null || getControlConnection()!!.couldNotConnect.get()
        }

        return false
    }

    fun sendCommand(command: String, callback: ControlConnectionResponseCallback?) {
        controlConnection!!.sendTaskCommand(this, command, callback)
    }

    companion object {
        /**
         * timeout to establish a control connection for a test
         */
        const val CONTROL_CONNECTION_TIMEOUT = 10000

        const val QOS_SERVER_PROTOCOL_VERSION = "QoSSP0.1"

        const val PARAM_QOS_TEST_OBJECTIVE_ID = "qos_test_uid"

        const val PARAM_QOS_TEST_OBJECTIVE_PORT = "server_port"

        const val PARAM_QOS_TEST_OBJECTIVE_ADDRESS = "server_addr"

        const val PARAM_QOS_CONCURRENCY_GROUP = "concurrency_group"

        const val PARAM_QOS_RESULT_START_TIME = "start_time_ns"

        const val PARAM_QOS_RESULT_END_TIME = "end_time_ns"

        const val PARAM_QOS_RESULT_DURATION_NS = "duration_ns"
    }
}
