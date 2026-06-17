/*******************************************************************************
 * Copyright 2013-2016 alladin-IT GmbH
 * Copyright 2013-2016 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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
package at.rtr.rmbt.client

import at.rtr.rmbt.client.helper.Config
import at.rtr.rmbt.client.v2.task.AbstractQoSTask
import at.rtr.rmbt.client.v2.task.QoSControlConnection
import at.rtr.rmbt.client.v2.task.QoSTestEnum
import at.rtr.rmbt.client.v2.task.TaskDesc
import at.rtr.rmbt.client.v2.task.VoipTask
import at.rtr.rmbt.client.v2.task.result.QoSResultCollector
import at.rtr.rmbt.client.v2.task.service.TestProgressListener.TestProgressEvent
import at.rtr.rmbt.client.v2.task.service.TestSettings
import at.rtr.rmbt.client.v2.task.service.TrafficService
import timber.log.Timber
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors

/**
 * As of RFC 3550 and RFC 3551 most RTP (VoIP) Codecs have a sampling rate of 8kHz.
 *
 * @author lb
 */
open class VoipTest : QualityOfServiceTest {

    private var customTimeout: Long? = null

    constructor(client: RMBTClient, nnTestSettings: TestSettings?, onlyVoipTest: Boolean, customTimeout: Long?, ignoreErrors: Boolean) :
        this(client, nnTestSettings, ignoreErrors, onlyVoipTest) {
        this.customTimeout = customTimeout
    }

    constructor(client: RMBTClient, nnTestSettings: TestSettings?, ignoreErrors: Boolean, onlyVoipTest: Boolean) :
        super(client, nnTestSettings, onlyVoipTest) {

        Timber.e("Ignore errors: %s", ignoreErrors)
        println("\n\n---- Initializing VOIP Tests ----\n")
        val exec = Executors.newFixedThreadPool(1)
        executor = exec
        executorService = ExecutorCompletionService(exec)
        statusRef.set(QoSTestEnum.START)
        testCountValue.set(0)

        var threadCounter = 0

        val taskDescList = client.taskDescList

        // remove redundant tasks... keep only voip desc
        val taskDescs = ArrayList<TaskDesc>()
        if (taskDescList != null && taskDescList.size > 0) {
            taskDescs.add(taskDescList[0])
        }

        var test: AbstractQoSTask? = null

        for (taskDesc in client.taskDescList!!) {
            val taskId = taskDesc.getParams()[TaskDesc.QOS_TEST_IDENTIFIER_KEY] as String?
            if (getTestId() == taskId) {
                test = VoipTask(this, taskDesc, threadCounter++, customTimeout, ignoreErrors)
            }
        }

        if (test != null) {
            // manage taskMap:
            var testList = testMapBacking[test.getTestType()]
            if (testList == null) {
                testList = ArrayList()
                testMapBacking[test.getTestType()] = testList
            }
            testList.add(test)

            val testTypeCounter: Counter
            if (testGroupCounterMapBacking.containsKey(test.getTestType())) {
                testTypeCounter = testGroupCounterMapBacking[test.getTestType()]!!
                testTypeCounter.increaseCounter(test.getConcurrencyGroup())
            } else {
                testTypeCounter = Counter(test.getTestType(), 1, test.getConcurrencyGroup())
                testGroupCounterMapBacking[test.getTestType()] = testTypeCounter
            }

            // manage concurrent test groups
            var tasks = concurrentTasks[test.getConcurrencyGroup()]
            if (tasks == null) {
                tasks = ArrayList()
                concurrentTasks[test.getConcurrencyGroup()] = tasks
            }
            tasks.add(test)

            val serverAddr = test.getTestServerAddr()
            if (serverAddr == null || !controlConnectionMap.containsKey(serverAddr)) {
                val params = RMBTTestParameter(
                    serverAddr, test.getTestServerPort(),
                    nnTestSettings!!.isUseSsl, test.getTaskDesc().token,
                    test.getTaskDesc().duration, test.getTaskDesc().numThreads,
                    test.getTaskDesc().numPings, test.getTaskDesc().startTime, Config.SERVER_TYPE_QOS
                )
                controlConnectionMap[serverAddr!!] = QoSControlConnection(client, params)
            }

            // check if qos test need test server
            if (test.needsQoSControlConnection()) {
                test.setControlConnection(controlConnectionMap[serverAddr!!])
                controlConnectionMap[serverAddr!!]!!.concurrencyGroupSet.add(test.getConcurrencyGroup())
            }
        }

        qoSTestSettings?.dispatchTestProgressEvent(TestProgressEvent.ON_CREATED, null, this)
    }

    protected open fun getTestId(): String = RMBTClient.TASK_VOIP

    @Throws(Exception::class)
    override fun call(): QoSResultCollector {
        statusRef.set(QoSTestEnum.VOIP)
        val result = QoSResultCollector()

        val testSize = testCountValue.get()

        var trafficServiceStatus = TrafficService.SERVICE_NOT_SUPPORTED

        if (qoSTestSettings?.trafficService != null) {
            trafficServiceStatus = qoSTestSettings.trafficService!!.start()
        }

        val groupIterator = concurrentTasks.keys.iterator()
        while (groupIterator.hasNext() && statusRef.get() != QoSTestEnum.ERROR) {
            val groupId = groupIterator.next()
            concurrentGroupCounter.set(groupId)

            // check if a qos control server connection needs to be initialized:
            openControlConnections(groupId)

            if (statusRef.get() == QoSTestEnum.ERROR) {
                break
            }

            val tasks = concurrentTasks[groupId]!!
            for (task in tasks) {
                executorService!!.submit(task)
            }

            for (i in tasks.indices) {
                try {
                    val testResult = executorService!!.take()
                    if (testResult != null) {
                        val curResult = testResult.get()

                        if (curResult.isFatalError) {
                            throw InterruptedException("interrupted due to test fatal error: $curResult")
                        }

                        if (!curResult.qosTask.hasConnectionError()) {
                            result.results.add(curResult)
                        } else {
                            println("test: " + curResult.testType.name + " failed. Could not connect to QoSControlServer.")
                        }
                        println(
                            "test " + curResult.testType.name + " finished (" + (progressCounter.get() + 1) + " out of " +
                                testSize + ", CONCURRENCY GROUP=" + groupId + ")"
                        )
                        val testTypeCounter = testGroupCounterMapBacking[curResult.testType]
                        if (testTypeCounter != null) {
                            testTypeCounter.value++
                        }
                    }
                } catch (e: InterruptedException) {
                    executor!!.shutdownNow()
                    e.printStackTrace()
                    statusRef.set(QoSTestEnum.ERROR)
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    progressCounter.incrementAndGet()
                }
            }

            closeControlConnections(groupId)
        }

        if (statusRef.get() == QoSTestEnum.ERROR) {
            progressCounter.set(testCountValue.get())
        }

        if (trafficServiceStatus != TrafficService.SERVICE_NOT_SUPPORTED) {
            qoSTestSettings!!.trafficService!!.stop()
            println(
                "TRAFFIC SERVICE: Tx Bytes = " + qoSTestSettings.trafficService!!.getTxBytes() +
                    ", Rx Bytes = " + qoSTestSettings.trafficService!!.getRxBytes()
            )
        }

        if (statusRef.get() != QoSTestEnum.ERROR) {
            statusRef.set(QoSTestEnum.QOS_FINISHED)
        }

        executor!!.shutdownNow()

        return result
    }
}
