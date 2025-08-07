package at.specure.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import at.specure.worker.request.CoverageMeasurementWorker
import at.specure.worker.request.SendDataWorker
import at.specure.worker.request.SettingsWorker
import at.specure.worker.request.SignalMeasurementChunkWorker
import at.specure.worker.request.SignalMeasurementInfoWorker
import java.util.concurrent.TimeUnit

private const val WAITING_TIME_BETWEEN_REQUEST = 10L
private const val SETTINGS_REQUEST_WORK_NAME = "SETTINGS_REQUEST_WORK_NAME"
private const val DELAYED_SUBMISSION_WORK_NAME = "DELAYED_SUBMISSION_WORK_NAME"
private const val SERVER_MEASUREMENT_INFO = "SERVER_MEASUREMENT_INFO"
private const val SIGNAL_MEASUREMENT_CHUNK = "SIGNAL_MEASUREMENT_CHUNK"
private const val COVERAGE_MEASUREMENT_REQUEST = "COVERAGE_MEASUREMENT_REQUEST"
private const val COVERAGE_MEASUREMENT_RESULT = "COVERAGE_MEASUREMENT_RESULT" //TODO: add coverage result request

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

    @Deprecated(message = "Use enqueueCoverageMeasurementRequest instead")
    fun enqueueSignalMeasurementInfoRequest(context: Context, measurementId: String) {

        val request = OneTimeWorkRequest.Builder(SignalMeasurementInfoWorker::class.java)
            .setInputData(SignalMeasurementInfoWorker.getData(measurementId))
            .setConstraints(getWorkerConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WAITING_TIME_BETWEEN_REQUEST, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(SERVER_MEASUREMENT_INFO, ExistingWorkPolicy.REPLACE, request)
    }

    fun enqueueSignalMeasurementChunkRequest(context: Context, chunkId: String) {

        val request = OneTimeWorkRequest.Builder(SignalMeasurementChunkWorker::class.java)
            .setInputData(SignalMeasurementChunkWorker.getData(chunkId))
            .setConstraints(getWorkerConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WAITING_TIME_BETWEEN_REQUEST, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(SIGNAL_MEASUREMENT_CHUNK, ExistingWorkPolicy.REPLACE, request)
    }

    fun enqueueCoverageMeasurementRequest(context: Context, sessionId: String) {

        val request = OneTimeWorkRequest.Builder(CoverageMeasurementWorker::class.java)
            .setInputData(CoverageMeasurementWorker.getData(sessionId))
            .setConstraints(getWorkerConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WAITING_TIME_BETWEEN_REQUEST, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(COVERAGE_MEASUREMENT_REQUEST, ExistingWorkPolicy.REPLACE, request)
    }

    // TODO: add enqueue coverage request and send coverage result request

    private fun getWorkerConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}