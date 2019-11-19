package at.specure.test

import at.rtr.rmbt.client.RMBTClient
import at.rtr.rmbt.client.TrafficServiceImpl
import at.rtr.rmbt.client.helper.IntermediateResult
import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.util.model.shared.exception.ErrorStatus
import at.specure.data.ClientUUID
import at.specure.measurement.MeasurementState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WrappedTestController(private val clientUUID: ClientUUID) : TestController {

    private var job: Job? = null

    private val result: IntermediateResult by lazy { IntermediateResult() }

    private var _listener: TestProgressListener? = null

    override fun start(listener: TestProgressListener) {
        Timber.d("Start---")
        if (job != null) {
            Timber.w("Runner is already started")
            return
        }

        _listener = listener

        job = GlobalScope.async {

            setState(MeasurementState.IDLE, 0)

            val uuid = clientUUID.value!!
            val controlServer = "dev.netztest.at"
            val port = 443
            val ssl = true

            val geoInfo = ArrayList<String>().apply {
                add("1572961354000")
                add("37.421998333333335")
                add("-122.08400000000002")
                add("20.0")
                add("0.0")
                add("0.0")
                add("0.0")
                add("gps")
            }

            val type = "MOBILE"
            val name = "RMBT"
            val version = "3.6.6"

            val info =
                JSONObject("{\"plattform\":\"Android\",\"os_version\":\"8.0.0(5598391)\",\"api_level\":\"26\",\"device\":\"generic_x86\",\"model\":\"Android SDK built for x86\",\"product\":\"sdk_gphone_x86\",\"language\":\"en\",\"timezone\":\"Europe\\/Kiev\",\"softwareRevision\":\"master_2358f8f-dirty\",\"softwareVersionCode\":30606,\"softwareVersionName\":\"3.6.6\",\"type\":\"MOBILE\",\"location\":{\"lat\":37.421998333333335,\"long\":-122.08400000000002,\"provider\":\"gps\",\"speed\":0,\"bearing\":0,\"time\":1572961510000,\"age\":716,\"accuracy\":20,\"mock_location\":false},\"ndt\":true,\"testCounter\":10,\"android_permission_status\":[{\"permission\":\"android.permission.ACCESS_FINE_LOCATION\",\"status\":true},{\"permission\":\"android.permission.ACCESS_COARSE_LOCATION\",\"status\":true},{\"permission\":\"android.permission.ACCESS_BACKGROUND_LOCATION\",\"status\":false}]}")

            val errorSet = mutableSetOf<ErrorStatus>()

            val client = RMBTClient.getInstance(controlServer, null, port, ssl, geoInfo, uuid, type, name, version, null, info, errorSet)

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