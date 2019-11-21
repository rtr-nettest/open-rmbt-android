package at.specure.test

import at.specure.measurement.MeasurementState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.random.Random

class MockedTestController : TestController {

    private var job: Job? = null
    private var state: MeasurementState = MeasurementState.IDLE
    private var progress = 0
    private var pingMs = 0L
    private var downloadSpeedBps = 0L
    private var uploadSpeedBps = 0L

    private val randomDelay: Long
        get() = Random.nextLong(10, 150)

    private var _listener: TestProgressListener? = null

    override val isRunning: Boolean
        get() = job != null

    override fun start(listener: TestProgressListener, deviceInfo: DeviceInfo) {
        if (job != null) {
            Timber.w("Runner is already started")
            return
        }

        _listener = listener

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
        _listener?.onPingChanged(pingMs)
    }

    private fun setDownloadSpeed(speedBps: Long) {
        downloadSpeedBps = speedBps
        _listener?.onDownloadSpeedChanged(speedBps)
    }

    private fun setUploadSpeed(speedBps: Long) {
        uploadSpeedBps = speedBps
        _listener?.onUploadSpeedChanged(speedBps)
    }

    private fun setState(state: MeasurementState, progress: Int) {
        this.state = state
        this.progress = progress
        _listener?.onProgressChanged(state, progress)
    }

    private suspend fun wait() = delay(randomDelay)

    override fun stop() {
        if (job == null) {
            Timber.w("Runner is already stopped")
        }
        job?.cancel()
        job = null

        _listener = null
    }
}