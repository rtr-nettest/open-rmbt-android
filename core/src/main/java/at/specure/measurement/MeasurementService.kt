package at.specure.measurement

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import at.rmbt.util.exception.HandledException
import at.specure.config.Config
import at.specure.di.CoreInjector
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.location.LocationInfo
import at.specure.location.LocationInfoLiveData
import at.specure.test.DeviceInfo
import at.specure.test.TestController
import at.specure.test.TestProgressListener
import timber.log.Timber
import javax.inject.Inject

class MeasurementService : LifecycleService() {

    @Inject
    lateinit var config: Config

    @Inject
    lateinit var runner: TestController

    @Inject
    lateinit var signalStrengthLiveData: SignalStrengthLiveData

    @Inject
    lateinit var activeNetworkLiveData: ActiveNetworkLiveData

    @Inject
    lateinit var locationInfoLiveData: LocationInfoLiveData

    private val producer: Producer by lazy { Producer() }
    private val clientAggregator: ClientAggregator by lazy { ClientAggregator() }

    private var measurementState: MeasurementState = MeasurementState.IDLE
    private var measurementProgress = 0
    private var pingMs = 0L
    private var downloadSpeedBps = 0L
    private var uploadSpeedBps = 0L

    private var signalStrengthInfo: SignalStrengthInfo? = null
    private var networkInfo: NetworkInfo? = null
    private var locationInfo: LocationInfo? = null

    private val testListener = object : TestProgressListener {

        override fun onProgressChanged(state: MeasurementState, progress: Int) {
            measurementState = state
            measurementProgress = progress
            clientAggregator.onProgressChanged(state, progress)
        }

        override fun onPingChanged(pingMs: Long) {
            this@MeasurementService.pingMs = pingMs
            clientAggregator.onPingChanged(pingMs)
        }

        override fun onDownloadSpeedChanged(speedBps: Long) {
            downloadSpeedBps = speedBps
            clientAggregator.onDownloadSpeedChanged(speedBps)
        }

        override fun onUploadSpeedChanged(speedBps: Long) {
            uploadSpeedBps = speedBps
            clientAggregator.onUploadSpeedChanged(speedBps)
        }
    }

