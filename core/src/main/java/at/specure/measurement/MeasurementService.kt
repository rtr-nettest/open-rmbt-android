package at.specure.measurement

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import at.rmbt.client.control.data.TestFinishReason
import at.rmbt.util.exception.HandledException
import at.rmbt.util.exception.NoConnectionException
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum
import at.rtr.rmbt.util.IllegalNetworkChangeException
import at.specure.config.Config
import at.specure.data.entity.LoopModeState
import at.specure.data.repository.ResultsRepository
import at.specure.data.repository.TestDataRepository
import at.specure.di.CoreInjector
import at.specure.di.NotificationProvider
import at.specure.info.strength.SignalStrengthInfo
import at.specure.location.LocationState
import at.specure.location.LocationWatcher
import at.specure.measurement.signal.SignalMeasurementProducer
import at.specure.measurement.signal.SignalMeasurementService
import at.specure.test.DeviceInfo
import at.specure.test.StateRecorder
import at.specure.test.TestController
import at.specure.test.TestProgressListener
import at.specure.test.toDeviceInfoLocation
import at.specure.util.CustomLifecycleService
import at.specure.worker.WorkLauncher
import timber.log.Timber
import java.util.Timer
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.concurrent.timerTask

class MeasurementService : CustomLifecycleService() {

    private var measurementLastUpdate: Long = System.currentTimeMillis()
    private var lastNotifiedState: MeasurementState = MeasurementState.FINISH
    private var elapsedTime: Long = 0
    private var startTime: Long = 0
    private var inactivityTimer: Timer = Timer()

    private fun isRMBTClientResponding(): Boolean {
        val clientLastResponseDurationMillis = System.currentTimeMillis() - measurementLastUpdate
        Timber.d("RMBTClient inactivity for: ${TimeUnit.MILLISECONDS.toSeconds(clientLastResponseDurationMillis)} seconds")
        return (clientLastResponseDurationMillis < TimeUnit.SECONDS.toMillis(MEASUREMENT_INACTIVITY_THRESHOLD_SECONDS))
    }

    private fun planInactivityCheck() {

        inactivityTimer.cancel()
        inactivityTimer.purge()
        inactivityTimer = Timer()
        Timber.d("RMBTClient inactivity checking started")
        inactivityTimer.scheduleAtFixedRate(
            timerTask {
                val isNotResponding = !isRMBTClientResponding()
                if (isNotResponding) {
                    testListener.onError()
                    runner.stop()
                    inactivityTimer.cancel()
                    inactivityTimer.purge()
                    Timber.e("Test has been terminated, because of RMBTClient inactivity")
                }
            },
            TimeUnit.SECONDS.toMillis(MEASUREMENT_INACTIVITY_CHECKER_PERIOD_SECONDS),
            TimeUnit.SECONDS.toMillis(MEASUREMENT_INACTIVITY_CHECKER_PERIOD_SECONDS)
        )
    }

    private fun removeInactivityCheck() {
        Timber.d("RMBTClient inactivity checking stopped")
        inactivityTimer.cancel()
    }

    @Inject
    lateinit var config: Config

    @Inject
    lateinit var runner: TestController

    @Inject
    lateinit var stateRecorder: StateRecorder

    @Inject
    lateinit var notificationProvider: NotificationProvider

    @Inject
    lateinit var testDataRepository: TestDataRepository

    @Inject
    lateinit var resultRepository: ResultsRepository

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    @Inject
    lateinit var locationWatcher: LocationWatcher

    private val producer: Producer by lazy { Producer() }
    private val clientAggregator: ClientAggregator by lazy { ClientAggregator() }
    private var signalMeasurementProducer: SignalMeasurementProducer? = null
    private var signalMeasurementPauseRequired = false
    private var measurementState: MeasurementState = MeasurementState.IDLE
    private var loopModeState: LoopModeState = LoopModeState.IDLE

