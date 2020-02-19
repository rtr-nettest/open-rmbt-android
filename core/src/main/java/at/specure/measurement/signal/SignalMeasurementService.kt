package at.specure.measurement.signal

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.lifecycle.LiveData
import at.specure.di.CoreInjector
import at.specure.di.NotificationProvider
import at.specure.util.CustomLifecycleService
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SignalMeasurementService : CustomLifecycleService() {

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

        processor.bind(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.v("SERVICE onStartCommand")
        if (intent?.action == ACTION_STOP) {
            stopMeasurement()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? {
        super.onBind(intent)
        Timber.v("SERVICE onBind")
        return producer
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.v("SERVICE onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.v("SERVICE onDestroy")
    }

    private fun startMeasurement() {
        if (!wakeLock.isHeld) {
            wakeLock.acquire(TimeUnit.HOURS.toMillis(8))
        }
        processor.startMeasurement()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent(this))
        } else {
            startService(intent(this))
        }

        startForeground(NOTIFICATION_ID, notificationProvider.signalMeasurementService(stopIntent(this))) // TODO
    }

    private fun stopMeasurement() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        processor.stopMeasurement()
        stopForeground(true)
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

        override fun startMeasurement() {
            this@SignalMeasurementService.startMeasurement()
        }

        override fun stopMeasurement() {
            this@SignalMeasurementService.stopMeasurement()
        }

        override fun pauseMeasurement() {
            processor.pauseMeasurement()
        }

        override fun resumeMeasurement() {
            processor.resumeMeasurement()
        }
    }

    companion object {

        private const val NOTIFICATION_ID = 2
        private const val ACTION_STOP = "KEY_ACTION_STOP"

        private fun stopIntent(context: Context): Intent = Intent(context, SignalMeasurementService::class.java).setAction(ACTION_STOP)

        fun intent(context: Context) = Intent(context, SignalMeasurementService::class.java)
    }
}