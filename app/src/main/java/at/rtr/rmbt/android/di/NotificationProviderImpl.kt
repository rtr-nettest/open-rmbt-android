package at.rtr.rmbt.android.di

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import at.rtr.rmbt.android.BuildConfig
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.activity.HomeActivity
import at.specure.di.NotificationProvider
import at.specure.measurement.MeasurementState

class NotificationProviderImpl(private val context: Context) : NotificationProvider {

    override fun measurementServiceNotification(progress: Int, state: MeasurementState, skipQoSTests: Boolean): Notification {

        val textResource: Int
        val stateProgress = when (state) {
            MeasurementState.PING -> {
                textResource = R.string.label_ping
                100
            }
            MeasurementState.DOWNLOAD -> {
                textResource = R.string.label_download
                200
            }
            MeasurementState.UPLOAD -> {
                textResource = R.string.label_upload
                300
            }
            MeasurementState.QOS -> {
                textResource = R.string.label_qos
                400
            }
            else -> {
                textResource = R.string.label_init
                0
            }
        }

        val totalProgress = if (skipQoSTests) 300 else 400
        val progressIntermediate = ((stateProgress + progress) / totalProgress.toFloat()) * 100

        val intent = PendingIntent.getActivity(context, 0, Intent(context, HomeActivity::class.java), 0)

        return NotificationCompat.Builder(context, measurementChannelId())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_z)
            .setContentText(context.getString(textResource))
            .setContentIntent(intent)
            .setContentTitle(context.getString(R.string.notification_test_title))
            .setTicker(context.getString(R.string.notification_test_ticker))
            .setProgress(100, progressIntermediate.toInt(), false)
            .build()!!
    }

    private fun measurementChannelId(): String {
        val channelId = BuildConfig.APPLICATION_ID + "_measurement_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationChannel = NotificationChannel(channelId, "Measurements", NotificationManager.IMPORTANCE_LOW)
            notificationChannel.description = "Channel for foreground notifications while tests are running"
            notificationManager.createNotificationChannel(notificationChannel)
        }
        return channelId
    }
}