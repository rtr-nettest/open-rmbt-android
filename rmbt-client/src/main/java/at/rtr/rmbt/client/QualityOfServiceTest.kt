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
package at.rtr.rmbt.client

import at.rtr.rmbt.client.helper.Config
import at.rtr.rmbt.client.v2.task.AbstractQoSTask
import at.rtr.rmbt.client.v2.task.DnsTask
import at.rtr.rmbt.client.v2.task.HttpProxyTask
import at.rtr.rmbt.client.v2.task.NonTransparentProxyTask
import at.rtr.rmbt.client.v2.task.QoSControlConnection
import at.rtr.rmbt.client.v2.task.QoSTestEnum
import at.rtr.rmbt.client.v2.task.QoSTestErrorEnum
import at.rtr.rmbt.client.v2.task.TaskDesc
import at.rtr.rmbt.client.v2.task.TcpTask
import at.rtr.rmbt.client.v2.task.TracerouteTask
import at.rtr.rmbt.client.v2.task.UdpTask
import at.rtr.rmbt.client.v2.task.VoipTask
import at.rtr.rmbt.client.v2.task.WebsiteTask
import at.rtr.rmbt.client.v2.task.result.QoSResultCollector
import at.rtr.rmbt.client.v2.task.result.QoSTestResult
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.rtr.rmbt.client.v2.task.service.TestProgressListener.TestProgressEvent
import at.rtr.rmbt.client.v2.task.service.TestSettings
import at.rtr.rmbt.client.v2.task.service.TrafficService
import java.util.TreeMap
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * @author lb
 */
open class QualityOfServiceTest : Callable<QoSResultCollector> {

    protected val client: RMBTClient

    protected val progressCounter = AtomicInteger()
    protected val testCountValue = AtomicInteger()
    protected val concurrentGroupCounter = AtomicInteger()
    protected val statusRef = AtomicReference<QoSTestEnum>()
    protected val errorStatusRef = AtomicReference(QoSTestErrorEnum.NONE)

    protected var executor: ExecutorService? = null
    protected var executorService: ExecutorCompletionService<QoSTestResult>? = null

    protected val qoSTestSettings: TestSettings?

    protected val concurrentTasks = TreeMap<Int, MutableList<AbstractQoSTask>>()
    protected val testMapBacking = TreeMap<QoSTestResultEnum, MutableList<AbstractQoSTask>>()
    protected val controlConnectionMap = TreeMap<String, QoSControlConnection>()
    private var onlyVoipTest = false

    protected val testGroupCounterMapBacking = TreeMap<QoSTestResultEnum, Counter>()

    // Kotlin-property views over the backing fields (Java sees getProgress()/getStatus()/… etc.)
    val progress: Int
        get() = progressCounter.get()

    val testSize: Int
        get() = testCountValue.get()

    var status: QoSTestEnum?
        get() = statusRef.get()
        set(value) {
            statusRef.set(value)
        }

    val testMap: TreeMap<QoSTestResultEnum, MutableList<AbstractQoSTask>>
        get() = testMapBacking

    val testGroupCounterMap: Map<QoSTestResultEnum, Counter>
        get() = testGroupCounterMapBacking

    /**
     * Only for purposes of inheritance
     */
    protected constructor(client: RMBTClient, nnTestSettings: TestSettings?, onlyVoipTest: Boolean) {
        this.client = client
        this.onlyVoipTest = onlyVoipTest
        this.qoSTestSettings = nnTestSettings
    }

