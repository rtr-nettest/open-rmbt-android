package at.specure.worker.request

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import at.specure.data.CoreDatabase
import at.specure.data.dao.TEST_SUBMISSION_GIVE_UP_MILLIS
import at.specure.data.dao.TEST_SUBMISSION_MAX_RETRY_COUNT
import at.specure.data.repository.ResultsRepository
import at.specure.di.CoreInjector
import at.specure.util.exception.DataMissingException
import timber.log.Timber
import javax.inject.Inject

const val KEY_TEST_UUID = "key_test_uuid"

class SendDataWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    @Inject
    lateinit var repository: ResultsRepository
    @Inject
    lateinit var db: CoreDatabase

    override fun doWork(): Result {
        CoreInjector.inject(this)


        val testUUID = inputData.getString(KEY_TEST_UUID) ?: throw DataMissingException("No testUUID passed")
        Timber.d("Delayed submission start of UUD $testUUID")

        repository.updateSubmissionsCounter(testUUID)
        val response = repository.sendTestResults(testUUID)
        with(response) {
            return if (ok) {
                Timber.d("Delayed submission success $testUUID")
                Result.success()
            } else {
                // Give up (stop retrying) after the retry budget or the max age is reached, so a bad
                // coverage area cannot make a result retry forever. Rescheduling here uses the 20-min
                // LINEAR backoff configured in WorkLauncher, i.e. at most ~3 attempts per hour.
                val submissionsCount = db.testDao().getSubmissionsRetryCount(testUUID) ?: 0
                val testTimeMillis = db.testDao().get(testUUID)?.testTimeMillis ?: 0
                val tooOld = testTimeMillis > 0 &&
                    System.currentTimeMillis() - testTimeMillis > TEST_SUBMISSION_GIVE_UP_MILLIS
                if (submissionsCount < TEST_SUBMISSION_MAX_RETRY_COUNT && !tooOld) {
                    Timber.d("Delayed submission retry of UUID $testUUID (attempt $submissionsCount)")
                    Result.retry()
                } else {
                    Timber.d("Delayed submission gave up for $testUUID after $submissionsCount attempts (tooOld=$tooOld)")
                    Result.failure()
                }
            }
        }
    }
}