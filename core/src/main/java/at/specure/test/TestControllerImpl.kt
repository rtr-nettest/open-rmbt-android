package at.specure.test

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import at.rtr.rmbt.client.QualityOfServiceTest
import at.rtr.rmbt.client.RMBTClient
import at.rtr.rmbt.client.RMBTClientCallback
import at.rtr.rmbt.client.TracerouteAndroidImpl
import at.rtr.rmbt.client.TrafficServiceImpl
import at.rtr.rmbt.client.WebsiteTestServiceImpl
import at.rtr.rmbt.client.helper.IntermediateResult
import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.rtr.rmbt.client.v2.task.service.TestSettings
import at.rtr.rmbt.util.model.shared.LoopModeSettings
import at.rtr.rmbt.util.model.shared.exception.ErrorStatus
import at.specure.config.Config
import at.specure.data.ClientUUID
import at.specure.data.MeasurementServers
import at.specure.measurement.MeasurementState
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.json.JSONObject
import timber.log.Timber
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.math.floor

private const val KEY_TEST_COUNTER = "testCounter"
private const val KEY_PREVIOUS_TEST_STATUS = "previousTestStatus"
private const val KEY_LOOP_MODE_SETTINGS = "loopmode_info"
private const val KEY_SERVER_SELECTION_ENABLED = "user_server_selection"
private const val KEY_SERVER_PREFERRED = "prefer_server"
private const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode"
private const val KEY_LOOP_MODE_ENABLED = "user_loop_mode"
private const val KEY_MEASUREMENT_TYPE = "measurement_type_flag" // used on the control server to determine type for signal measurement
private const val KEY_TEMPERATURE = "temperature"
private const val KEY_COVERAGE = "coverage"

private const val TEST_MAX_TIME = 3000
private const val MAX_VALUE_UNFINISHED_TEST = 0.9f

