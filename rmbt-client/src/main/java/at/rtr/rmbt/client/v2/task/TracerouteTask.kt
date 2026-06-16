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
import at.rtr.rmbt.client.RMBTClient
import at.rtr.rmbt.client.v2.task.result.QoSTestResult
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.rtr.rmbt.util.tools.TracerouteService.HopDetail
import org.json.JSONArray
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * @author lb
 */
class TracerouteTask(nnTest: QualityOfServiceTest, taskDesc: TaskDesc, threadId: Int, private val masked: Boolean) :
    AbstractQoSTask(nnTest, taskDesc, threadId, threadId) {

    private val host: String?
    private val timeout: Long
    private val maxHops: Int

    init {
        this.host = taskDesc.getParams()[PARAM_HOST] as String?

        var value = taskDesc.getParams()[PARAM_TIMEOUT] as String?
        this.timeout = if (value != null) value.toLong() else DEFAULT_TIMEOUT

        value = taskDesc.getParams()[PARAM_MAX_HOPS] as String?
        this.maxHops = if (value != null) value.toInt() else DEFAULT_MAX_HOPS
    }

    @Throws(Exception::class)
    override fun call(): QoSTestResult {
        val qostestresult = if (masked) QoSTestResultEnum.TRACEROUTE_MASKED else QoSTestResultEnum.TRACEROUTE

        val testResult = initQoSTestResult(qostestresult)

        testResult.resultMap[RESULT_HOST] = host
        testResult.resultMap[RESULT_TIMEOUT] = timeout
        testResult.resultMap[RESULT_MAX_HOPS] = maxHops

        try {
            onStart(testResult)
            val pingTool = getQoSTest().getTestSettings().tracerouteServiceClazz!!.newInstance()
            pingTool.setHost(host)
            pingTool.setMaxHops(maxHops)

            val pingDetailList: List<HopDetail> = ArrayList()
            pingTool.setResultListObject(pingDetailList)

            val traceFuture = RMBTClient.getCommonThreadPool().submit(pingTool)

            try {
                traceFuture.get(timeout, TimeUnit.NANOSECONDS)
                if (!pingTool.hasMaxHopsExceeded()) {
                    testResult.resultMap[RESULT_STATUS] = "OK"
                    testResult.resultMap[RESULT_HOPS] = pingDetailList.size
                } else {
                    testResult.resultMap[RESULT_STATUS] = "MAX_HOPS_EXCEEDED"
                    testResult.resultMap[RESULT_HOPS] = maxHops
                }
            } catch (e: TimeoutException) {
                testResult.resultMap[RESULT_STATUS] = "TIMEOUT"
            } finally {
                val resultArray = JSONArray()
                for (p in pingDetailList) {
                    val json = p.toJson(this.masked)
                    if (json != null) {
                        resultArray.put(json)
                    }
                }

                testResult.resultMap[RESULT_DETAILS] = resultArray
            }
        } catch (e: Exception) {
            e.printStackTrace()
            testResult.resultMap[RESULT_STATUS] = "ERROR"
        } finally {
            onEnd(testResult)
        }

        return testResult
    }

    override fun initTask() {
    }

    override fun getTestType(): QoSTestResultEnum =
        if (masked) QoSTestResultEnum.TRACEROUTE_MASKED else QoSTestResultEnum.TRACEROUTE

    override fun needsQoSControlConnection(): Boolean = false

    companion object {
        const val DEFAULT_TIMEOUT = 10000000000L

        const val DEFAULT_MAX_HOPS = 30

        const val PARAM_HOST = "host"

        const val PARAM_TIMEOUT = "timeout"

        const val PARAM_MAX_HOPS = "max_hops"

        const val RESULT_HOST = "traceroute_objective_host"

        const val RESULT_DETAILS = "traceroute_result_details"

        const val RESULT_TIMEOUT = "traceroute_objective_timeout"

        const val RESULT_STATUS = "traceroute_result_status"

        const val RESULT_MAX_HOPS = "traceroute_objective_max_hops"

        const val RESULT_HOPS = "traceroute_result_hops"
    }
}
