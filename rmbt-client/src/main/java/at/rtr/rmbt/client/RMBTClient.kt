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

import android.os.Handler
import android.os.Looper
import android.util.Log
import at.rtr.rmbt.client.RMBTTest.CurrentSpeed
import at.rtr.rmbt.client.helper.Config
import at.rtr.rmbt.client.helper.ControlServerConnection
import at.rtr.rmbt.client.helper.IntermediateResult
import at.rtr.rmbt.client.helper.RMBTOutputCallback
import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.client.v2.task.TaskDesc
import at.rtr.rmbt.client.v2.task.VoipTask
import at.rtr.rmbt.client.v2.task.result.QoSResultCollector
import at.rtr.rmbt.client.v2.task.result.QoSTestResult
import at.rtr.rmbt.client.v2.task.service.TestMeasurement
import at.rtr.rmbt.client.v2.task.service.TestSettings
import at.rtr.rmbt.client.v2.task.service.TrafficService
import at.rtr.rmbt.util.model.shared.exception.ErrorStatus
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RMBTClient private constructor(
    private val params: RMBTTestParameter,
    val controlConnection: ControlServerConnection?,
    private val cacheDir: File?,
    private val enabledJitterAndPacketLoss: Boolean
) : RMBTClientCallback {

    private val durationInitNano = 2500000000L // TODO
    private val durationUpNano: Long
    private val durationDownNano: Long

    private val pingNano = AtomicLong(-1)
    private val downBitPerSec = AtomicLong(-1)
    private val upBitPerSec = AtomicLong(-1)
    private val jitter = AtomicLong(-1)
    private val jitterStartTime = AtomicLong(-1)
    private val packetLossUp = AtomicLong(-1)
    private val packetLossDown = AtomicLong(-1)

    /* ping status */
    private val pingTsStart = AtomicLong(-1)
    private val pingNumDome = AtomicInteger(-1)
    private val pingTsLastPing = AtomicLong(-1)

    private var lastCounter = 0
    private val lastTransfer: Array<LongArray>
    private val lastTime: Array<LongArray>

    private val testThreadPool: ExecutorService?

    private val testTasks: Array<RMBTTest?>?

    private var totalResult: TotalTestResult? = null

    var sslSocketFactory: SSLSocketFactory? = null
        private set

    private val voipReference = AtomicReference<QualityOfServiceTest>()

    private var outputCallback: RMBTOutputCallback? = null
    private val outputToStdout = true

    private val aborted = AtomicBoolean()

    val errorMsg: String = ""

    var commonCallback: RMBTClientCallback? = null

    val taskDescList: List<TaskDesc>?

    private val testStatus = AtomicReference(TestStatus.WAIT)
    private val statusBeforeErrorRef = AtomicReference<TestStatus?>(null)
    private val statusChangeTime = AtomicLong()
    private val jitterTestStart = AtomicLong()
    private val jitterTestDurationNanos = AtomicLong()

    var trafficService: TrafficService? = null

    private val measurementMap = ConcurrentHashMap<TestStatus, TestMeasurement>()

    private var measurementLastUpdate = System.currentTimeMillis()
    private var inactivityExecutor: ScheduledExecutorService? = null
    private var inactivityFuture: ScheduledFuture<*>? = null

    val isEnabledJitterAndPacketLossTest: Boolean
        get() = enabledJitterAndPacketLoss

    private constructor(params: RMBTTestParameter, controlConnection: ControlServerConnection?) :
        this(params, controlConnection, null, false)

    init {
        this.jitterTestDurationNanos.set(10000000000L) // set default value
        planInactivityCheck()

        params.check()

        if (params.numThreads > 0) {
            testThreadPool = Executors.newFixedThreadPool(params.numThreads)
            testTasks = arrayOfNulls(params.numThreads)
        } else {
            testThreadPool = null
            testTasks = null
        }

        durationDownNano = params.duration * 1000000000L
        durationUpNano = params.duration * 1000000000L

        lastTransfer = Array(params.numThreads) { LongArray(KEEP_LAST_ENTRIES) }
        lastTime = Array(params.numThreads) { LongArray(KEEP_LAST_ENTRIES) }

        taskDescList = controlConnection?.v2TaskDesc
    }

    private fun isRMBTClientResponding(): Boolean {
        val clientLastResponseDurationMillis = System.currentTimeMillis() - measurementLastUpdate
        Timber.d("RMBTClient inactivity internal for: " + TimeUnit.MILLISECONDS.toSeconds(clientLastResponseDurationMillis) + " seconds")
        return clientLastResponseDurationMillis < TimeUnit.SECONDS.toMillis(MEASUREMENT_INACTIVITY_THRESHOLD_SECONDS)
    }

    @Synchronized
    private fun planInactivityCheck() {
        stopInactivityCheck()

        val executor = inactivityExecutor
        if (executor == null || executor.isShutdown) {
            inactivityExecutor = Executors.newSingleThreadScheduledExecutor()
        }

        Timber.d("RMBTClient inactivity internal checking started (manual schedule)")
        scheduleNextInactivityCheck()
    }

    private fun scheduleNextInactivityCheck() {
        val delayMillis = TimeUnit.SECONDS.toMillis(MEASUREMENT_INACTIVITY_CHECKER_PERIOD_SECONDS)

        inactivityFuture = inactivityExecutor!!.schedule(
            {
                val isNotResponding = !isRMBTClientResponding()

                if (isNotResponding) {
                    Timber.e("Test has been terminated, because of RMBTClient inactivity internal")

                    Handler(Looper.getMainLooper()).post {
                        commonCallback?.onTestStatusUpdate(TestStatus.ERROR)
                    }

                    abortTest(true)
                    shutdown()
                } else {
                    scheduleNextInactivityCheck()
                }
            },
            delayMillis,
            TimeUnit.MILLISECONDS
        )
    }

    @Synchronized
    private fun stopInactivityCheck() {
        Timber.d("RMBTClient inactivity internal checking stopped")
        inactivityFuture?.cancel(true)
        inactivityFuture = null
    }

    private fun createSSLSocketFactory(): SSLSocketFactory? {
        log("initSSL...")
        try {
            val sc = getSSLContext(null, null)
            return sc.socketFactory
        } catch (e: Exception) {
            setErrorStatus()
            log(e)
        }
        return null
    }

    fun runTest(): TotalTestResult? {
        println("starting test...")

        var txBytes: Long = 0
        var rxBytes: Long = 0
        val timeStampStart = System.nanoTime()

        try {
            measurementLastUpdate = System.currentTimeMillis()
            planInactivityCheck()
            commonCallback!!.onClientReady(
                controlConnection!!.getTestUuid(),
                controlConnection.getLoopUuid(),
                controlConnection.getTestToken(),
                controlConnection.getStartTimeNs(),
                params.numThreads
            )
        } catch (e: Exception) {
            e.printStackTrace()
            testStatus.set(TestStatus.ERROR)
        }

        if (testStatus.get() != TestStatus.ERROR && testThreadPool != null) {
            if (trafficService != null) {
                txBytes = trafficService!!.getTotalTxBytes()
                rxBytes = trafficService!!.getTotalRxBytes()
            }

            resetSpeed()
            downBitPerSec.set(-1)
            upBitPerSec.set(-1)
            pingNano.set(-1)

            val waitTime = params.startTime - System.currentTimeMillis()
            if (waitTime > 0) {
                status = TestStatus.WAIT
                log(String.format(Locale.US, "we have to wait %d ms...", waitTime))
                Thread.sleep(waitTime)
                log(String.format(Locale.US, "...done.", waitTime))
            } else {
                log(String.format(Locale.US, "luckily we do not have to wait.", waitTime))
            }

            status = TestStatus.INIT
            statusBeforeErrorRef.set(null)

            if (testThreadPool.isShutdown) {
                throw IllegalStateException("RMBTClient already shut down")
            }
            log("starting test...")

            val numThreads = params.numThreads

            aborted.set(false)

            val result = TotalTestResult()
            totalResult = result

            if (params.isEncryption) {
                sslSocketFactory = createSSLSocketFactory()
            }

            log(String.format(Locale.US, "Host: %s; Port: %s; Enc: %s", params.host, params.port, params.isEncryption))
            log(String.format(Locale.US, "starting %d threads...", numThreads))

            val barrier = CyclicBarrier(numThreads)

            val results = arrayOfNulls<Future<ThreadTestResult?>>(numThreads)

            val storeResults = (params.duration * 1000000000L / MIN_DIFF_TIME).toInt()

            val fallbackToOneThread = AtomicBoolean()

            for (i in 0 until numThreads) {
                val task = RMBTTest(this, params, i, barrier, storeResults, MIN_DIFF_TIME, fallbackToOneThread)
                testTasks!![i] = task
                results[i] = testThreadPool.submit(task)
            }

            try {
                var shortestPing = Long.MAX_VALUE

                // wait for all threads first
                for (i in 0 until numThreads) {
                    results[i]!!.get()
                }

                if (aborted.get()) {
                    return null
                }

                val allDownBytes = arrayOfNulls<LongArray>(numThreads)
                val allDownNsecs = arrayOfNulls<LongArray>(numThreads)
                val allUpBytes = arrayOfNulls<LongArray>(numThreads)
                val allUpNsecs = arrayOfNulls<LongArray>(numThreads)

                var realNumThreads = 0
                log("")
                for (i in 0 until numThreads) {
                    val testResult = results[i]!!.get()

                    if (testResult != null) {
                        realNumThreads++

                        log(
                            String.format(
                                Locale.US, "Thread %d: Download: bytes: %d time: %.3f s", i,
                                ThreadTestResult.getLastEntry(testResult.down!!.bytes),
                                ThreadTestResult.getLastEntry(testResult.down!!.nsec) / 1e9
                            )
                        )
                        log(
                            String.format(
                                Locale.US, "Thread %d: Upload:   bytes: %d time: %.3f s", i,
                                ThreadTestResult.getLastEntry(testResult.up!!.bytes),
                                ThreadTestResult.getLastEntry(testResult.up!!.nsec) / 1e9
                            )
                        )

                        val ping = testResult.ping_shortest
                        if (ping < shortestPing) {
                            shortestPing = ping
                        }

                        if (testResult.pings.isNotEmpty()) {
                            result.pings.addAll(testResult.pings)
                        }

                        allDownBytes[i] = testResult.down!!.bytes
                        allDownNsecs[i] = testResult.down!!.nsec
                        allUpBytes[i] = testResult.up!!.bytes
                        allUpNsecs[i] = testResult.up!!.nsec

                        result.totalDownBytes += testResult.totalDownBytes
                        result.totalUpBytes += testResult.totalUpBytes

                        // aggregate speedItems
                        result.speedItems.addAll(testResult.speedItems)

                        // set client version
                        result.client_version = testResult.client_version
                    }
                }

                result.calculateDownload(allDownBytes, allDownNsecs)
                result.calculateUpload(allUpBytes, allUpNsecs)

                log("")
                log(String.format(Locale.US, "Total calculated bytes down: %d", result.bytes_download))
                log(String.format(Locale.US, "Total calculated time down:  %.3f s", result.nsec_download / 1e9))
                log(String.format(Locale.US, "Total calculated bytes up:   %d", result.bytes_upload))
                log(String.format(Locale.US, "Total calculated time up:    %.3f s", result.nsec_upload / 1e9))

                // get Connection Info from thread 1 (one thread must run)
                result.ip_local = results[0]!!.get()!!.ip_local
                result.ip_server = results[0]!!.get()!!.ip_server
                result.port_remote = results[0]!!.get()!!.port_remote
                result.encryption = results[0]!!.get()!!.encryption

                result.num_threads = realNumThreads

                result.ping_shortest = shortestPing

                result.speed_download = result.getDownloadSpeedBitPerSec() / 1e3
                result.speed_upload = result.getUploadSpeedBitPerSec() / 1e3

                log("")
                log(String.format(Locale.US, "Total Down: %.0f kBit/s", result.getDownloadSpeedBitPerSec() / 1e3))
                log(String.format(Locale.US, "Total UP:   %.0f kBit/s", result.getUploadSpeedBitPerSec() / 1e3))
                log(String.format(Locale.US, "Ping:       %.2f ms", shortestPing / 1e6))

                if (controlConnection != null) {
                    log("")
                    val testUUID = params.uuid
                    val testTime = controlConnection.getTestTime()
                    log(
                        String.format(
                            Locale.US, "time=%s, uuid=%s\n",
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(testTime)), testUUID
                        )
                    )
                }

                downBitPerSec.set(Math.round(result.getDownloadSpeedBitPerSec()))
                upBitPerSec.set(Math.round(result.getUploadSpeedBitPerSec()))

                log("end.")
                status = TestStatus.SPEEDTEST_END

                if (trafficService != null) {
                    txBytes = trafficService!!.getTotalTxBytes() - txBytes
                    rxBytes = trafficService!!.getTotalRxBytes() - rxBytes
                    result.setTotalTrafficMeasurement(TestMeasurement(rxBytes, txBytes, timeStampStart, System.nanoTime()))
                    result.setMeasurementMap(measurementMap)
                }

                return result
            } catch (e: ExecutionException) {
                log(e)
                abortTest(true)
                return null
            } catch (e: InterruptedException) {
                // Client is interrupted e.g. if a user of the Android app pushed the "Back"-button
                log("RMBTClient interrupted!")
                abortTest(false)
                throw e
            }
        } else {
            if (testStatus.get() == TestStatus.ERROR) {
                abortTest(true)
            } else {
                status = TestStatus.SPEEDTEST_END
            }
            return null
        }
    }

    private fun getCacheDir(): File? = this.cacheDir

    fun performVoipTest() {
        if (!aborted.get()) {
            try {
                // Implementation for Jitter and Packet loss
                val qosTestSettings = TestSettings()
                qosTestSettings.cacheFolder = getCacheDir()
                qosTestSettings.trafficService = TrafficServiceImpl()
                qosTestSettings.tracerouteServiceClazz = TracerouteAndroidImpl::class.java
                qosTestSettings.startTimeNs = controlConnection!!.getStartTimeNs()
                qosTestSettings.isUseSsl = params.isEncryption

                val voipTest: QualityOfServiceTest = JitterTest(this, qosTestSettings)
                voipReference.set(voipTest)
                status = TestStatus.PACKET_LOSS_AND_JITTER
                val voipResult = voipTest.call()

                val voipTestRsults = voipResult.results
                if (voipTestRsults.isNotEmpty()) {
                    val qoSTestResult = voipTestRsults[0]
                    val resultMap = qoSTestResult.resultMap

                    val voipTestResultHandler = VoipTestResultHandler()
                    val voipTestResult = voipTestResultHandler.convertResultsToObject(resultMap)
                    totalResult!!.voipTestResult = voipTestResult

                    val prefixOut = VoipTask.RESULT_VOIP_PREFIX + VoipTask.RESULT_OUTGOING_PREFIX
                    val prefixIn = VoipTask.RESULT_VOIP_PREFIX + VoipTask.RESULT_INCOMING_PREFIX

                    val meanJitterOut = resultMap[prefixOut + VoipTask.RESULT_MEAN_JITTER] as Long?
                    val meanJitterIn = resultMap[prefixIn + VoipTask.RESULT_MEAN_JITTER] as Long?
                    if (meanJitterIn != null && meanJitterOut != null) {
                        val meanJitter = (meanJitterIn + meanJitterOut) / 2
                        totalResult!!.jitterMeanNanos = meanJitter
                        this.jitter.set(meanJitter)
                    }

                    val callDuration = resultMap[VoipTask.RESULT_CALL_DURATION] as Long
                    jitterTestDurationNanos.set(callDuration)
                    val delay = resultMap[VoipTask.RESULT_DELAY] as Long
                    val outPacketsNumber = resultMap[prefixOut + VoipTask.RESULT_NUM_PACKETS] as Long
                    val inPacketsNumber = resultMap[prefixIn + VoipTask.RESULT_NUM_PACKETS] as Int

                    val total = callDuration.toInt() / delay.toInt()

                    val packetLossDown = (100f * ((total - inPacketsNumber).toFloat() / total.toFloat())).toInt()
                    val packetLossUp = (100f * ((total - outPacketsNumber).toFloat() / total.toFloat())).toInt()

                    totalResult!!.packetLossPercent = (packetLossDown + packetLossUp) / 2f

                    this.packetLossDown.set(packetLossDown.toLong())
                    this.packetLossUp.set(packetLossUp.toLong())

                    Timber.e("JITTER: %s, PL_DOWN: %s, PL_UP: %s", jitter, packetLossDown, packetLossUp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Timber.e("JITTER ERROR %s", e.message)
                log(e)
            }
        }
    }

    fun abortTest(error: Boolean): Boolean {
        println("RMBTClient stopTest")

        if (error) {
            setErrorStatus()
        } else {
            status = TestStatus.ABORTED
            controlConnection!!.abortStartedTest()
        }
        aborted.set(true)

        testThreadPool?.shutdownNow()

        stopInactivityCheck()
        return true
    }

    fun shutdown() {
        println("Shutting down RMBT thread pool...")
        testThreadPool?.shutdownNow()
        stopInactivityCheck()
        println("Shutdown finished.")
    }

    protected fun finalize() {
        testThreadPool?.shutdownNow()
        stopInactivityCheck()
    }

    fun setOutputCallback(outputCallback: RMBTOutputCallback?) {
        this.outputCallback = outputCallback
    }

    private fun resetSpeed() {
        lastCounter = 0
    }

    private fun getTotalSpeed(): Float {
        var sumTrans: Long = 0
        var maxTime: Long = 0

        val currentSpeed = CurrentSpeed()

        for (i in 0 until params.numThreads) {
            testTasks!![i]?.let {
                it.getCurrentSpeed(currentSpeed)

                if (currentSpeed.time > maxTime) {
                    maxTime = currentSpeed.time
                }
                sumTrans += currentSpeed.trans
            }
        }

        return if (maxTime == 0L) 0f else sumTrans.toFloat() / maxTime.toFloat() * 1e9f * 8.0f
    }

    private val speedMap = HashMap<Int, MutableList<SpeedItem>>()

    private fun getAvgSpeed(): Float {
        var sumDiffTrans: Long = 0
        var maxDiffTime: Long = 0

        val currentSpeed = CurrentSpeed()

        val currentIndex = lastCounter % KEEP_LAST_ENTRIES
        var diffReferenceIndex = (lastCounter - KEEP_LAST_ENTRIES + 1) % KEEP_LAST_ENTRIES
        if (diffReferenceIndex < 0) {
            diffReferenceIndex = 0
        }

        lastCounter++

        for (i in 0 until params.numThreads) {
            testTasks!![i]?.let {
                it.getCurrentSpeed(currentSpeed)

                lastTime[i][currentIndex] = currentSpeed.time
                lastTransfer[i][currentIndex] = currentSpeed.trans

                var speedList = speedMap[i]
                if (speedList == null) {
                    speedList = ArrayList()
                    speedMap[i] = speedList
                }

                speedList.add(SpeedItem(false, i, currentSpeed.time, currentSpeed.trans))

                val diffTime = currentSpeed.time - lastTime[i][diffReferenceIndex]
                val diffTrans = currentSpeed.trans - lastTransfer[i][diffReferenceIndex]

                if (diffTime > maxDiffTime) {
                    maxDiffTime = diffTime
                }
                sumDiffTrans += diffTrans
            }
        }

        return if (maxDiffTime == 0L) 0f else sumDiffTrans.toFloat() / maxDiffTime.toFloat() * 1e9f * 8.0f
    }

    fun getIntermediateResult(iResult: IntermediateResult?): IntermediateResult {
        val r = iResult ?: IntermediateResult()
        r.status = testStatus.get()
        r.remainingWait = 0
        val diffTime = System.nanoTime() - statusChangeTime.get()
        when (r.status) {
            TestStatus.WAIT -> {
                r.progress = 0f
                r.remainingWait = params.startTime - System.currentTimeMillis()
            }
            TestStatus.INIT -> r.progress = diffTime.toFloat() / durationInitNano
            TestStatus.PACKET_LOSS_AND_JITTER -> {
                r.jitter = jitter.get()
                r.packetLossUp = packetLossUp.get()
                r.packetLossDown = packetLossDown.get()
                r.progress = getJitterProgress()
            }
            TestStatus.PING -> {
                r.progress = getPingProgress()
                // getting final result values
                if (enabledJitterAndPacketLoss) {
                    r.jitter = jitter.get()
                    r.packetLossUp = packetLossUp.get()
                    r.packetLossDown = packetLossDown.get()
                }
            }
            TestStatus.DOWN -> {
                r.progress = diffTime.toFloat() / durationDownNano
                downBitPerSec.set(Math.round(getAvgSpeed()).toLong())
            }
            TestStatus.INIT_UP -> r.progress = 0f
            TestStatus.UP -> {
                r.progress = diffTime.toFloat() / durationUpNano
                upBitPerSec.set(Math.round(getAvgSpeed()).toLong())
            }
            TestStatus.SPEEDTEST_END -> r.progress = 1f
            TestStatus.ERROR, TestStatus.ABORTED -> r.progress = 0f
            else -> {}
        }

        if (r.progress > 1) {
            r.progress = 1f
        }

        r.pingNano = pingNano.get()
        r.downBitPerSec = downBitPerSec.get()
        r.upBitPerSec = upBitPerSec.get()
        r.jitter = jitter.get()
        r.packetLossUp = packetLossUp.get()
        r.packetLossDown = packetLossDown.get()

        r.setLogValues()

        return r
    }

    fun getJitterProgress(): Float {
        return if (jitter.get() != -1L) {
            Timber.d("JITTER PROGRESS " + 100 + "%")
            1f
        } else {
            val currentTime = System.nanoTime()
            val jitterStartTimeLong = jitterStartTime.get()
            val l = (currentTime - jitterStartTimeLong).toFloat()
            Log.e(
                "PROGRESS SEGMENTS:",
                "start:   " + jitterStartTimeLong + "              now:   " + currentTime + "            diff:   " + l + "   " + (l / jitterTestDurationNanos.get())
            )
            Timber.d("JITTER PROGRESS " + (l * 100) / jitterTestDurationNanos.get() + "%")
            l / jitterTestDurationNanos.get()
        }
    }

    var status: TestStatus
        get() = testStatus.get()
        set(value) {
            testStatus.set(value)
            statusChangeTime.set(System.nanoTime())
            if (value == TestStatus.INIT_UP) {
                // DOWN is finished
                downBitPerSec.set(Math.round(getTotalSpeed()).toLong())
                resetSpeed()
            }

            if (value == TestStatus.INIT) {
                jitterStartTime.set(-1)
            }

            Timber.d("STATUS CHANGED TO: " + value.name)

            // JITTER PACKET LOSS
            if (value == TestStatus.PACKET_LOSS_AND_JITTER) {
                if (jitterStartTime.get() == -1L) {
                    jitterStartTime.set(System.nanoTime())
                }
            }
        }

    val statusBeforeError: TestStatus?
        get() = statusBeforeErrorRef.get()

    fun startTrafficService(threadId: Int, status: TestStatus) {
        if (trafficService != null) {
            // a concurrent map is needed in case multiple threads want to start the traffic service
            // only the first thread should be able to start the service
            val tm = TestMeasurement(status.toString(), trafficService)
            val previousTm = measurementMap.putIfAbsent(status, tm)
            if (previousTm == null) {
                tm.start(threadId)
            }
        }
    }

    fun stopTrafficMeasurement(threadId: Int, status: TestStatus) {
        val testMeasurement = measurementMap[status]
        testMeasurement?.stop(threadId)
    }

    fun getTrafficMeasurementMap(): Map<TestStatus, TestMeasurement> = measurementMap

    fun sendResult(additionalValues: JSONObject?, headerValue: String?) {
        if (controlConnection != null) {
            val error = controlConnection.sendTestResult(totalResult!!, additionalValues, headerValue)
            if (error != null) {
                setErrorStatus()
                log("Error sending Result...")
                log(error)
            }
        }
    }

    fun sendQoSResult(qosResult: QoSResultCollector, headerValue: String?) {
        if (controlConnection != null) {
            val error = controlConnection.sendQoSResult(totalResult, qosResult.toJson(), headerValue)
            if (error != null) {
                setErrorStatus()
                log("Error sending QoS Result...")
                log(error)
            }
        }
    }

    private fun setErrorStatus() {
        val lastStatus = testStatus.getAndSet(TestStatus.ERROR)
        if (lastStatus != TestStatus.ERROR) {
            statusBeforeErrorRef.set(lastStatus)
        }
    }

    fun log(text: CharSequence?) {
        if (outputToStdout) {
            println(text)
        }
        outputCallback?.log(text!!)
    }

    fun log(e: Exception) {
        if (outputToStdout) {
            e.printStackTrace(System.out)
        }
        outputCallback?.log(String.format(Locale.US, "Error: %s", e.message))
    }

    fun setPing(shortestPing: Long) {
        pingNano.set(shortestPing)
    }

    fun updatePingStatus(tsStart: Long, pingsDone: Int, tsLastPing: Long) {
        pingTsStart.set(tsStart)
        pingNumDome.set(pingsDone)
        pingTsLastPing.set(tsLastPing)
    }

    private fun getPingProgress(): Float {
        val start = pingTsStart.get()

        if (start == -1L) { // not yet started
            return 0f
        }

        val numDone = pingNumDome.get()
        val lastPing = pingTsLastPing.get()
        val now = System.nanoTime()

        val numPings = params.numPings

        if (numPings <= 0) { // nothing to do
            return 1f
        }

        val factorPerPing = 1f / numPings.toFloat()
        val base = factorPerPing * numDone

        val approxTimePerPing: Long = if (numDone == 0 || lastPing == -1L) {
            // during first ping, assume 100ms
            100000000
        } else {
            (lastPing - start) / numDone
        }

        var factorLastPing = (now - lastPing).toFloat() / approxTimePerPing.toFloat()
        if (factorLastPing < 0) {
            factorLastPing = 0f
        }
        if (factorLastPing > 1) {
            factorLastPing = 1f
        }

        var result = base + factorLastPing * factorPerPing
        val pingDurationMs = params.doPingIntervalMilliseconds.toLong()
        if (pingDurationMs > 0) {
            val elapsedMs = (now - pingTsStart.get()) / 1000000
            val progressBasedOnDuration = Math.min(1f, Math.max(0f, elapsedMs / pingDurationMs.toFloat()))
            result = Math.min(result, progressBasedOnDuration)
        }

        if (result < 0) {
            return 0f
        }
        if (result > 1) {
            return 1f
        }

        return result
    }

    val publicIP: String?
        get() = controlConnection?.getRemoteIp()

    val serverName: String?
        get() = controlConnection?.getServerName()

    val provider: String?
        get() = controlConnection?.getProvider()

    val testUuid: String?
        get() = controlConnection?.getTestUuid()

    val startTimeMillis: Long
        get() = controlConnection?.getStartTimeMillis() ?: 0

    val totalTestResult: TotalTestResult
        get() = totalResult!!

    override fun onSpeedDataChanged(threadId: Int, bytes: Long, timestampNanos: Long, isUpload: Boolean) {
        Timber.v("speed upload: $isUpload id:  $threadId bytes: $bytes timestampNs: $timestampNanos")
        commonCallback?.onSpeedDataChanged(threadId, bytes, timestampNanos, isUpload)
        measurementLastUpdate = System.currentTimeMillis()
        planInactivityCheck()
    }

    override fun onPingDataChanged(clientPing: Long, serverPing: Long, timeNs: Long) {
        Timber.v("ping: %s", clientPing)
        commonCallback?.let {
            val startTime = controlConnection?.getStartTimeNs() ?: 0
            it.onPingDataChanged(clientPing, serverPing, timeNs - startTime)
        }
        measurementLastUpdate = System.currentTimeMillis()
        planInactivityCheck()
    }

    override fun onClientReady(testUUID: String, loopUUID: String?, testToken: String, testStartTimeNanos: Long, threadNumber: Int) {
        // leave empty
    }

    override fun onTestCompleted(result: TotalTestResult, waitQoSResults: Boolean) {
        // leave empty
    }

    override fun onQoSTestCompleted(qosResult: QoSResultCollector?) {
        // leave empty
    }

    override fun onTestStatusUpdate(status: TestStatus?) {
        // leave empty
    }

    companion object {
        private val COMMON_THREAD_POOL: ExecutorService = Executors.newCachedThreadPool()

        private const val MIN_DIFF_TIME = 100000000L // 100 ms

        private const val KEEP_LAST_ENTRIES = 20

        /*------------------------------------ V2 tests --------------------------------------*/
        const val TASK_UDP = "udp"
        const val TASK_TCP = "tcp"
        const val TASK_DNS = "dns"
        const val TASK_VOIP = "voip"
        const val TASK_NON_TRANSPARENT_PROXY = "non_transparent_proxy"
        const val TASK_HTTP = "http_proxy"
        const val TASK_WEBSITE = "website"
        const val TASK_TRACEROUTE = "traceroute"
        const val TASK_TRACEROUTE_MASKED = "traceroute_masked"
        const val TASK_JITTER = "jitter" // Voip test to be performed in main test

        private const val MEASUREMENT_INACTIVITY_THRESHOLD_SECONDS = 120L
        private const val MEASUREMENT_INACTIVITY_CHECKER_PERIOD_SECONDS = 1L

        fun getCommonThreadPool(): ExecutorService = COMMON_THREAD_POOL

        fun getInstance(
            host: String,
            pathPrefix: String?,
            port: Int,
            encryption: Boolean,
            geoInfo: ArrayList<String>?,
            uuid: String,
            clientType: String?,
            clientName: String?,
            clientVersion: String?,
            overrideParams: RMBTTestParameter?,
            additionalValues: JSONObject?,
            headerValue: String?,
            cacheDir: File?
        ): RMBTClient? {
            return getInstance(
                host, pathPrefix, port, encryption, geoInfo, uuid, clientType,
                clientName, clientVersion, overrideParams, additionalValues, headerValue, cacheDir, null, false
            )
        }

        /**
         * Gets a new instance of RMBTClient or "null", if the connection to the given ControlServer cannot be established
         */
        fun getInstance(
            host: String,
            pathPrefix: String?,
            port: Int,
            encryption: Boolean,
            geoInfo: ArrayList<String>?,
            uuid: String,
            clientType: String?,
            clientName: String?,
            clientVersion: String?,
            overrideParams: RMBTTestParameter?,
            additionalValues: JSONObject?,
            headerValue: String?,
            cacheDir: File?,
            errorSet: MutableSet<ErrorStatus>?,
            enabledJitterAndPacketLoss: Boolean
        ): RMBTClient? {
            val controlConnection = ControlServerConnection()

            val error = controlConnection.requestNewTestConnection(
                host, pathPrefix, port, encryption, geoInfo,
                uuid, clientType, clientName, clientVersion, additionalValues, headerValue
            )

            if (controlConnection.getLastErrorList() != null && errorSet != null) {
                errorSet.addAll(controlConnection.getLastErrorList()!!)
            }

            if (error != null) {
                println(error)
                return null
            }

            val errorNewTest = controlConnection.requestQoSTestParameters(
                host, pathPrefix, port, encryption, geoInfo,
                uuid, clientType, clientName, clientVersion, additionalValues, headerValue
            )

            if (errorNewTest != null) {
                println(errorNewTest)
                return null
            }

            val params = controlConnection.getTestParameter(overrideParams)

            return RMBTClient(params, controlConnection, cacheDir, enabledJitterAndPacketLoss)
        }

        fun getInstance(params: RMBTTestParameter): RMBTClient {
            return RMBTClient(params, null)
        }

        fun getTrustingManager(): TrustManager {
            return object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {
                }

                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {
                }
            }
        }

        fun getSSLContext(caResource: String?, certResource: String?): SSLContext {
            var caTmp: X509Certificate? = null
            try {
                if (caResource != null) {
                    val cf = CertificateFactory.getInstance("X.509")
                    caTmp = cf.generateCertificate(RMBTClient::class.java.classLoader!!.getResourceAsStream(caResource)) as X509Certificate
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val ca = caTmp

            var certTmp: X509Certificate? = null
            try {
                if (certResource != null) {
                    val cf = CertificateFactory.getInstance("X.509")
                    certTmp = cf.generateCertificate(RMBTClient::class.java.classLoader!!.getResourceAsStream(certResource)) as X509Certificate
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val cert = certTmp

            val tm: TrustManager = if (cert == null) {
                getTrustingManager()
            } else {
                object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return if (ca == null) arrayOf(cert) else arrayOf(ca)
                    }

                    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {
                    }

                    override fun checkServerTrusted(certs: Array<X509Certificate>?, authType: String) {
                        if (certs == null) {
                            throw CertificateException()
                        }
                        for (c in certs) {
                            if (cert == c) {
                                return
                            }
                        }
                        throw CertificateException()
                    }
                }
            }

            val trustManagers = arrayOf(tm)

            val sc = SSLContext.getInstance(Config.RMBT_ENCRYPTION_STRING)
            sc.init(null, trustManagers, java.security.SecureRandom())
            return sc
        }
    }
}
