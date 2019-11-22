package at.specure.measurement

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.Observer
import at.specure.config.Config
import at.specure.di.CoreInjector
import at.specure.di.NotificationProvider
import at.specure.info.network.ActiveNetworkLiveData
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthLiveData
import at.specure.location.LocationInfo
import at.specure.location.LocationInfoLiveData
import at.specure.repository.TestDataRepository
import at.specure.test.DeviceInfo
import at.specure.test.TestController
import at.specure.test.TestProgressListener
import timber.log.Timber
import java.util.concurrent.TimeUnit
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

    @Inject
    lateinit var notificationProvider: NotificationProvider

    @Inject
    lateinit var testDataRepository: TestDataRepository

    private val producer: Producer by lazy { Producer() }
    private val clientAggregator: ClientAggregator by lazy { ClientAggregator() }

    private var measurementState: MeasurementState = MeasurementState.IDLE
    private var measurementProgress = 0
    private var pingMs = 0L
    private var downloadSpeedBps = 0L
    private var uploadSpeedBps = 0L
    private var hasErrors = false

    private var signalStrengthInfo: SignalStrengthInfo? = null
    private var networkInfo: NetworkInfo? = null
    private var locationInfo: LocationInfo? = null

    private val notificationManager: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var wifiLock: WifiManager.WifiLock

    private val testListener = object : TestProgressListener {

        override fun onProgressChanged(state: MeasurementState, progress: Int) {
            measurementState = state
            measurementProgress = progress
            clientAggregator.onProgressChanged(state, progress)

            notificationManager.notify(NOTIFICATION_ID, notificationProvider.measurementServiceNotification(progress, state, config.skipQoSTests))
        }

        override fun onPingChanged(pingMs: Long) {
            this@MeasurementService.pingMs = pingMs
            clientAggregator.onPingChanged(pingMs)
        }

        override fun onDownloadSpeedChanged(progress: Int, speedBps: Long) {
            downloadSpeedBps = speedBps
            clientAggregator.onDownloadSpeedChanged(progress, speedBps)
        }

        override fun onUploadSpeedChanged(progress: Int, speedBps: Long) {
            uploadSpeedBps = speedBps
            clientAggregator.onUploadSpeedChanged(progress, speedBps)
        }

        override fun onFinish() {
            stopForeground(true)
            clientAggregator.onMeasurementFinish()
            unlock()
        }

        override fun onError() {
            hasErrors = true
            stopForeground(true)
            clientAggregator.onMeasurementError()
            unlock()
        }

        override fun onClientReady(testUUID: String) {
            locationInfo?.let {
                testDataRepository.saveGeoLocation(testUUID, it)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        CoreInjector.inject(this)

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "$packageName:RMBTWifiLock")
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:RMBTWakeLock")

        signalStrengthLiveData.observe(this, Observer {
            signalStrengthInfo = it
            clientAggregator.onSignalChanged(it)
        })

        activeNetworkLiveData.observe(this, Observer {
            networkInfo = it
            clientAggregator.onActiveNetworkChanged(it)
        })

        locationInfoLiveData.observe(this, Observer { info ->
            locationInfo = info
            if (runner.isRunning) {
                runner.testUUID?.let { UUID ->
                    testDataRepository.saveGeoLocation(UUID, info)
                }
            }
        })
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        attachToForeground()
        @Suppress("UNNECESSARY_SAFE_CALL") // intent may be null after service restarted by the system
        if (intent?.action == ACTION_START_TESTS) {
            startTests()
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
                age = it.ageNanos,
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

        hasErrors = false
        runner.start(testListener, deviceInfo)

        attachToForeground()
        lock()
    }

    private fun attachToForeground() {
        startForeground(NOTIFICATION_ID, notificationProvider.measurementServiceNotification(0, MeasurementState.INIT, true))
    }

    private fun stopTests() {
        Timber.d("Stop tests")
        runner.stop()

        stopForeground(true)
        unlock()
    }

    private fun resetStates() {
        testListener.onProgressChanged(MeasurementState.IDLE, 0)
        testListener.onPingChanged(0)
        testListener.onDownloadSpeedChanged(0, 0)
        testListener.onUploadSpeedChanged(0, 0)
    }

    private fun lock() {
        try {
            if (!wakeLock.isHeld) {
                wakeLock.acquire(TimeUnit.MINUTES.toMillis(10))
            }
            if (!wifiLock.isHeld) {
                wifiLock.acquire()
            }
            Timber.d("Wake locked")
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private fun unlock() {
        try {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            if (wifiLock.isHeld) {
                wifiLock.release()
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }
    }

    private inner class Producer : Binder(), MeasurementProducer {

        override fun addClient(client: MeasurementClient) {
            with(client) {
                clientAggregator.addClient(this)
                onProgressChanged(measurementState, measurementProgress)
                onPingChanged(pingMs)
                onDownloadSpeedChanged(measurementProgress, downloadSpeedBps)
                onUploadSpeedChanged(measurementProgress, uploadSpeedBps)
                onSignalChanged(signalStrengthInfo)
                onActiveNetworkChanged(networkInfo)
                if (hasErrors) {
                    client.onMeasurementError()
                }
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

        override val isTestsRunning: Boolean
            get() = runner.isRunning

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

        override fun onMeasurementError() {
            clients.forEach {
                it.onMeasurementError()
            }
        }

        override fun onSignalChanged(signalStrengthInfo: SignalStrengthInfo?) {
            clients.forEach {
                it.onSignalChanged(signalStrengthInfo)
            }
        }

        override fun onDownloadSpeedChanged(progress: Int, speedBps: Long) {
            clients.forEach {
                it.onDownloadSpeedChanged(progress, speedBps)
            }
        }

        override fun onUploadSpeedChanged(progress: Int, speedBps: Long) {
            clients.forEach {
                it.onUploadSpeedChanged(progress, speedBps)
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

        private const val NOTIFICATION_ID = 1

        private const val ACTION_START_TESTS = "KEY_START_TESTS"

        fun startTests(context: Context) {
            val intent = intent(context)
            intent.action = ACTION_START_TESTS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun intent(context: Context) = Intent(context, MeasurementService::class.java)
    }
}