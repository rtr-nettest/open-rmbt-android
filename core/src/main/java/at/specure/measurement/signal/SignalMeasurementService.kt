package at.specure.measurement.signal

import at.specure.config.Config
import android.app.AlarmManager
import android.app.PendingIntent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private var alarmManager: AlarmManager? = null

    private lateinit var wakeLock: PowerManager.WakeLock

    private lateinit var alarmStopPendingIntent: PendingIntent

    var endTime: Long? = null
    var isUnstoppable: Boolean =
        false // during the loop mode should be this value set to true, then alarm will not stop signal measurement, but instead will set shouldEndAfterLoopMode to true
    var shouldEndAfterLoopMode: Boolean =
        false // after end of the loop mode this will try to continue in signal measurement, but when time runs out it should not be resumed anymore

    override fun onCreate() {
        super.onCreate()
        Timber.v("SERVICE onCreate")

        CoreInjector.inject(this)

        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:RMBTSignalWakeLock")
        alarmStopPendingIntent = PendingIntent.getService(this, 0, alarmStopIntent(this), PendingIntent.FLAG_IMMUTABLE)
        processor.bind(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.v("SERVICE onStartCommand")
        when (intent?.action) {
            ACTION_STOP -> {
                stopMeasurement()
            }
            ACTION_ALARM_STOP -> {
                if (!isUnstoppable) {
                    endTime = null
                    shouldEndAfterLoopMode = false
                    Timber.i("Signal measurement stopping on alarm")
                    stopMeasurement()
                } else {
                    Timber.i("Signal measurement try to stop on alarm but it is unstoppable, set to stop later after it will be not unstoppable")
                    shouldEndAfterLoopMode = true
                }
            }
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

    private fun startMeasurement(signalMeasurementType: SignalMeasurementType) {
        acquireWakeLock()
        Timber.d("Starting coverage session SMS1")
        processor.startMeasurement(false, signalMeasurementType)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent(this))
        } else {
            startService(intent(this))
        }

        shouldEndAfterLoopMode = false
        isUnstoppable = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            startForeground(NOTIFICATION_ID, notificationProvider.signalMeasurementService(null), FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notificationProvider.signalMeasurementService(null))
        }
    }

    private fun stopMeasurement() {
        releaseWakeLock()
        cancelSignalMeasurementStopAlarm()
        Timber.i("Signal measurement stopped")
        processor.stopMeasurement(false)
        stopForeground(true)
    }

    private fun setEndAlarm() {
        val durationInMinutes = config.signalMeasurementDurationMin.toLong()
        val currentTimeMillis = System.currentTimeMillis()
        if ((durationInMinutes > 0) && (endTime?.compareTo(currentTimeMillis) != 1)) {
            endTime = currentTimeMillis + TimeUnit.MINUTES.toMillis(durationInMinutes)
            alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager

            cancelSignalMeasurementStopAlarm()

            endTime?.let {
                val formatter = SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS", Locale.getDefault())
                val dateString = formatter.format(Date(endTime!!))

                Timber.i("Signal measurement will end by alarm at: $dateString")
                alarmManager?.set(AlarmManager.RTC_WAKEUP, endTime!!, alarmStopPendingIntent)
            }
        }
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

    private fun cancelSignalMeasurementStopAlarm() {
        if (alarmStopPendingIntent != null && alarmManager != null) {
            Timber.i("Signal measurement alarm cancelled")
            alarmManager?.cancel(alarmStopPendingIntent)
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
            this@SignalMeasurementService.isUnstoppable = unstoppable
            Timber.i("Signal measurement start unstoppable: $unstoppable")
            this@SignalMeasurementService.startMeasurement(signalMeasurementType)
        }

        override fun stopMeasurement(unstoppable: Boolean) {
            this@SignalMeasurementService.isUnstoppable = unstoppable
            Timber.i("Signal measurement stop: $unstoppable")
            this@SignalMeasurementService.stopMeasurement()
        }

        override fun pauseMeasurement(unstoppable: Boolean) {
            this@SignalMeasurementService.isUnstoppable = unstoppable
            Timber.i("Signal measurement pause unstoppable: $unstoppable")
            processor.pauseMeasurement(unstoppable)
        }

        override fun resumeMeasurement(unstoppable: Boolean) {
            this@SignalMeasurementService.isUnstoppable = unstoppable
            Timber.i("Signal measurement resume unstoppable: $unstoppable")
            processor.resumeMeasurement(unstoppable)
            if (!isUnstoppable && shouldEndAfterLoopMode) {
                this@SignalMeasurementService.endTime = null
                this@SignalMeasurementService.shouldEndAfterLoopMode = false
                Timber.i("Signal measurement stopping on alarm delayed")
                stopMeasurement()
            }
        }

        override fun setEndAlarm() {
            Timber.i("Signal measurement trying to set alarm")
            this@SignalMeasurementService.setEndAlarm()
        }
    }

    companion object {

        private const val NOTIFICATION_ID = 3
        private const val ACTION_STOP = "KEY_ACTION_STOP"
        private const val ACTION_ALARM_STOP = "KEY_ACTION_ALARM_STOP"

        fun stopIntent(context: Context): Intent = Intent(context, SignalMeasurementService::class.java).setAction(ACTION_STOP)

        private fun alarmStopIntent(context: Context): Intent = Intent(context, SignalMeasurementService::class.java).setAction(ACTION_ALARM_STOP)

        fun intent(context: Context) = Intent(context, SignalMeasurementService::class.java)
    }
}