class TestControllerImpl(
    private val context: Context,
    private val config: Config,
    private val clientUUID: ClientUUID,
    private val connectivityManager: ConnectivityManager,
    private val measurementServer: MeasurementServers
) : TestController {

    private var lastNetwork: Network? = null
    private var job: Job? = null
    private var clientJob: Job? = null
    private val result: IntermediateResult by lazy { IntermediateResult() }
    private var _testUUID: String? = null
    private var _listener: TestProgressListener? = null
    private var _testStartTimeNanos = 0L

    override val testUUID: String?
        get() = _testUUID

    override val isRunning: Boolean
        get() = job != null

    override val testStartTimeNanos: Long
        get() = _testStartTimeNanos

    private var previousJitterProgress = -1
    private var previousDownloadProgress = -1
    private var previousUploadProgress = -1

    private var client: RMBTClient? = null
    private var qosTest: QualityOfServiceTest? = null

    private var finalDownloadValuePosted = false
    private var finalUploadValuePosted = false

    override fun reset() {
        lastNetwork = null
        client?.shutdown()
        qosTest?.interrupt()

        if (clientJob == null) {
            Timber.w("client job is already stopped")
        } else {
            clientJob?.cancel()
        }

        if (job == null) {
            Timber.w("Runner is already stopped")
        } else {
            job?.cancel()
        }

        job = null
        clientJob = null
        _listener = null
        _testUUID = null
        finalDownloadValuePosted = false
        finalUploadValuePosted = false
        previousDownloadProgress = -1
        previousUploadProgress = -1
        _listener = null
    }

    override fun start(
        deviceInfo: DeviceInfo,
        loopModeUUID: String?,
        loopLocalUUID: String?,
        loopTestCount: Int,
        listener: TestProgressListener,
        clientCallback: RMBTClientCallback
    ) {
        if (job != null) {
            Timber.w("Runner is already started")
            return
        }

        lastNetwork = null
        _listener = listener

        job = GlobalScope.async {

            previousDownloadProgress = -1
            previousUploadProgress = -1
            finalDownloadValuePosted = false
            finalUploadValuePosted = false

            setState(MeasurementState.INIT, 0)

            var geoInfo: ArrayList<String>? = null
            deviceInfo.location?.let {
                geoInfo = arrayListOf(
                    it.time.toString(),
                    it.lat.toString(),
                    it.long.toString(),
                    it.accuracy.toString(),
                    it.altitude.toString(),
                    it.bearing.toString(),
                    it.speed.toString(),
                    it.provider
                )
            }

            val loopSettings: LoopModeSettings? = if (config.loopModeEnabled) {
                LoopModeSettings(
                    config.loopModeWaitingTimeMin,
                    config.loopModeDistanceMeters,
                    config.loopModeNumberOfTests,
                    loopTestCount,
                    loopModeUUID
                )
            } else null

            val gson = Gson()
            val errorSet = mutableSetOf<ErrorStatus>()
            val additionalValues = JSONObject(gson.toJson(deviceInfo))
                .put(KEY_TEST_COUNTER, config.testCounter)
                .put(KEY_PREVIOUS_TEST_STATUS, config.previousTestStatus)

            additionalValues.put(KEY_TEMPERATURE, deviceInfo.temperature)

            //add flag for coverage mode only when it is enabled
            if (config.coverageModeEnabled == true) {
                additionalValues.put(KEY_COVERAGE, config.coverageModeEnabled)
            }

            loopSettings?.let {
                additionalValues.put(KEY_LOOP_MODE_SETTINGS, JSONObject(gson.toJson(it, LoopModeSettings::class.java)))
            }

            if (config.expertModeEnabled) {
                additionalValues.put(KEY_SERVER_SELECTION_ENABLED, true)
                measurementServer.selectedMeasurementServer?.let {
                    additionalValues.put(KEY_SERVER_PREFERRED, it.uuid)
                }
            }

            if (config.developerModeIsEnabled) {
                additionalValues.put(KEY_DEVELOPER_MODE_ENABLED, true)
            }

            if (config.loopModeEnabled) {
                additionalValues.put(KEY_LOOP_MODE_ENABLED, true)
                additionalValues.put(KEY_MEASUREMENT_TYPE, SignalMeasurementType.LOOP_ACTIVE.signalTypeName)
            } else {
                additionalValues.put(KEY_MEASUREMENT_TYPE, SignalMeasurementType.REGULAR.signalTypeName)
            }

            client = RMBTClient.getInstance(
                config.controlServerHost,
                if (config.headerValue.isNullOrEmpty()) null else config.rmbtClientRequestsPathPrefix,
                config.controlServerPort,
                config.controlServerUseSSL,
                geoInfo,
                clientUUID.value,
                deviceInfo.clientType,
                deviceInfo.clientName,
                deviceInfo.softwareVersionName,
                null,
                additionalValues,
                config.headerValue,
                context.cacheDir,
                errorSet,
                config.performJitterAndPacketLossTest
            )

            val client = client

            if (client == null || errorSet.isNotEmpty()) {
                Timber.w("Client has errors")
                job?.cancel()
                job = null
                _listener?.onError()
                return@async
            }

            client.commonCallback = clientCallback
            client.trafficService = TrafficServiceImpl()

            val connection = client.controlConnection
            Timber.i("Client UUID: ${connection?.clientUUID}")
            Timber.i("Test UUID: ${connection.testUuid}")
            Timber.i("Server Name: ${connection?.serverName}")
            Timber.i("Loop Id: ${connection?.loopUuid}")
            Timber.i("Start Time Nanos: ${connection?.startTimeNs}")

            _testStartTimeNanos = connection?.startTimeNs ?: 0
            _testUUID = connection.testUuid

            _listener?.onClientReady(_testUUID!!, connection.loopUuid, loopLocalUUID, _testStartTimeNanos)

            val skipQoSTests = !config.shouldRunQosTest

            clientJob = GlobalScope.async {
                @Suppress("BlockingMethodInNonBlockingContext")
                val result = client.runTest()

                if (!finalUploadValuePosted) {
                    val speed = floor(client.totalTestResult.speed_upload + 0.5).toLong() * 1000
                    _listener?.onUploadSpeedChanged(-1, speed)
                    finalUploadValuePosted = true
                }

                // clientCallback.onTestCompleted(result, !skipQoSTests)
                if (!skipQoSTests) { // needs to prevent calling onTestCompleted and finishing before unimplemented QoS phase
                    val qosTestSettings = TestSettings()
                    qosTestSettings.cacheFolder = context.cacheDir
                    qosTestSettings.websiteTestService = WebsiteTestServiceImpl(context)
                    qosTestSettings.tracerouteServiceClazz = TracerouteAndroidImpl::class.java
                    qosTestSettings.trafficService = TrafficServiceImpl()
                    qosTestSettings.startTimeNs = _testStartTimeNanos
                    qosTestSettings.isUseSsl = config.qosSSL

                    // get default dns servers
                    qosTestSettings.defaultDnsResolvers = getDnsServers()

                    qosTest = QualityOfServiceTest(client, qosTestSettings)
                    client.status = TestStatus.QOS_TEST_RUNNING
                    val qosResult = qosTest?.call()
                    Timber.d("qos finished")
                    client.status = TestStatus.QOS_END
                    config.lastQosTestExecutionTimestampMillis = System.currentTimeMillis()
                    clientCallback.onQoSTestCompleted(qosResult)
                }
            }

            var currentStatus = TestStatus.WAIT

            while (!currentStatus.isFinalState(skipQoSTests)) {
                currentStatus = client.status
                Timber.v(currentStatus.name)

                clientCallback.onTestStatusUpdate(currentStatus)
                checkIllegalNetworkChange(client)

                when (currentStatus) {
                    TestStatus.WAIT -> handleWait()
                    TestStatus.INIT -> handleInit(client)
                    TestStatus.PING -> handlePing(client)
                    TestStatus.DOWN -> handleDown(client)
                    TestStatus.PACKET_LOSS_AND_JITTER -> handleJitterAndPacketLoss(client)
                    TestStatus.INIT_UP -> handleInitUp()
                    TestStatus.UP -> handleUp(client)
                    TestStatus.SPEEDTEST_END -> handleSpeedTestEnd(client, clientCallback, skipQoSTests)
                    TestStatus.QOS_TEST_RUNNING -> handleQoSRunning(qosTest)
                    TestStatus.QOS_END -> handleQoSEnd(client, clientCallback)
                    TestStatus.ERROR -> handleError(client)
                    TestStatus.END -> handleEnd(client, clientCallback)
                    TestStatus.ABORTED -> handleAbort(client)
                }

                if (currentStatus.isFinalState(skipQoSTests)) {
                    config.testCounter++
                    config.previousTestStatus = if (currentStatus == TestStatus.ERROR) {
                        var errorStatus = "ERROR"
                        client.statusBeforeError?.let {
                            errorStatus = "${errorStatus}_$it"
                        }
                        errorStatus
                    } else TestStatus.END.name

                    client.commonCallback = null
                    client.shutdown()
                    qosTest?.interrupt()

                    this@TestControllerImpl.client = null
                    qosTest = null
                    if (currentStatus != TestStatus.ERROR) {
                        _listener?.onFinish()
                    }
                    stop()
                    _testUUID = null
                } else {
                    delay(100)
                }
            }
        }
    }

    private fun handleWait() {
        setState(MeasurementState.INIT, 0)
    }

    private fun handleInit(client: RMBTClient) {
        client.getIntermediateResult(result)
        setState(MeasurementState.INIT, (result.progress * 100).toInt())
    }

    private fun handlePing(client: RMBTClient) {
        client.getIntermediateResult(result)
        val progress = (result.progress * 100).toInt()

        setState(MeasurementState.PING, progress)
        Timber.d("PING TCI: ${(result.progress * 100).toInt()}")
    }

    private fun handleDown(client: RMBTClient) {
        client.getIntermediateResult(result)
        val progress = (result.progress * 100).toInt()
        if (progress < 10) {
            if (client.totalTestResult.jitterMeanNanos > 0) {
                Timber.d("Jitter result nanos: ${client.totalTestResult.jitterMeanNanos}")
                setState(MeasurementState.JITTER_AND_PACKET_LOSS, 100)
                _listener?.onJitterChanged(client.totalTestResult.jitterMeanNanos)
                val packetLoss = client.totalTestResult.packetLossPercent.toInt()
                if (packetLoss >= 0) {
                    _listener?.onPacketLossChanged(packetLoss)
                }
            }
        } else {
            if (progress != previousDownloadProgress) {
                setState(MeasurementState.DOWNLOAD, progress)
                if (result.pingNano > 0) {
                    _listener?.onPingChanged(result.pingNano)
                }
                val value = result.downBitPerSec
                _listener?.onDownloadSpeedChanged(progress, value)
                previousDownloadProgress = progress
            }
        }
    }

    private fun handleJitterAndPacketLoss(client: RMBTClient) {
        client.getIntermediateResult(result)
        val progress = (result.progress * 100).toInt()
        Timber.d("JITTER PROGRESS TCI: $progress")
        if (progress < 10) {
            setState(MeasurementState.PING, 100)
            if (result.pingNano > 0) {
                _listener?.onPingChanged(result.pingNano)
            }
        } else {
            setState(MeasurementState.JITTER_AND_PACKET_LOSS, progress)
            if (client.totalTestResult.jitterMeanNanos > 0) {
                _listener?.onJitterChanged(client.totalTestResult.jitterMeanNanos)
            }
            val packetLoss = client.totalTestResult.packetLossPercent.toInt()
            if (packetLoss >= 0) {
                _listener?.onPacketLossChanged(packetLoss)
            }
            if (progress != previousJitterProgress) {
                setState(MeasurementState.JITTER_AND_PACKET_LOSS, progress)
                if (result.pingNano > 0) {
                    _listener?.onPingChanged(result.pingNano)
                }
                val value = result.downBitPerSec
                _listener?.onDownloadSpeedChanged(progress, value)
                previousJitterProgress = progress
            }
        }
    }

    private fun handleInitUp() {
        setState(MeasurementState.UPLOAD, 0)
    }

    private fun handleUp(client: RMBTClient) {
        client.getIntermediateResult(result)
        val progress = (result.progress * 100).toInt()
        if (progress != previousUploadProgress) {
            setState(MeasurementState.UPLOAD, (result.progress * 100).toInt())
            if (result.pingNano > 0) {
                _listener?.onPingChanged(result.pingNano)
            }
            val value = result.upBitPerSec
            Timber.e("Progressy2: ${result.upBitPerSec} - $value")
            _listener?.onUploadSpeedChanged(progress, value)
            previousUploadProgress = progress
        }

        if (!finalDownloadValuePosted) {
            val value = result.downBitPerSec
            Timber.w("Progressy2: ${result.upBitPerSec} - $value")
            _listener?.onDownloadSpeedChanged(-1, value)
            finalDownloadValuePosted = true
        }
    }

    private fun handleSpeedTestEnd(client: RMBTClient, callback: RMBTClientCallback, skipQoSTest: Boolean) {
        if (skipQoSTest) {
            setState(MeasurementState.FINISH, 0)
            callback.onTestCompleted(client.totalTestResult, false)
        }
    }

    private fun handleQoSRunning(qosTest: QualityOfServiceTest?) {
        qosTest ?: return
        Timber.i("${TestStatus.QOS_TEST_RUNNING} progress: ${qosTest.progress}/${qosTest.testSize}")
        val progress = ((qosTest.progress.toDouble() / qosTest.testSize) * 100).toInt()
        setState(MeasurementState.QOS, progress)

        val counterMap = qosTest.testGroupCounterMap
        val testMap = qosTest.testMap

        val progressMap = mutableMapOf<QoSTestResultEnum, Int>()

        counterMap.forEach { entry ->
            val key = entry.key!!
            val counter = entry.value!!

            var testGroupProgress = 0f
            val currentTimestamp = System.nanoTime()

            val taskList = testMap[key]
            if (taskList != null && counter.value < counter.target) {
                taskList.forEach { task ->
                    if (task.hasStarted()) {
                        val runningMs = TimeUnit.NANOSECONDS.toMillis(task.getRelativeDurationNs(currentTimestamp))
                        if (runningMs >= TEST_MAX_TIME * MAX_VALUE_UNFINISHED_TEST && !task.hasFinished()) {
                            testGroupProgress += (1f / counter.target) * MAX_VALUE_UNFINISHED_TEST
                        } else if (!task.hasFinished()) {
                            testGroupProgress += (1f / counter.target) * (runningMs / TEST_MAX_TIME)
                        }
                    }
                }

                testGroupProgress += counter.value.toFloat() / counter.target
                testGroupProgress *= 100f
            } else if (counter.value == counter.target) {
                testGroupProgress = 100f
            } else {
                Timber.w("Task $key not found!")
            }

            progressMap[key] = testGroupProgress.toInt()
            Timber.i("$key : $testGroupProgress")
        }

        _listener?.onQoSTestProgressUpdate(qosTest.progress, qosTest.testSize, progressMap)
    }

    private fun checkIllegalNetworkChange(client: RMBTClient) {
        var newNetworkWifi = false
        var newNetworkCellular = false
        var newNetworkEthernet = false
        var lastNetworkWifi = false
        var lastNetworkCellular = false
        var lastNetworkEthernet = false

        val activeNetwork = connectivityManager.activeNetwork
        activeNetwork?.let {
            val newNetworkCapabilities = connectivityManager.getNetworkCapabilities(it)
            newNetworkCapabilities?.let { nc ->
                newNetworkCellular = nc.hasTransport(TRANSPORT_CELLULAR)
                newNetworkWifi = nc.hasTransport(TRANSPORT_WIFI)
                newNetworkEthernet = nc.hasTransport(TRANSPORT_ETHERNET)
            }
        }

        lastNetwork?.let {
            val lastNetworkCapabilities = connectivityManager.getNetworkCapabilities(it)
            lastNetworkCapabilities?.let { nc ->
                lastNetworkCellular = nc.hasTransport(TRANSPORT_CELLULAR)
                lastNetworkWifi = nc.hasTransport(TRANSPORT_WIFI)
                lastNetworkEthernet = nc.hasTransport(TRANSPORT_ETHERNET)
            }
        }

        if (lastNetwork != null && ((lastNetworkCellular != newNetworkCellular) || (lastNetworkEthernet != newNetworkEthernet) || (lastNetworkWifi != newNetworkWifi))) {
            Timber.e("XDTE: Illegal network change detected \n last: $lastNetwork \n\n active: $activeNetwork")
            Timber.e("XDTE: Illegal network change detected \n type last vs  new:\n cellular:   $lastNetworkCellular    vs  $newNetworkCellular\nwifi:   $lastNetworkWifi    vs  $newNetworkWifi\nethernet:   $lastNetworkEthernet    vs  $newNetworkEthernet\n")
            handleError(client)
            stop()
        } else {
            lastNetwork = activeNetwork
        }
    }

    private fun handleQoSEnd(client: RMBTClient, clientCallback: RMBTClientCallback) {
        setState(MeasurementState.FINISH, 0)
        clientCallback.onTestCompleted(client.totalTestResult, false)
        _testUUID = null
    }

    private fun handleError(client: RMBTClient) {
        Timber.d("XDTE: Handle error ${client.errorMsg}")
        _listener?.onError()
        _testUUID = null
    }

    private fun handleAbort(client: RMBTClient) {
        Timber.e("${TestStatus.ABORTED} handling not implemented")
        _testUUID = null
    }

    private fun handleEnd(client: RMBTClient, clientCallback: RMBTClientCallback) {
        setState(MeasurementState.FINISH, 0)
        clientCallback.onTestCompleted(client.totalTestResult, false)
        _testUUID = null
    }

    private fun setState(state: MeasurementState, progress: Int) {
        _listener?.onProgressChanged(state, progress)
    }

    override fun stop() {
        client?.shutdown()
        qosTest?.interrupt()

        if (clientJob == null) {
            Timber.w("client job is already stopped")
        } else {
            clientJob?.cancel()
        }

        if (job == null) {
            Timber.w("Runner is already stopped")
        } else {
            job?.cancel()
        }

        job = null
        clientJob = null
        _listener?.onPostFinish()
        _listener = null
    }

    private fun TestStatus.isFinalState(skipQoSTest: Boolean) = this == TestStatus.ABORTED ||
            this == TestStatus.END ||
            this == TestStatus.ERROR ||
            (this == TestStatus.SPEEDTEST_END && skipQoSTest) ||
            this == TestStatus.QOS_END

    private fun getDnsServers(): List<InetAddress>? {
        val servers = mutableListOf<InetAddress>()
        val networks = arrayOf(connectivityManager.activeNetwork)
        for (i in networks.indices) {
            val linkProperties = connectivityManager.getLinkProperties(networks[i])
            if (linkProperties != null) {
                servers.addAll(linkProperties.dnsServers)
            }
        }
        servers.forEach {
            Timber.d("DNS Server: ${it.hostName} (${it.hostAddress})")
        }
        return servers
    }
}