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

class WrappedTestController(private val config: Config, private val clientUUID: ClientUUID) : TestController {

    private var job: Job? = null

    private val result: IntermediateResult by lazy { IntermediateResult() }

    private var _listener: TestProgressListener? = null

    override val isRunning: Boolean
        get() = job != null

    override fun start(listener: TestProgressListener, deviceInfo: DeviceInfo) {
        Timber.d("Start---")
        if (job != null) {
            Timber.w("Runner is already started")
            return
        }

        _listener = listener

        job = GlobalScope.async {

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

            if (errorSet.isNotEmpty()) {
                Timber.e("ERRORS CLIENT")
                return@async
            }

            client?.trafficService = TrafficServiceImpl()
            val connection = client?.controlConnection
            Timber.i("Client UUID: ${connection?.clientUUID}")
            Timber.i("Server Name: ${connection?.serverName}")
            Timber.i("Loop Id: ${connection?.loopUuid}")

            GlobalScope.async {
                client?.runTest()
            }

            var currentStatus = TestStatus.WAIT
            while (!currentStatus.isFinalState()) {
                currentStatus = client.status
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
                    client?.shutdown()
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
        setState(MeasurementState.DOWNLOAD, (result.progress * 100).toInt())
        val ping = TimeUnit.NANOSECONDS.toMillis(result.pingNano)
        if (ping >= 0) {
            _listener?.onPingChanged(ping)
        }
        _listener?.onDownloadSpeedChanged(result.downBitPerSec)
    }

    private fun handleInitUp() {
        setState(MeasurementState.UPLOAD, 0)
    }

    private fun handleUp(client: RMBTClient) {
        client.getIntermediateResult(result)
        setState(MeasurementState.UPLOAD, (result.progress * 100).toInt())
        _listener?.onUploadSpeedChanged(result.upBitPerSec)
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
        Timber.e("${TestStatus.ERROR} handling not implemented")
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