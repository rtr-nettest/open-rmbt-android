package at.specure.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import at.specure.worker.request.SettingsWorker
import java.util.concurrent.TimeUnit

private const val WAITING_TIME_BETWEEN_REQUEST = 10L
private const val SETTINGS_REQUEST_WORK_NAME = "SETTINGS_REQUEST_WORK_NAME"

object WorkLauncher {

    fun enqueueSettingsRequest(context: Context) {
        val workRequest = OneTimeWorkRequest.Builder(SettingsWorker::class.java)
            .setConstraints(getWorkerConstraints())
            .addTag(SETTINGS_REQUEST_WORK_NAME).setBackoffCriteria(BackoffPolicy.LINEAR, WAITING_TIME_BETWEEN_REQUEST, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SETTINGS_REQUEST_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
    }

    private fun getWorkerConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}