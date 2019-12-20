package at.specure.worker.request

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import at.specure.data.CoreDatabase
import at.specure.data.repository.ResultsRepository
import at.specure.di.CoreInjector
import at.specure.util.exception.DataMissingException
import timber.log.Timber
import javax.inject.Inject

const val KEY_TEST_UUID = "key_test_uuid"
const val ATTEMPTS_LIMIT = 10

class SendDataWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    @Inject
    lateinit var repository: ResultsRepository
    @Inject
    lateinit var db: CoreDatabase

    override fun doWork(): Result {
        CoreInjector.inject(this)

        Timber.d("Delayed submission start")
        val testUUID = inputData.getString(KEY_TEST_UUID) ?: throw DataMissingException("No testUUID passed")

        repository.updateSubmissionsCounter(testUUID)
        val response = repository.sendTestResults(testUUID)
        with(response) {
            return if (ok) {
                Timber.d("Delayed submission success")
                Result.success()
            } else {
                val submissionsCount = db.testDao().getSubmissionsRetryCount(testUUID)
                if (submissionsCount != null && submissionsCount < ATTEMPTS_LIMIT) {
                    Timber.d("Delayed submission retry")
                    Result.retry()
                } else {
                    Timber.d("Delayed submission failure after $submissionsCount attempts")
                    Result.failure()
                }
            }
        }
    }
}