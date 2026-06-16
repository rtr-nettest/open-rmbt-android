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
import at.rtr.rmbt.client.v2.task.service.WebsiteTestService
import at.rtr.rmbt.client.v2.task.service.WebsiteTestService.RenderingListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * @author lb
 */
class WebsiteTask(nnTest: QualityOfServiceTest, taskDesc: TaskDesc, threadId: Int) :
    AbstractQoSTask(nnTest, taskDesc, threadId, threadId) {

    private val testImpl: WebsiteTestService
    private val url: String?
    private val timeout: Long

    init {
        this.testImpl = nnTest.getTestSettings().websiteTestService!!.getInstance()

        var value = taskDesc.getParams()[PARAM_URL] as String?
        this.url = value

        value = taskDesc.getParams()[PARAM_TIMEOUT] as String?
        this.timeout = if (value != null) value.toLong() else DEFAULT_TIMEOUT
    }

    @Throws(Exception::class)
    override fun call(): QoSTestResult {
        val result = initQoSTestResult(QoSTestResultEnum.WEBSITE)
        try {
            onStart(result)

            result.resultMap[RESULT_URL] = url
            result.resultMap[RESULT_TIMEOUT] = timeout.toString()

            val latch = CountDownLatch(1)

            testImpl.setOnRenderingFinishedListener(object : RenderingListener {

                override fun onTimeoutReached(test: WebsiteTestService): Boolean {
                    println("WEBSITETEST timeout")
                    result.resultMap[RESULT_STATUS] = test.getStatusCode()
                    result.resultMap[RESULT_INFO] = "TIMEOUT"
                    result.resultMap[RESULT_DURATION] = test.getDownloadDuration()
                    result.resultMap[RESULT_RX_BYTES] = test.getRxBytes()
                    result.resultMap[RESULT_TX_BYTES] = test.getTxBytes()
                    latch.countDown()
                    return true
                }

                override fun onRenderFinished(test: WebsiteTestService) {
                    println("WEBSITETEST finished")
                    result.resultMap[RESULT_STATUS] = test.getStatusCode()
                    result.resultMap[RESULT_INFO] = "OK"
                    result.resultMap[RESULT_DURATION] = test.getDownloadDuration()
                    result.resultMap[RESULT_RX_BYTES] = test.getRxBytes()
                    result.resultMap[RESULT_TX_BYTES] = test.getTxBytes()
                    latch.countDown()
                }

                override fun onDownloadStarted(test: WebsiteTestService) {
                    // nothing to do?
                }

                override fun onError(test: WebsiteTestService): Boolean {
                    println("WEBSITETEST Error")
                    result.resultMap[RESULT_STATUS] = test.getStatusCode()
                    result.resultMap[RESULT_INFO] = "ERROR"
                    result.resultMap[RESULT_DURATION] = test.getDownloadDuration()
                    result.resultMap[RESULT_RX_BYTES] = test.getRxBytes()
                    result.resultMap[RESULT_TX_BYTES] = test.getTxBytes()
                    latch.countDown()
                    return true
                }
            })

            println("Starting WEBSITETASK")

            testImpl.run(url, (timeout / 1000000).toInt().toLong())
            latch.await(timeout, TimeUnit.NANOSECONDS)

            println("Stopping WEBSITETASK")

            return result
        } catch (e: Exception) {
            throw e
        } finally {
            onEnd(result)
        }
    }

    override fun initTask() {
    }

    override fun getTestType(): QoSTestResultEnum = QoSTestResultEnum.WEBSITE

    override fun needsQoSControlConnection(): Boolean = false

    companion object {
        private const val DEFAULT_TIMEOUT = 10000000000L

        const val PARAM_URL = "url"

        const val PARAM_TIMEOUT = "timeout"

        const val RESULT_URL = "website_objective_url"

        const val RESULT_TIMEOUT = "website_objective_timeout"

        const val RESULT_DURATION = "website_result_duration"

        const val RESULT_STATUS = "website_result_status"

        const val RESULT_INFO = "website_result_info"

        const val RESULT_RX_BYTES = "website_result_rx_bytes"

        const val RESULT_TX_BYTES = "website_result_tx_bytes"
    }
}
