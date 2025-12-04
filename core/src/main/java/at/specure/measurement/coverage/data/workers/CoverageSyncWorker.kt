package at.specure.measurement.coverage.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import at.rmbt.util.exception.NoConnectionException
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.di.CoreInjector
import timber.log.Timber
import javax.inject.Inject

/**
 * Worker responsible for retrying failed fence uploads and cleaning up old sessions.
 * Runs automatically when internet is available or when manually scheduled.
 */
class CoverageSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @Inject
    lateinit var repository: SignalMeasurementRepository

    override suspend fun doWork(): Result {
        // Inject repository via custom DI
        CoreInjector.inject(this)

        return try {
            Timber.d("CoverageSyncWorker started")

            try {
                repository.retrySendFences()
            } catch (e: NoConnectionException) {
                Timber.w("No connection for session retry sending — retry later")
                return Result.retry()
            } catch (e: Exception) {
                Timber.e(e, "Error retried sending fences")
            }

            // Always clean old data if deletable
            repository.removeOldFencesAndSessions()

            Timber.d("CoverageSyncWorker finished successfully")
            Result.success()

        } catch (e: NoConnectionException) {
            Timber.w("CoverageSyncWorker — no connection, retry triggered")
            Result.retry()

        } catch (e: Exception) {
            Timber.e(e, "CoverageSyncWorker failed")
            Result.failure()
        }
    }
}
