package at.specure.test

import at.rtr.rmbt.client.RMBTClient
import at.rtr.rmbt.client.TrafficServiceImpl
import at.rtr.rmbt.client.helper.IntermediateResult
import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.util.model.shared.exception.ErrorStatus
import at.specure.config.Config
import at.specure.data.ClientUUID
import at.specure.measurement.MeasurementState
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class TestControllerImpl(private val config: Config, private val clientUUID: ClientUUID) : TestController {

    private var job: Job? = null

    private val result: IntermediateResult by lazy { IntermediateResult() }

    private var _testUUID: String? = null

    private var _listener: TestProgressListener? = null

    override val testUUID: String?
        get() = _testUUID

    override val isRunning: Boolean
        get() = job != null

    private var previousDownloadProgress = -1
    private var previousUploadProgress = -1

    override fun start(listener: TestProgressListener, deviceInfo: DeviceInfo) {
        Timber.d("Start---")
        if (job != null) {
            Timber.w("Runner is already started")
            return
        }

        _listener = listener

        job = GlobalScope.async {

            previousDownloadProgress = -1
            previousUploadProgress = -1

            setState(MeasurementState.IDLE, 0)

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

            val errorSet = mutableSetOf<ErrorStatus>()

            val client = RMBTClient.getInstance(
                config.controlServerHost,
                null,
                config.controlServerPort,
                config.controlServerUseSSL,
                geoInfo,
                clientUUID.value,
                deviceInfo.clientType,
                deviceInfo.clientName,
                deviceInfo.softwareVersionName,
                null,
                JSONObject(Gson().toJson(deviceInfo)),
                errorSet
            )

            if (client == null || errorSet.isNotEmpty()) {
                Timber.w("Client has errors")
                _listener?.onError()
                return@async
            }
            client.commonCallback = listener
            client.trafficService = TrafficServiceImpl()
            val connection = client.controlConnection
            Timber.i("Client UUID: ${connection?.clientUUID}")
            Timber.i("Test UUID: ${connection.testUuid}")
            Timber.i("Server Name: ${connection?.serverName}")
            Timber.i("Loop Id: ${connection?.loopUuid}")

            _testUUID = connection.testUuid
            _listener?.onClientReady(_testUUID!!)

            GlobalScope.async {
                @Suppress("BlockingMethodInNonBlockingContext")
                client.runTest()
            }

            var currentStatus = TestStatus.WAIT
            while (!currentStatus.isFinalState()) {
                currentStatus = client.status
                Timber.v(currentStatus.name)
                when (currentStatus) {
                    TestStatus.WAIT -> handleWait()
                    TestStatus.INIT -> handleInit(client)
                    TestStatus.PING -> handlePing(client)
                    TestStatus.DOWN -> handleDown(client)
                    TestStatus.INIT_UP -> handleInitUp()
                    TestStatus.UP -> handleUp(client)
                    TestStatus.SPEEDTEST_END -> handleSpeedTestEnd(client)
                    TestStatus.QOS_TEST_RUNNING -> handleQoSRunning(client)
                    TestStatus.QOS_END -> handleQoSEnd(client)
                    TestStatus.ERROR -> handleError(client)
                    TestStatus.END -> handleEnd(client)
                    TestStatus.ABORTED -> handleAbort(client)
                }

                if (currentStatus.isFinalState()) {
                    client.commonCallback = null
                    client.shutdown()
                    if (currentStatus != TestStatus.ERROR) {
                        _listener?.onFinish()
                    }
                    stop()
                } else {
                    delay(100)
                }
            }
        }
    }

    private fun handleWait() {
        setState(MeasurementState.IDLE, 0)
    }

    private fun handleInit(client: RMBTClient) {
        client.getIntermediateResult(result)
        setState(MeasurementState.INIT, (result.progress * 100).toInt())
    }

    private fun handlePing(client: RMBTClient) {
        client.getIntermediateResult(result)
        setState(MeasurementState.PING, (result.progress * 100).toInt())
    }

    private fun handleDown(client: RMBTClient) {
        client.getIntermediateResult(result)
        val progress = (result.progress * 100).toInt()
        if (progress != previousDownloadProgress) {
            setState(MeasurementState.DOWNLOAD, progress)
            val ping = TimeUnit.NANOSECONDS.toMillis(result.pingNano)
            if (ping >= 0) {
                _listener?.onPingChanged(ping)
            }
            _listener?.onDownloadSpeedChanged(progress, result.downBitPerSec)
            previousDownloadProgress = progress
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
            _listener?.onUploadSpeedChanged(progress, result.upBitPerSec)
            previousUploadProgress = progress
        }
    }

    private fun handleSpeedTestEnd(client: RMBTClient) {
        setState(MeasurementState.FINISH, 0)
    }

    private fun handleQoSRunning(client: RMBTClient) {
        Timber.e("${TestStatus.QOS_TEST_RUNNING} handling not implemented")
    }

    private fun handleQoSEnd(client: RMBTClient) {
        Timber.e("${TestStatus.QOS_END} handling not implemented")
    }

    private fun handleError(client: RMBTClient) {
        _listener?.onError()
    }

    private fun handleAbort(client: RMBTClient) {
        Timber.e("${TestStatus.ABORTED} handling not implemented")
    }

    private fun handleEnd(client: RMBTClient) {
        setState(MeasurementState.FINISH, 0)
    }

    private fun setState(state: MeasurementState, progress: Int) {
        _listener?.onProgressChanged(state, progress)
    }

    override fun stop() {
        if (job == null) {
            Timber.w("Runner is already stopped")
        } else {
            job?.cancel()
        }
        job = null
        _listener = null
    }

    private fun TestStatus.isFinalState() = this == TestStatus.ABORTED ||
            this == TestStatus.END ||
            this == TestStatus.ERROR ||
            this == TestStatus.SPEEDTEST_END
}