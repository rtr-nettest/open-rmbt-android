package at.specure.measurement

import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthWatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.random.Random

class MockedTestController(
    private val signalStrengthWatcher: SignalStrengthWatcher,
    private val activeNetworkWatcher: ActiveNetworkWatcher
) : TestController, SignalStrengthWatcher.SignalStrengthListener, ActiveNetworkWatcher.NetworkChangeListener {

    private var job: Job? = null
    private var state: MeasurementState = MeasurementState.IDLE
    private var progress = 0
    private var pingMs = 0L
    private var downloadSpeedBps = 0L
    private var uploadSpeedBps = 0L
    private var signalStrength: SignalStrengthInfo? = null
    private var networkInfo: NetworkInfo? = null

    private val randomDelay: Long
        get() = Random.nextLong(10, 150)

    override var stateListener: ((state: MeasurementState, progress: Int) -> (Unit))? = null
    override var pingListener: ((pingMs: Long) -> (Unit))? = null
    override var downloadSpeedListener: ((speedBps: Long) -> Unit)? = null
    override var uploadSpeedListener: ((speedBps: Long) -> Unit)? = null
    override var signalStrengthListener: ((signalStrengthInfo: SignalStrengthInfo?) -> (Unit))? = null
    override var networkInfoListener: ((networkInfo: NetworkInfo?) -> (Unit))? = null

    override fun start() {
        if (job != null) {
            Timber.w("Runner is already started")
            return
        }

        Timber.w(Thread.currentThread().name)

        signalStrengthWatcher.addListener(this)
        activeNetworkWatcher.addListener(this)

        job = GlobalScope.async {
            setState(MeasurementState.IDLE, 0) // Some dummy state
            setDownloadSpeed(0)
            setUploadSpeed(0)
            setPing(0)
            wait()

            Timber.w(Thread.currentThread().name)

            setState(MeasurementState.INIT, 0)
            repeat(100) {
                wait()
                setState(MeasurementState.INIT, it)
            }

            setState(MeasurementState.PING, 0)
            repeat(100) {
                wait()
                setState(MeasurementState.PING, it)
            }
            setPing(Random.nextLong(10, 500))

            setState(MeasurementState.DOWNLOAD, 0)
            repeat(100) {
                wait()
                setDownloadSpeed(Random.nextLong(25160000, 25170000))
                setState(MeasurementState.DOWNLOAD, it)
            }

            setState(MeasurementState.UPLOAD, 0)
            repeat(100) {
                wait()
                setUploadSpeed(Random.nextLong(9430000, 9440000))
                setState(MeasurementState.UPLOAD, it)
            }

            setState(MeasurementState.QOS, 0)
            repeat(100) {
                wait()
                setState(MeasurementState.QOS, it)
            }

            wait()
            setState(MeasurementState.FINISH, 0)

            stop()
        }
    }

    private fun setPing(pingMs: Long) {
        this.pingMs = pingMs
        pingListener?.invoke(pingMs)
    }

    private fun setDownloadSpeed(speedBps: Long) {
        downloadSpeedBps = speedBps
        downloadSpeedListener?.invoke(speedBps)
    }

    private fun setUploadSpeed(speedBps: Long) {
        uploadSpeedBps = speedBps
        uploadSpeedListener?.invoke(speedBps)
    }

    private fun setState(state: MeasurementState, progress: Int) {
        this.state = state
        this.progress = progress
        stateListener?.invoke(state, progress)
    }

    private suspend fun wait() = delay(randomDelay)

    override fun onSignalStrengthChanged(signalInfo: SignalStrengthInfo?) {
        signalStrength = signalInfo
        signalStrengthListener?.invoke(signalInfo)
    }

    override fun onActiveNetworkChanged(info: NetworkInfo?) {
        networkInfo = info
        networkInfoListener?.invoke(info)
    }

    override fun stop() {
        if (job == null) {
            Timber.w("Runner is already stopped")
        }
        job?.cancel()
        job = null
        signalStrengthWatcher.removeListener(this)
        activeNetworkWatcher.removeListener(this)
    }
}