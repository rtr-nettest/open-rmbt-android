package at.specure.measurement.signal

import at.specure.config.Config
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.lifecycle.LiveData
import at.specure.di.CoreInjector
import at.specure.di.NotificationProvider
import at.rmbt.client.control.data.SignalMeasurementType
import at.specure.util.CustomLifecycleService
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SignalMeasurementService : CustomLifecycleService() {

    @Inject
    lateinit var config: Config

    @Inject
    lateinit var notificationProvider: NotificationProvider

    @Inject
    lateinit var processor: SignalMeasurementProcessor

    private val producer: Producer by lazy { Producer() }

    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        Timber.v("SERVICE onCreate")

        CoreInjector.inject(this)

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:RMBTSignalWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.v("SERVICE onStartCommand")
        when (intent?.action) {
            ACTION_STOP -> {
                stopMeasurement()
            }
        }

        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        super.onBind(intent)
        Timber.v("SERVICE onBind")
        return producer
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.v("SERVICE onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
        releaseWakeLock()
        Timber.v("SERVICE onDestroy")
    }

    private fun startMeasurement(signalMeasurementType: SignalMeasurementType) {
        acquireWakeLock()
        Timber.d("Starting coverage session SMS1")
        processor.startMeasurement(false, signalMeasurementType)

        startForegroundService(intent(this))

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            startForeground(NOTIFICATION_ID, notificationProvider.signalMeasurementService(null), FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notificationProvider.signalMeasurementService(null))
        }
    }

    private fun stopMeasurement() {
        releaseWakeLock()
        Timber.d("Stoping coverage session SMS1")
        processor.stopMeasurement(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (!wakeLock.isHeld) {
            wakeLock.acquire(TimeUnit.MINUTES.toMillis(config.signalMeasurementDurationMin.toLong()))
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }

    private inner class Producer : Binder(), SignalMeasurementProducer {

        override val isActive: Boolean
            get() = processor.isActive

        override val isPaused: Boolean
            get() = processor.isPaused

        override val activeStateLiveData: LiveData<Boolean>
            get() = processor.activeStateLiveData

        override val pausedStateLiveData: LiveData<Boolean>
            get() = processor.pausedStateLiveData

        override val signalMeasurementSessionIdLiveData: LiveData<String?>
            get() = processor.signalMeasurementSessionIdLiveData

        override val signalMeasurementSessionErrorLiveData: LiveData<Exception?>
            get() = processor.signalMeasurementSessionErrorLiveData

        override fun startMeasurement(unstoppable: Boolean, signalMeasurementType: SignalMeasurementType) {
            Timber.i("Signal measurement start unstoppable: $unstoppable")
            this@SignalMeasurementService.startMeasurement(signalMeasurementType)
        }

        override fun stopMeasurement(unstoppable: Boolean) {
            Timber.i("Signal measurement stop: $unstoppable")
            this@SignalMeasurementService.stopMeasurement()
        }
    }

    companion object {

        private const val NOTIFICATION_ID = 3
        private const val ACTION_STOP = "KEY_ACTION_STOP"

        fun stopIntent(context: Context): Intent = Intent(context, SignalMeasurementService::class.java).setAction(ACTION_STOP)

        fun intent(context: Context) = Intent(context, SignalMeasurementService::class.java)
    }
}