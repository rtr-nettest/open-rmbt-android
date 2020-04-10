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
import at.rtr.rmbt.android.ui.activity.LoopFinishedActivity
import at.rtr.rmbt.android.util.timeString
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.LoopModeState
import at.specure.di.NotificationProvider
import at.specure.measurement.MeasurementState

class NotificationProviderImpl(private val context: Context) : NotificationProvider {

    override fun measurementServiceNotification(
        progress: Int,
        state: MeasurementState,
        skipQoSTests: Boolean,
        loopModeRecord: LoopModeRecord?,
        loopModeTestsCount: Int,
        cancellationIntent: Intent
    ): Notification {
        return if (loopModeRecord == null) {
            createMeasurementNotification(progress, state, skipQoSTests, context.getString(R.string.notification_test_title), cancellationIntent)
        } else {
            if (loopModeRecord.status == LoopModeState.IDLE) {
                createMeasurementNotification(progress, state, skipQoSTests, context.getString(R.string.notification_test_title), cancellationIntent)
            } else {
                createMeasurementNotification(
                    progress,
                    state,
                    skipQoSTests,
                    context.getString(R.string.notification_loop_mode_title_running, loopModeRecord.testsPerformed, loopModeTestsCount),
                    cancellationIntent
                )
            }
        }
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

    private fun createMeasurementNotification(
        progress: Int,
        state: MeasurementState,
        skipQoSTests: Boolean,
        titleText: String,
        cancellationIntent: Intent
    ): Notification {
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

        val totalProgress = if (skipQoSTests) 400 else 500
        val progressIntermediate = ((stateProgress + progress) / totalProgress.toFloat()) * 100

        val intent = PendingIntent.getActivity(context, 0, Intent(context, HomeActivity::class.java), 0)
        val actionIntent = PendingIntent.getService(context, 0, cancellationIntent, 0)
        val action = NotificationCompat.Action.Builder(0, context.getString(R.string.text_cancel_measurement), actionIntent).build()

        return NotificationCompat.Builder(context, measurementChannelId())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(action)
            .setContentText(context.getString(textResource))
            .setContentIntent(intent)
            .setContentTitle(titleText)
            .setProgress(100, progressIntermediate.toInt(), false)
            .build()!!
    }

    override fun loopCountDownNotification(
        timePassedMillis: Long,
        metersPassed: Int,
        metersRequired: Int,
        testsPassed: Int,
        testsCount: Int,
        cancellationIntent: Intent,
        locationAvailable: Boolean
    ): Notification {

        val locationString = if (locationAvailable) {
            String.format("  %dm left  ", metersRequired - metersPassed)
        } else {
            "  No GPS  "
        }

        val text = String.format(
            "%s%s(%d/%d)",
            timePassedMillis.timeString(),
            locationString,
            testsPassed,
            testsCount
        )

        val intent = PendingIntent.getActivity(context, 0, Intent(context, HomeActivity::class.java), 0)
        val actionIntent = PendingIntent.getService(context, 0, cancellationIntent, 0)
        val action = NotificationCompat.Action.Builder(0, context.getString(R.string.text_cancel_measurement), actionIntent).build()

        return NotificationCompat.Builder(context, measurementChannelId())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_notification)
            .addAction(action)
            .setContentText(text)
            .setContentIntent(intent)
            .setContentTitle(context.getString(R.string.notification_loop_mode_title_active))
            .build()!!
    }

    override fun signalMeasurementService(stopMeasurementIntent: Intent): Notification {
        val intent = PendingIntent.getActivity(context, 0, Intent(context, HomeActivity::class.java), 0)
        val actionIntent = PendingIntent.getService(context, 0, stopMeasurementIntent, 0)
        val action = NotificationCompat.Action.Builder(0, context.getString(R.string.text_stop_measurement), actionIntent).build()

        return NotificationCompat.Builder(context, measurementChannelId())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_cloud_upload)
            .addAction(action)
            .setContentText(context.getString(R.string.notification_signal_test_text))
            .setContentIntent(intent)
            .setContentTitle(context.getString(R.string.notification_signal_test_title))
            .build()!!
    }

    override fun loopModeFinishedNotification(): Notification {
        val intent = PendingIntent.getActivity(context, 0, Intent(context, LoopFinishedActivity::class.java), 0)
        return NotificationCompat.Builder(context, measurementChannelId())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(intent)
            .setContentTitle(context.getString(R.string.notification_loop_mode_finished_title))
            .build()!!
    }
}