    constructor(client: RMBTClient, nnTestSettings: TestSettings?) {
        println("\n\n---- Initializing QoS Tests ----\n")
        this.client = client
        val exec = Executors.newFixedThreadPool(client.taskDescList!!.size)
        executor = exec
        executorService = ExecutorCompletionService(exec)
        statusRef.set(QoSTestEnum.START)
        testCountValue.set(client.taskDescList!!.size)
        this.qoSTestSettings = nnTestSettings

        var threadCounter = 0

        for (taskDesc in client.taskDescList!!) {
            val taskId = taskDesc.getParams()[TaskDesc.QOS_TEST_IDENTIFIER_KEY] as String?
            var test: AbstractQoSTask? = null

            if (RMBTClient.TASK_HTTP == taskId) {
                test = HttpProxyTask(this, taskDesc, threadCounter++)
            } else if (RMBTClient.TASK_NON_TRANSPARENT_PROXY == taskId) {
                test = NonTransparentProxyTask(this, taskDesc, threadCounter++)
            } else if (RMBTClient.TASK_DNS == taskId) {
                // Android O - if dns servers are set and default dns servers should be used - use these
                if (taskDesc.getParams()[DnsTask.PARAM_DNS_RESOLVER] == null && (nnTestSettings?.defaultDnsResolvers?.size ?: 0) > 0) {
                    taskDesc.getParams()[DnsTask.PARAM_DNS_RESOLVER] = nnTestSettings!!.defaultDnsResolvers!![0].hostAddress
                }
                test = DnsTask(this, taskDesc, threadCounter++)
            } else if (RMBTClient.TASK_TCP == taskId) {
                test = TcpTask(this, taskDesc, threadCounter++)
            } else if (RMBTClient.TASK_UDP == taskId) {
                test = UdpTask(this, taskDesc, threadCounter++)
            } else if (RMBTClient.TASK_VOIP == taskId) {
                test = VoipTask(this, taskDesc, threadCounter++, null, false)
            } else if (RMBTClient.TASK_TRACEROUTE == taskId) {
                if (nnTestSettings != null && nnTestSettings.tracerouteServiceClazz != null) {
                    test = TracerouteTask(this, taskDesc, threadCounter++, false)
                } else {
                    println("No TracerouteService implementation: Skipping TracerouteTask: $taskDesc")
                }
            } else if (RMBTClient.TASK_TRACEROUTE_MASKED == taskId) {
                val traceRouteMaskedAvailable = true // enable service
                if (traceRouteMaskedAvailable && nnTestSettings != null && nnTestSettings.tracerouteServiceClazz != null) {
                    test = TracerouteTask(this, taskDesc, threadCounter++, true)
                } else {
                    println("No TracerouteMaskedService implementation: Skipping TracerouteMaskedTask: $taskDesc")
                }
            } else if (RMBTClient.TASK_WEBSITE == taskId) {
                if (nnTestSettings != null && nnTestSettings.websiteTestService != null) {
                    test = WebsiteTask(this, taskDesc, threadCounter++)
                } else {
                    println("No WebsiteTestService implementation: Skipping WebsiteTask: $taskDesc")
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

                val serverAddr = test.getTestServerAddr()!!
                if (!controlConnectionMap.containsKey(serverAddr)) {
                    val params = RMBTTestParameter(
                        serverAddr, test.getTestServerPort(),
                        nnTestSettings!!.isUseSsl, test.getTaskDesc().token,
                        test.getTaskDesc().duration, test.getTaskDesc().numThreads,
                        test.getTaskDesc().numPings, test.getTaskDesc().startTime, Config.SERVER_TYPE_QOS
                    )
                    controlConnectionMap[serverAddr] = QoSControlConnection(getRMBTClient(), params)
                }

                // check if qos test need test server
                if (test.needsQoSControlConnection()) {
                    test.setControlConnection(controlConnectionMap[serverAddr])
                    controlConnectionMap[serverAddr]!!.concurrencyGroupSet.add(test.getConcurrencyGroup())
                }
            }
        }

        qoSTestSettings?.dispatchTestProgressEvent(TestProgressEvent.ON_CREATED, null, this)
    }

    override fun call(): QoSResultCollector {
        statusRef.set(QoSTestEnum.QOS_RUNNING)
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

        executor?.shutdownNow()

        return result
    }

    fun getErrorStatus(): QoSTestErrorEnum = errorStatusRef.get()

    fun setErrorStatus(newStatus: QoSTestErrorEnum) {
        errorStatusRef.set(newStatus)
    }

    fun getCurrentConcurrentGroup(): Int = concurrentGroupCounter.get()

    fun getTestSettings(): TestSettings? = qoSTestSettings

    open fun getRMBTClient(): RMBTClient = client

    @Synchronized
    open fun interrupt() {
        executor?.shutdownNow()
        // Tear down any open QoS control connections so their blocking reader threads/sockets don't
        // leak when the QoS phase is abandoned (e.g. skipped because the server is unresponsive).
        controlConnectionMap.values.forEach { runCatching { it.interrupt() } }
    }

    protected open fun finalize() {
        executor?.shutdownNow()
    }

    protected fun openControlConnections(concurrencyGroup: Int) {
        manageControlConnections(concurrencyGroup, true)
    }

    protected fun closeControlConnections(concurrencyGroup: Int) {
        manageControlConnections(concurrencyGroup, false)
    }

    private fun manageControlConnections(concurrencyGroup: Int, openAll: Boolean) {
        val iterator = controlConnectionMap.values.iterator()
        while (iterator.hasNext()) {
            val controlConnection = iterator.next()

            try {
                if (controlConnection.concurrencyGroupSet.size > 0) {
                    if (openAll) {
                        if (controlConnection.concurrencyGroupSet.first() == concurrencyGroup) {
                            controlConnection.connect()
                            RMBTClient.getCommonThreadPool().execute(controlConnection)
                        }
                    } else {
                        if (controlConnection.concurrencyGroupSet.last() == concurrencyGroup) {
                            controlConnection.close()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * @author lb
     */
    class Counter(var testType: QoSTestResultEnum, target: Int, concurrencyGroup: Int) {
        var value: Int = 0

        var target: Int = target

        var firstTest: Int = concurrencyGroup

        var lastTest: Int = concurrencyGroup

        fun increaseCounter(concurrencyGroup: Int) {
            this.target++
            lastTest = if (concurrencyGroup > lastTest) concurrencyGroup else lastTest
            firstTest = if (concurrencyGroup < firstTest) concurrencyGroup else firstTest
        }

        override fun toString(): String =
            "Counter [testType=$testType, value=$value, target=$target, firstTest=$firstTest, lastTest=$lastTest]"
    }
}
