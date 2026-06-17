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
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author lb
 */
class NonTransparentProxyTask(nnTest: QualityOfServiceTest, taskDesc: TaskDesc, threadId: Int) :
    AbstractQoSTask(nnTest, taskDesc, threadId, threadId) {

    private val testRequest: String
    private val port: Int
    private val timeout: Long

    init {
        var requestString = taskDesc.getParams()[PARAM_PROXY_REQUEST] as String?

        if (!requestString!!.endsWith("\n")) {
            requestString += "\n"
        }

        this.testRequest = requestString
        this.port = (taskDesc.getParams()[PARAM_PROXY_PORT] as String?)!!.toInt()

        val value = taskDesc.getParams()[PARAM_PROXY_TIMEOUT] as String?
        this.timeout = if (value != null) value.toLong() else DEFAULT_TIMEOUT
    }

    override fun call(): QoSTestResult {
        val result = initQoSTestResult(QoSTestResultEnum.NON_TRANSPARENT_PROXY)
        try {
            onStart(result)
            result.resultMap[RESULT_PORT] = port

            val latch = CountDownLatch(1)

            val callback = object : ControlConnectionResponseCallback {
                override fun onResponse(controlResponse: String?, controlRequest: String?) {
                    try {
                        // wait for ok -> server has opened requested socket
                        if (controlResponse != null && controlResponse.startsWith("OK")) {
                            // open test socket
                            val socketAddr = InetSocketAddress(InetAddress.getByName(getTestServerAddr()), port)
                            val testSocket = Socket()
                            testSocket.connect(socketAddr, (timeout / 1000000).toInt())
                            testSocket.soTimeout = (timeout / 1000000).toInt()

                            // send request to echo service
                            sendMessage(testSocket, testRequest)

                            // read response from echo service
                            var testResponse = readLine(testSocket)
                            println("NON_TRANSPARENT_PROXY response: $testResponse")
                            if (testResponse != null) {
                                testResponse = String.format(Locale.US, "%s", testResponse)
                                result.resultMap[RESULT_RESPONSE] = testResponse.trim()
                            } else {
                                throw IOException()
                            }

                            result.resultMap[RESULT_STATUS] = "OK"
                        } else {
                            result.resultMap[RESULT_STATUS] = "ERROR"
                        }
                    } catch (e: SocketTimeoutException) {
                        e.printStackTrace()
                        result.resultMap[RESULT_STATUS] = "TIMEOUT"
                    } catch (e: Exception) {
                        e.printStackTrace()
                        result.resultMap[RESULT_STATUS] = "ERROR"
                    } finally {
                        latch.countDown()
                    }
                }
            }

            sendCommand("NTPTEST $port", callback)
            if (!latch.await(timeout, TimeUnit.NANOSECONDS)) {
                result.resultMap[RESULT_STATUS] = "TIMEOUT"
            }

            if (!result.resultMap.containsKey(RESULT_RESPONSE)) {
                result.resultMap[RESULT_RESPONSE] = ""
            }
            result.resultMap[RESULT_REQUEST] = testRequest.trim()
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

    override fun getTestType(): QoSTestResultEnum = QoSTestResultEnum.NON_TRANSPARENT_PROXY

    override fun needsQoSControlConnection(): Boolean = true

    companion object {
        const val DEFAULT_TIMEOUT = 5000000000L

        const val PARAM_PROXY_REQUEST = "request"

        const val PARAM_PROXY_PORT = "port"

        const val PARAM_PROXY_TIMEOUT = "timeout"

        const val RESULT_RESPONSE = "nontransproxy_result_response"

        const val RESULT_REQUEST = "nontransproxy_objective_request"

        const val RESULT_PORT = "nontransproxy_objective_port"

        const val RESULT_TIMEOUT = "nontransproxy_objective_timeout"

        const val RESULT_STATUS = "nontransproxy_result"
    }
}
