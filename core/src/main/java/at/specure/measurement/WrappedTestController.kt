package at.specure.measurement

import at.rtr.rmbt.client.RMBTClient
import at.rtr.rmbt.client.TrafficServiceImpl
import at.rtr.rmbt.client.helper.IntermediateResult
import at.rtr.rmbt.client.helper.TestStatus
import at.rtr.rmbt.util.model.shared.exception.ErrorStatus
import at.specure.data.ClientUUID
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthWatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WrappedTestController(
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val clientUUID: ClientUUID
) : TestController, SignalStrengthWatcher.SignalStrengthListener,
    ActiveNetworkWatcher.NetworkChangeListener {

    private var job: Job? = null
    private var state: MeasurementState = MeasurementState.IDLE
    private var progress = 0
    private var pingMs = 0L
    private var downloadSpeedBps = 0L
    private var uploadSpeedBps = 0L
    private var signalStrength: SignalStrengthInfo? = null
    private var networkInfo: NetworkInfo? = null

    private val result: IntermediateResult by lazy { IntermediateResult() }

    override var stateListener: ((state: MeasurementState, progress: Int) -> Unit)? = null
    override var pingListener: ((pingMs: Long) -> Unit)? = null
    override var downloadSpeedListener: ((speedBps: Long) -> Unit)? = null
    override var uploadSpeedListener: ((speedBps: Long) -> Unit)? = null
    override var signalStrengthListener: ((signalStrengthInfo: SignalStrengthInfo?) -> Unit)? = null
    override var networkInfoListener: ((networkInfo: NetworkInfo?) -> Unit)? = null

    override fun start() {
        Timber.d("Start---")
        if (job != null) {
            Timber.w("Runner is already started")
            return
        }

        signalStrengthWatcher.addListener(this)
        activeNetworkWatcher.addListener(this)

        job = GlobalScope.async {

            setState(MeasurementState.IDLE, 0)
            resetState()

            val uuid = clientUUID.value!!
            val controlServer = "dev.netztest.at"
            val port = 443
            val ssl = true

//            val geoInfo = listOf("1572961354000", "37.421998333333335", "-122.08400000000002", "20.0", "0.0", "0.0", "0.0", "gps")
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
        if (ping >= 0 && ping != pingMs) {
            pingMs = ping
            pingListener?.invoke(ping)
        }
        downloadSpeedBps = result.downBitPerSec
        downloadSpeedListener?.invoke(downloadSpeedBps)
    }

    private fun handleInitUp() {
        setState(MeasurementState.UPLOAD, 0)
    }

    private fun handleUp(client: RMBTClient) {
        client.getIntermediateResult(result)
        setState(MeasurementState.UPLOAD, (result.progress * 100).toInt())
        uploadSpeedBps = result.upBitPerSec
        uploadSpeedListener?.invoke(uploadSpeedBps)
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
        this.state = state
        this.progress = progress
        stateListener?.invoke(state, progress)
    }

    override fun stop() {
        if (job == null) {
            Timber.w("Runner is already stopped")
        } else {
            signalStrengthWatcher.removeListener(this)
            activeNetworkWatcher.removeListener(this)

            job?.cancel()
        }
        job = null
    }

    private fun resetState() {
        pingMs = 0
        pingListener?.invoke(0)
        downloadSpeedBps = 0
        downloadSpeedListener?.invoke(downloadSpeedBps)
        uploadSpeedBps = 0
        uploadSpeedListener?.invoke(uploadSpeedBps)
    }

    private fun TestStatus.isFinalState() = this == TestStatus.ABORTED ||
            this == TestStatus.END ||
            this == TestStatus.ERROR ||
            this == TestStatus.SPEEDTEST_END

    override fun onSignalStrengthChanged(signalInfo: SignalStrengthInfo?) {
        signalStrength = signalInfo
        signalStrengthListener?.invoke(signalInfo)
    }

    override fun onActiveNetworkChanged(info: NetworkInfo?) {
        networkInfo = info
        networkInfoListener?.invoke(info)
    }
}