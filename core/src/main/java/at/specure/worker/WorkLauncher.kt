package at.specure.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import at.specure.worker.request.SendDataWorker
import at.specure.worker.request.SettingsWorker
import java.util.concurrent.TimeUnit

private const val WAITING_TIME_BETWEEN_REQUEST = 10L
private const val SETTINGS_REQUEST_WORK_NAME = "SETTINGS_REQUEST_WORK_NAME"
private const val DELAYED_SUBMISSION_WORK_NAME = "DELAYED_SUBMISSION_WORK_NAME"

const val KEY_TEST_UUID = "key_test_uuid"

object WorkLauncher {

    fun enqueueSettingsRequest(context: Context) {
        val workRequest = OneTimeWorkRequest.Builder(SettingsWorker::class.java)
            .setConstraints(getWorkerConstraints())
            .addTag(SETTINGS_REQUEST_WORK_NAME).setBackoffCriteria(BackoffPolicy.LINEAR, WAITING_TIME_BETWEEN_REQUEST, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SETTINGS_REQUEST_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
    }

    fun enqueueDelayedDataSaveRequest(context: Context, testUUID: String) {
        val inputData = Data.Builder().putString(KEY_TEST_UUID, testUUID).build()

        val workRequest = OneTimeWorkRequest.Builder(SendDataWorker::class.java)
            .setInputData(inputData)
            .setConstraints(getWorkerConstraints())
            .addTag(DELAYED_SUBMISSION_WORK_NAME)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WAITING_TIME_BETWEEN_REQUEST, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(DELAYED_SUBMISSION_WORK_NAME, ExistingWorkPolicy.REPLACE, workRequest)
    }

    private fun getWorkerConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}