    private var measurementProgress = 0
    private var pingNanos = 0L
    private var downloadSpeedBps = 0L
    private var uploadSpeedBps = 0L
    private var hasErrors = false
    private var startNetwork: Network? = null
    private val lastMeasurementSignal: SignalStrengthInfo?
        get() = stateRecorder.lastMeasurementSignalStrength
    private var qosTasksPassed = 0

    private var qosTasksTotal = 0
    private var qosProgressMap: Map<QoSTestResultEnum, Int> = mapOf()
    private val notificationManager: NotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var wifiLock: WifiManager.WifiLock
    private var loopCountdownTimer: CountDownTimer? = null
    private var startPendingTest = false

    private val signalMeasurementConnection = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            signalMeasurementProducer = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            signalMeasurementProducer = service as SignalMeasurementProducer
            if (signalMeasurementPauseRequired) {
                signalMeasurementProducer?.pauseMeasurement(true)
                signalMeasurementPauseRequired = false
            }
        }
    }

    private val testListener = object : TestProgressListener {

        override fun onProgressChanged(state: MeasurementState, progress: Int) {
            measurementLastUpdate = System.currentTimeMillis()
            measurementState = state
            measurementProgress = progress
            if (config.loopModeEnabled) {
                loopModeState = LoopModeState.RUNNING
            }
            clientAggregator.onProgressChanged(state, progress)

            if ((state != MeasurementState.QOS) && (state != MeasurementState.ERROR) && (state != MeasurementState.IDLE) && (state != MeasurementState.FINISH)) {
                Timber.d("MeasurementViewModel: Progress changed notification state: $state")
                notifyDelayed(
                    notificationProvider.measurementServiceNotification(
                        progress,
                        state,
                        !config.shouldRunQosTest,
                        stateRecorder.loopModeRecord,
                        config.loopModeNumberOfTests,
                        stopTestsIntent(this@MeasurementService)
                    ),
                    state,
                    false
                )
            }
        }

        /**
         * This methods solves problem with changing notification too often (then it is problem to click on the button in the notification)
         */
        fun notifyDelayed(notification: Notification, state: MeasurementState, forceUpdate: Boolean) {

            if (elapsedTime > NOTIFICATION_UPDATE_INTERVAL_MS || lastNotifiedState != state || forceUpdate) {
                Handler(Looper.getMainLooper()).post {
                    notificationManager.notify(
                        NOTIFICATION_ID,
                        notification
                    )
                    lastNotifiedState = state
                    startTime = System.currentTimeMillis()
                    elapsedTime = 0
                }
            } else elapsedTime = System.currentTimeMillis() - startTime
        }

        override fun onPingChanged(pingNanos: Long) {
            this@MeasurementService.pingNanos = pingNanos
            clientAggregator.onPingChanged(pingNanos)
        }

        override fun onDownloadSpeedChanged(progress: Int, speedBps: Long) {
            downloadSpeedBps = speedBps
            stateRecorder.onDownloadSpeedChanged(progress, speedBps)
            clientAggregator.onDownloadSpeedChanged(progress, speedBps)
        }

        override fun onUploadSpeedChanged(progress: Int, speedBps: Long) {
            uploadSpeedBps = speedBps
            stateRecorder.onUploadSpeedChanged(progress, speedBps)
            clientAggregator.onUploadSpeedChanged(progress, speedBps)
        }

        override fun onFinish() {
            removeInactivityCheck()
            notificationManager.cancel(NOTIFICATION_ID)

            stateRecorder.onLoopTestFinished()

            if (!config.loopModeEnabled || (config.loopModeEnabled && (stateRecorder.loopTestCount >= config.loopModeNumberOfTests))) {
                loopCountdownTimer?.cancel()
                Timber.d("TIMER: cancelling 3: ${loopCountdownTimer?.hashCode()}")

                if (config.loopModeEnabled) {
                    notificationManager.notify(NOTIFICATION_LOOP_FINISHED_ID, notificationProvider.loopModeFinishedNotification())
                    loopModeState = LoopModeState.FINISHED
                }
                stopForeground(true)
                stateRecorder.finish()
                unlock()
                resumeSignalMeasurement(false)
            } else {
                resumeSignalMeasurement(true)
            }
        }

        override fun onPostFinish() {
            if (startPendingTest) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (config.loopModeEnabled && (stateRecorder.loopTestCount < config.loopModeNumberOfTests || (config.loopModeNumberOfTests == 0 && config.developerModeIsEnabled))) {
                        runner.reset()
                        Timber.d("LOOP STARTING PENDING TEST from onFinish")
                        runTest()
                    }
                }, 500)
            }
        }

        override fun onError() {
            removeInactivityCheck()
            if (config.loopModeEnabled && (stateRecorder.loopTestCount < config.loopModeNumberOfTests || (config.loopModeNumberOfTests == 0 && config.developerModeIsEnabled))) {
                resumeSignalMeasurement(true)
            } else {
                resumeSignalMeasurement(false)
            }
            if (startNetwork != connectivityManager.activeNetwork) {
                Timber.e("Network change!")
                try {
                    throw IllegalNetworkChangeException("Illegal network change during the test")
                } catch (ex: Exception) {
                    stateRecorder.setErrorCause(Log.getStackTraceString(ex))
                }
            }

            stateRecorder.onUnsuccessTest(TestFinishReason.ERROR)

            if (config.loopModeEnabled) {
                if (stateRecorder.loopLocalUuid == null) {
                    loopCountdownTimer?.cancel()
                    Timber.d("TIMER: cancelling 4: ${loopCountdownTimer?.hashCode()}")
                    hasErrors = true
                    notificationManager.cancel(NOTIFICATION_ID)
                    clientAggregator.onMeasurementError()
                    stateRecorder.finish()
                    unlock()
                    stopForeground(true)
                }
            }

            measurementState = MeasurementState.ERROR
            onProgressChanged(measurementState, 0)
            if (config.loopModeEnabled && (stateRecorder.loopTestCount >= config.loopModeNumberOfTests || (config.loopModeNumberOfTests == 0 && config.developerModeIsEnabled))) {
                loopCountdownTimer?.cancel()
                Timber.d("TIMER: cancelling 8: ${loopCountdownTimer?.hashCode()}")
                hasErrors = true
                notificationManager.cancel(NOTIFICATION_ID)
                clientAggregator.onMeasurementError()
                stateRecorder.onLoopTestStatusChanged(LoopModeState.FINISHED)
                stateRecorder.finish()
                unlock()
                stopForeground(true)
            }

            stateRecorder.onLoopTestFinished()

            Timber.d("TEST ERROR HANDLING")

            if (startPendingTest && (config.loopModeEnabled && (stateRecorder.loopTestCount < config.loopModeNumberOfTests || (config.loopModeNumberOfTests == 0 && config.developerModeIsEnabled)))) {
                Timber.d("TEST ERROR HANDLING - PENDING")
                runner.reset()
                Timber.d("LOOP STARTING PENDING TEST from onError")
                runTest()
            } else {
                Timber.d("TEST ERROR HANDLING - NOT PENDING")
                if (!config.loopModeEnabled || (config.loopModeEnabled && (stateRecorder.loopTestCount >= config.loopModeNumberOfTests))) {
                    Timber.d("TIMER: cancelling 5: ${loopCountdownTimer?.hashCode()}")
                    loopCountdownTimer?.cancel()
                    Timber.d("TEST ERROR HANDLING - NOT PENDING LOOP DISABLED")
                    if (config.loopModeEnabled) {
                        loopModeState = LoopModeState.FINISHED
                        notificationManager.cancel(NOTIFICATION_ID)
                        notificationManager.notify(NOTIFICATION_LOOP_FINISHED_ID, notificationProvider.loopModeFinishedNotification())
                        clientAggregator.onMeasurementError()
                    } else {
                        notificationManager.cancel(NOTIFICATION_ID)
                        clientAggregator.onMeasurementError()
                    }
                    hasErrors = true
                    stateRecorder.finish()
                    unlock()
                    stopForeground(true)
                }
            }
        }

        override fun onClientReady(testUUID: String, loopUUID: String?, loopLocalUUID: String?, testStartTimeNanos: Long) {
            planInactivityCheck()
            clientAggregator.onClientReady(testUUID, loopLocalUUID)
            startNetwork = connectivityManager.activeNetwork
            stateRecorder.onReadyToSubmit = { shouldShowResults ->
                resultRepository.sendTestResults(testUUID) {
                    it.onSuccess {
                        if (shouldShowResults) {
                            clientAggregator.onSubmitted()
                        }
                    }

                    it.onFailure { ex ->
                        if (shouldShowResults) {
                            clientAggregator.onSubmitted()
                        }
                        if (ex is NoConnectionException) {
                            Timber.d("Delayed submission work created")
                            WorkLauncher.enqueueDelayedDataSaveRequest(applicationContext, testUUID)
                        }
                    }
                }
            }
        }

        override fun onQoSTestProgressUpdate(tasksPassed: Int, tasksTotal: Int, progressMap: Map<QoSTestResultEnum, Int>) {
            clientAggregator.onQoSTestProgressUpdated(tasksPassed, tasksTotal, progressMap)
            qosTasksPassed = tasksPassed
            qosTasksTotal = tasksTotal
            qosProgressMap = progressMap

            val progress = (tasksPassed / tasksTotal.toFloat()) * 100
            Timber.d("MeasurementViewModel: QOS progress changed notification")
            notifyDelayed(
                notificationProvider.measurementServiceNotification(
                    progress.toInt(),
                    MeasurementState.QOS,
                    !config.shouldRunQosTest,
                    stateRecorder.loopModeRecord,
                    config.loopModeNumberOfTests,
                    stopTestsIntent(this@MeasurementService)
                ), MeasurementState.QOS, false
            )
        }
    }

    private fun resumeSignalMeasurement(unstoppable: Boolean) {
        signalMeasurementPauseRequired = false
        signalMeasurementProducer?.resumeMeasurement(unstoppable)
    }

    private fun pauseSignalMeasurement() {
        signalMeasurementPauseRequired = true // in case when service connection wasn't established before test started
        signalMeasurementProducer?.pauseMeasurement(true)
    }

    override fun onCreate() {
        super.onCreate()
        CoreInjector.inject(this)

        stateRecorder.bind(this)

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "$packageName:RMBTWifiLock")
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:RMBTWakeLock")

        stateRecorder.onLoopDistanceReached = {
            Timber.i("LOOP MODE DISTANCE REACHED")
            Timber.d("TIMER: cancelling 1: ${loopCountdownTimer?.hashCode()}")
            loopCountdownTimer?.cancel()

            if ((stateRecorder.loopTestCount < config.loopModeNumberOfTests) || config.loopModeNumberOfTests == 0 && config.developerModeIsEnabled) {
                if (runner.isRunning) {
                    startPendingTest = true
                    Timber.d("LOOP STARTING PENDING TEST set to true onCreate due to distance")
                } else {
                    runner.reset()
                    Timber.d("LOOP STARTING TEST onCreate due to distance")
                    runTest()
                }
            }
        }

        bindService(SignalMeasurementService.intent(this), signalMeasurementConnection, Context.BIND_AUTO_CREATE)
    }

    @Suppress("UNNECESSARY_SAFE_CALL")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("MeasurementViewModel: Service: onStartCommand ${intent?.action}")
        if (intent != null) {
            attachToForeground()
            when (intent?.action) {
                ACTION_START_TESTS -> startTests()
                ACTION_STOP_TESTS -> stopTests()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        Timber.i("onBind")
        return producer
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.i("onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        resumeSignalMeasurement(false)
        signalMeasurementConnection.onServiceDisconnected(null)
        unbindService(signalMeasurementConnection)
        super.onDestroy()
    }

    private fun startTests() {
        Timber.d("Start tests")
        stateRecorder.resetLoopMode()
        if (config.loopModeEnabled) {
            stateRecorder.initializeLoopModeData(null)
        }
        loopModeState = if (config.loopModeEnabled) {
            LoopModeState.RUNNING
        } else {
            LoopModeState.IDLE
        }
        measurementState = MeasurementState.INIT

        attachToForeground()
        lock()
        Timber.d("LOOP STARTING TEST from startTests")
        runTest()
    }

    private fun scheduleNextLoopTest() {
        stateRecorder.onLoopTestScheduled()
        try {
            Handler(Looper.getMainLooper()).post {
                Timber.d("TIMER: cancelling: ${loopCountdownTimer?.hashCode()}")
                loopCountdownTimer?.cancel()
                loopCountdownTimer = null

                loopCountdownTimer = object : CountDownTimer(loopDelayMs, 1000) {

                    override fun onFinish() {
                        Timber.i("CountDownTimer finished - ${this.hashCode()}")
                        if (runner.isRunning) {
                            Timber.d("LOOP STARTING PENDING TEST set to true")
                            startPendingTest = true
                        } else {
                            if (!config.loopModeEnabled || (config.loopModeEnabled && (stateRecorder.loopTestCount < config.loopModeNumberOfTests || (config.loopModeNumberOfTests == 0 && config.developerModeIsEnabled)))) {
                                runner.reset()
                                Timber.d("LOOP STARTING PENDING TEST on timer finished")
                                runTest()
                            }
                        }
                    }

                    override fun onTick(millisUntilFinished: Long) {
                        if (stateRecorder.loopModeRecord?.status == LoopModeState.FINISHED || stateRecorder.loopModeRecord?.status == LoopModeState.CANCELLED || stateRecorder.loopModeRecord?.testsPerformed ?: 0 >= config.loopModeNumberOfTests) {
                            this.cancel()
                        }
                        Timber.d("CountDownTimer tick $millisUntilFinished - ${this.hashCode()}")
                        if (!runner.isRunning) {
                            val locationAvailable =
                                locationWatcher.state == LocationState.ENABLED
                            val distancePassed = stateRecorder.loopModeRecord?.movementDistanceMeters ?: 0

                            Timber.d("Created measurement notification time remaining")
                            if (stateRecorder.loopModeRecord?.status == LoopModeState.IDLE) {
                                val notification = notificationProvider.loopCountDownNotification(
                                    millisUntilFinished,
                                    distancePassed,
                                    config.loopModeDistanceMeters,
                                    stateRecorder.loopTestCount,
                                    config.loopModeNumberOfTests,
                                    stopTestsIntent(this@MeasurementService),
                                    locationAvailable
                                )
                                notificationManager.notify(NOTIFICATION_ID, notification)
                                Timber.d("Created measurement notification time remaining IDLE state")
                            }
                            clientAggregator.onLoopCountDownTimer(loopDelayMs - millisUntilFinished, loopDelayMs)
                            clientAggregator.onLoopDistanceChanged(distancePassed, config.loopModeDistanceMeters, locationAvailable)
                        }
                    }
                }
                Timber.d("TIMER: CountDownTimer created - ${loopCountdownTimer.hashCode()}")
                loopCountdownTimer?.start()
                Timber.d("TIMER: starting: ${loopCountdownTimer?.hashCode()}")
            }

            Timber.d("CountDownTimer scheduled")
        } catch (ex: Exception) {
            Timber.e(ex, "CountDownTimer")
        }
    }

    private val loopDelayMs: Long
        get() {
            val timeAwait = TimeUnit.MINUTES.toMillis(config.loopModeWaitingTimeMin.toLong())
            if (timeAwait == 0L) {
                return 3000
            }
            return timeAwait
        }

    private fun runTest() {
        notificationManager.cancel(NOTIFICATION_LOOP_FINISHED_ID)
        startPendingTest = false
        if (config.loopModeEnabled && (stateRecorder.loopTestCount < config.loopModeNumberOfTests || (config.loopModeNumberOfTests == 0 && config.developerModeIsEnabled))) {
            scheduleNextLoopTest()
        }

        Timber.d("LOOP MODE: runner is running: ${runner.isRunning}")
        if (!runner.isRunning) {
            resetStates()
        }

        pauseSignalMeasurement()

        var location: DeviceInfo.Location? = null

        if (locationWatcher.state == LocationState.ENABLED) {
            stateRecorder.locationInfo?.let {
                location = it.toDeviceInfoLocation()
            }
        }

        stateRecorder.updateLocationInfo()
        if (config.loopModeEnabled) {
            loopModeState = LoopModeState.RUNNING
            stateRecorder.onTestInLoopStarted()
        }

        val deviceInfo = DeviceInfo(
            context = this,
            location = location
        )

        qosTasksPassed = 0
        qosTasksTotal = 0
        qosProgressMap = mapOf()

        hasErrors = false

        runner.start(
            deviceInfo,
            stateRecorder.loopModeRecord?.uuid,
            stateRecorder.loopLocalUuid,
            stateRecorder.loopTestCount,
            testListener,
            stateRecorder
        )
    }

    private fun attachToForeground() {
        Timber.d("MeasurementViewModel: Attached to foreground notification")
        startForeground(
            NOTIFICATION_ID,
            notificationProvider.measurementServiceNotification(
                0,
                MeasurementState.INIT,
                true,
                stateRecorder.loopModeRecord,
                config.loopModeNumberOfTests,
                stopTestsIntent(this@MeasurementService)
            )
        )

        if (!producer.isTestsRunning) {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    private fun stopTests() {
        Timber.d("Stop tests")
        // stop foreground does not hide notification about test running during loop mode sometimes
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(NOTIFICATION_LOOP_FINISHED_ID)
        loopModeState = LoopModeState.CANCELLED
        stateRecorder.onLoopTestStatusChanged(loopModeState)
        runner.stop()
        Timber.d("TIMER: cancelling 2: ${loopCountdownTimer?.hashCode()}")
        loopCountdownTimer?.cancel()
        config.previousTestStatus = TestFinishReason.ABORTED.name // cannot be handled in TestController
        stateRecorder.onUnsuccessTest(TestFinishReason.ABORTED)
        measurementState = MeasurementState.ABORTED
        stateRecorder.finish()
        clientAggregator.onMeasurementCancelled()
        clientAggregator.onProgressChanged(measurementState, 0)
        resumeSignalMeasurement(false)
        stopForeground(true)
        unlock()
    }

    private fun resetStates() {
        loopModeState = LoopModeState.IDLE
        testListener.onProgressChanged(MeasurementState.INIT, 0)
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
            Timber.i("Wake locked")
        } catch (ex: Exception) {
            Timber.e(ex, "Wake lock failed")
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
            Timber.v("Wake unlocked")
        } catch (ex: Exception) {
            Timber.e(ex, "Wake unlock failed")
        }
    }

    /**
     * Binder, creates a bound to listeners - in this case MeasurementViewModel
     */
    private inner class Producer : Binder(), MeasurementProducer {

        override fun addClient(client: MeasurementClient) {
            with(client) {
                clientAggregator.addClient(this)
                onProgressChanged(measurementState, measurementProgress)
                onPingChanged(pingNanos)
                onDownloadSpeedChanged(measurementProgress, downloadSpeedBps)
                onUploadSpeedChanged(measurementProgress, uploadSpeedBps)
                isQoSEnabled(config.shouldRunQosTest)
                runner.testUUID?.let {
                    onClientReady(it, stateRecorder.loopLocalUuid)
                }
                if (hasErrors) {
                    client.onMeasurementError()
                }

                if (qosProgressMap.isNotEmpty()) {
                    onQoSTestProgressUpdated(qosTasksPassed, qosTasksTotal, qosProgressMap)
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

        override val pingNanos: Long
            get() = this@MeasurementService.pingNanos

        override val loopModeState: LoopModeState
            get() = this@MeasurementService.loopModeState

        override val lastMeasurementSignalInfo: SignalStrengthInfo?
            get() = this@MeasurementService.lastMeasurementSignal

        override val isTestsRunning: Boolean
            get() = !((!config.loopModeEnabled && (this.measurementState == MeasurementState.IDLE || this.measurementState == MeasurementState.ERROR || this.measurementState == MeasurementState.ABORTED || this.measurementState == MeasurementState.FINISH)) || (config.loopModeEnabled && ((this.loopModeState == LoopModeState.IDLE && this.loopLocalUUID == null) || this.loopModeState == LoopModeState.FINISHED || this.loopModeState == LoopModeState.CANCELLED)))

        override val testUUID: String?
            get() = runner.testUUID

        //        override val loopUUID: String?
//            get() = stateRecorder.loopModeRecord?.uuid
//
        override val loopLocalUUID: String?
            get() = stateRecorder.loopLocalUuid

        override fun startTests() {
            this@MeasurementService.startTests()
        }

        override fun stopTests() {
            this@MeasurementService.stopTests()
        }
    }

    /**
     * This aggregates all listeners for test updates
     */
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

        override fun onMeasurementError() {
            clients.forEach {
                it.onMeasurementError()
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

        override fun onPingChanged(pingNanos: Long) {
            clients.forEach {
                it.onPingChanged(pingNanos)
            }
        }

        override fun onClientReady(testUUID: String, loopLocalUUID: String?) {
            clients.forEach {
                it.onClientReady(testUUID, loopLocalUUID)
            }
        }

        override fun isQoSEnabled(enabled: Boolean) {
            clients.forEach {
                it.isQoSEnabled(enabled)
            }
        }

        override fun onSubmitted() {
            if (config.loopModeEnabled) {
                if (stateRecorder.loopTestCount >= config.loopModeNumberOfTests && config.loopModeNumberOfTests != 0) {
                    clients.forEach {
                        it.onSubmitted()
                    }
                }
            } else {
                clients.forEach {
                    it.onSubmitted()
                }
            }
        }

        override fun onSubmissionError(exception: HandledException) {
            if (config.loopModeEnabled) {
                if (config.loopModeNumberOfTests >= config.loopModeNumberOfTests && config.loopModeNumberOfTests != 0) {
                    clients.forEach {
                        it.onSubmissionError(exception)
                    }
                }
            } else {
                clients.forEach {
                    it.onSubmissionError(exception)
                }
            }
        }

        override fun onQoSTestProgressUpdated(tasksPassed: Int, tasksTotal: Int, progressMap: Map<QoSTestResultEnum, Int>) {
            clients.forEach {
                it.onQoSTestProgressUpdated(tasksPassed, tasksTotal, progressMap)
            }
        }

        override fun onLoopCountDownTimer(timePassedMillis: Long, timeTotalMillis: Long) {
            clients.forEach {
                it.onLoopCountDownTimer(timePassedMillis, timeTotalMillis)
            }
        }

        override fun onLoopDistanceChanged(distancePassed: Int, distanceTotal: Int, locationAvailable: Boolean) {
            clients.forEach {
                it.onLoopDistanceChanged(distancePassed, distanceTotal, locationAvailable)
            }
        }

        override fun onMeasurementCancelled() {
            clients.forEach {
                it.onMeasurementCancelled()
            }
        }
    }

    companion object {

        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_LOOP_FINISHED_ID = 2
        private const val NOTIFICATION_UPDATE_INTERVAL_MS = 700

        private const val MEASUREMENT_INACTIVITY_THRESHOLD_SECONDS: Long = 120
        private const val MEASUREMENT_INACTIVITY_CHECKER_PERIOD_SECONDS: Long = 1

        private const val ACTION_START_TESTS = "KEY_START_TESTS"
        private const val ACTION_STOP_TESTS = "KEY_STOP_TESTS"

        fun startTests(context: Context) {
            val intent = intent(context)
            intent.action = ACTION_START_TESTS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTestsIntent(context: Context): Intent = Intent(context, MeasurementService::class.java).apply {
            action = ACTION_STOP_TESTS
        }

        fun intent(context: Context) = Intent(context, MeasurementService::class.java)
    }
}