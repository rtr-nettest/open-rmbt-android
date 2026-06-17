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

import at.rtr.rmbt.client.QualityOfServiceTest
import at.rtr.rmbt.client.v2.task.result.QoSTestResult
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TcpTask(nnTest: QualityOfServiceTest, taskDesc: TaskDesc, threadId: Int) :
    AbstractQoSTask(nnTest, taskDesc, threadId, threadId) {

    private val testPortOut: Int?
    private val testPortIn: Int?
    private val timeout: Long

    init {
        var value = taskDesc.getParams()[PARAM_PORT_IN] as String?
        testPortIn = value?.toInt()

        value = taskDesc.getParams()[PARAM_PORT_OUT] as String?
        testPortOut = value?.toInt()

        value = taskDesc.getParams()[PARAM_TIMEOUT] as String?
        timeout = if (value != null) value.toLong() else DEFAULT_TIMEOUT
    }

    @Throws(Exception::class)
    override fun call(): QoSTestResult {
        val result = initQoSTestResult(QoSTestResultEnum.TCP)
        try {
            onStart(result)

            if (testPortIn != null) {
                result.resultMap[RESULT_IN] = "FAILED"
            }
            if (testPortOut != null) {
                result.resultMap[RESULT_OUT] = "FAILED"
            }

            try {
                println("TCPTASK: " + getTestServerAddr() + ":" + getTestServerPort())

                if (testPortOut != null) {
                    // needed for timeout:
                    val latch = CountDownLatch(1)

                    // response handler
                    val callback = object : ControlConnectionResponseCallback {
                        override fun onResponse(response: String?, request: String?) {
                            if (response != null && response.startsWith("OK")) {
                                println("got response: $response")
                                var socketOut: Socket? = null

                                try {
                                    val s = getSocket(getTestServerAddr()!!, testPortOut!!, false, (timeout / 1000000).toInt())
                                    socketOut = s
                                    s.soTimeout = (timeout / 1000000).toInt()
                                    sendMessage(s, "PING\n")
                                    val testResponse = readLine(s)

                                    println("TCP OUT TEST response: $testResponse")

                                    result.resultMap[RESULT_RESPONSE_OUT] = testResponse
                                    s.close()
                                    result.resultMap[RESULT_OUT] = "OK"
                                } catch (e: SocketTimeoutException) {
                                    result.resultMap[RESULT_OUT] = "TIMEOUT"
                                } catch (e: Exception) {
                                    result.resultMap[RESULT_OUT] = "ERROR"
                                } finally {
                                    latch.countDown()
                                    if (socketOut != null && !socketOut.isClosed) {
                                        try {
                                            socketOut.close()
                                        } catch (e: IOException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    sendCommand("TCPTEST OUT $testPortOut", callback)
                    if (!latch.await(timeout, TimeUnit.NANOSECONDS)) {
                        result.resultMap[RESULT_OUT] = "TIMEOUT"
                    }
                }

                if (testPortIn != null) {
                    var serverSocket: ServerSocket? = null
                    try {
                        val ss = ServerSocket(testPortIn)
                        serverSocket = ss
                        sendCommand("TCPTEST IN $testPortIn", null)

                        ss.soTimeout = (timeout / 1000000).toInt()
                        val socketIn = ss.accept()
                        socketIn.soTimeout = (timeout / 1000000).toInt()
                        val response = readLine(socketIn)
                        println("TCP IN TEST response: $response")
                        result.resultMap[RESULT_RESPONSE_IN] = response
                        socketIn.close()
                        result.resultMap[RESULT_IN] = "OK"
                    } finally {
                        serverSocket?.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (testPortIn != null) {
                result.resultMap[RESULT_PORT_IN] = testPortIn
            }

            if (testPortOut != null) {
                result.resultMap[RESULT_PORT_OUT] = testPortOut
            }

            result.resultMap[RESULT_TIMEOUT] = timeout

            return result
        } catch (e: Exception) {
            throw e
        } finally {
            onEnd(result)
        }
    }

    override fun initTask() {
    }

    override fun getTestType(): QoSTestResultEnum = QoSTestResultEnum.TCP

    override fun needsQoSControlConnection(): Boolean = true

    companion object {
        private const val DEFAULT_TIMEOUT = 3000000000L

        const val PARAM_PORT_IN = "in_port"

        const val PARAM_PORT_OUT = "out_port"

        const val PARAM_TIMEOUT = "timeout"

        const val RESULT_PORT_IN = "tcp_objective_in_port"

        const val RESULT_PORT_OUT = "tcp_objective_out_port"

        const val RESULT_TIMEOUT = "tcp_objective_timeout"

        const val RESULT_IN = "tcp_result_in"

        const val RESULT_OUT = "tcp_result_out"

        const val RESULT_RESPONSE_OUT = "tcp_result_out_response"

        const val RESULT_RESPONSE_IN = "tcp_result_in_response"
    }
}