    override fun onCreate() {
        super.onCreate()
        CoreInjector.inject(this)

        signalStrengthLiveData.observe(this, Observer {
            signalStrengthInfo = it
            clientAggregator.onSignalChanged(it)
        })

        activeNetworkLiveData.observe(this, Observer {
            networkInfo = it
            clientAggregator.onActiveNetworkChanged(it)
        })

        locationInfoLiveData.observe(this, Observer {
            locationInfo = it
        })
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        @Suppress("UNNECESSARY_SAFE_CALL") // intent may be null after service restarted by the system
        when (intent?.action) {
            ACTION_START_TESTS -> startTests()
            ACTION_STOP_TESTS -> stopTests()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Timber.i("onBind")
        return producer
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.i("onUnbind")
        return super.onUnbind(intent)
    }

    private fun startTests() {
        Timber.d("Start tests")
        if (!runner.isRunning) {
            resetStates()
        }

        var location: DeviceInfo.Location? = null
        locationInfo?.let {
            location = DeviceInfo.Location(
                lat = it.latitude,
                long = it.longitude,
                provider = it.provider.name,
                speed = it.speed,
                bearing = it.bearing,
                time = it.elapsedRealtimeNanos,
                age = it.age,
                accuracy = it.accuracy,
                mock_location = it.locationIsMocked,
                altitude = it.altitude
            )
        }

        val deviceInfo = DeviceInfo(
            context = this,
            ndt = config.NDTEnabled,
            testCounter = config.testCounter,
            location = location
        )

        runner.start(testListener, deviceInfo)

        startForeground(1, notification)
    }

    private fun stopTests() {
        Timber.d("Stop tests")
        runner.stop()

        stopForeground(true)
    }

    private fun resetStates() {
        testListener.onProgressChanged(MeasurementState.IDLE, 0)
        testListener.onPingChanged(0)
        testListener.onDownloadSpeedChanged(0)
        testListener.onUploadSpeedChanged(0)
    }

    private val notification: Notification
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationChannel = NotificationChannel("123", "Dummy channel", NotificationManager.IMPORTANCE_LOW)
                notificationChannel.description = "Description"
                notificationManager.createNotificationChannel(notificationChannel)
            }
            return NotificationCompat.Builder(this, "123")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentText("Foreground")
                .setContentTitle("Measurement Service")
                .build()
        }

    private inner class Producer : Binder(), MeasurementProducer {

        override fun addClient(client: MeasurementClient) {
            with(client) {
                clientAggregator.addClient(this)
                onProgressChanged(measurementState, measurementProgress)
                onPingChanged(pingMs)
                onDownloadSpeedChanged(downloadSpeedBps)
                onUploadSpeedChanged(uploadSpeedBps)
                onSignalChanged(signalStrengthInfo)
                onActiveNetworkChanged(networkInfo)
            }
        }

        override fun removeClient(client: MeasurementClient) {
            clientAggregator.removeClient(client)
        }

        override val measurementState: MeasurementState
            get() = this@MeasurementService.measurementState

        override val measurementProgress: Int
            get() = this@MeasurementService.measurementProgress

        override val downloadSpeedBps: Long
            get() = this@MeasurementService.downloadSpeedBps

        override val uploadSpeedBps: Long
            get() = this@MeasurementService.uploadSpeedBps

        override val pingMs: Long
            get() = this@MeasurementService.pingMs

        override val signalStrengthInfo: SignalStrengthInfo?
            get() = this@MeasurementService.signalStrengthInfo

        override val networkInfo: NetworkInfo?
            get() = this@MeasurementService.networkInfo

        override fun startTests() {
            this@MeasurementService.startTests()
        }

        override fun stopTests() {
            this@MeasurementService.stopTests()
        }
    }

    private inner class ClientAggregator : MeasurementClient {

        private val clients = mutableSetOf<MeasurementClient>()

        fun addClient(client: MeasurementClient) {
            clients.add(client)
        }

        fun removeClient(client: MeasurementClient) {
            clients.add(client)
        }

        override fun onProgressChanged(state: MeasurementState, progress: Int) {
            clients.forEach {
                it.onProgressChanged(state, progress)
            }
        }

        override fun onMeasurementFinish() {
            clients.forEach {
                it.onMeasurementFinish()
            }
        }

        override fun onMeasurementError(error: HandledException) {
            clients.forEach {
                it.onMeasurementError(error)
            }
        }

        override fun onSignalChanged(signalStrengthInfo: SignalStrengthInfo?) {
            clients.forEach {
                it.onSignalChanged(signalStrengthInfo)
            }
        }

        override fun onDownloadSpeedChanged(speedBps: Long) {
            clients.forEach {
                it.onDownloadSpeedChanged(speedBps)
            }
        }

        override fun onUploadSpeedChanged(speedBps: Long) {
            clients.forEach {
                it.onUploadSpeedChanged(speedBps)
            }
        }

        override fun onPingChanged(pingMs: Long) {
            clients.forEach {
                it.onPingChanged(pingMs)
            }
        }

        override fun onActiveNetworkChanged(networkInfo: NetworkInfo?) {
            clients.forEach {
                it.onActiveNetworkChanged(networkInfo)
            }
        }
    }

    companion object {

        private const val ACTION_START_TESTS = "KEY_START_TESTS"
        private const val ACTION_STOP_TESTS = "KEY_STOP_TESTS"

        fun startTests(context: Context) {
            val intent = intent(context)
            intent.action = "!@3"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTests(context: Context) {
            val intent = intent(context)
            intent.action = ACTION_STOP_TESTS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun intent(context: Context) = Intent(context, MeasurementService::class.java)
    }
}