package at.specure.measurement.coverage.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import at.rmbt.util.exception.NoConnectionException
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.di.CoreInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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

    @Inject
    lateinit var  settings: CoverageMeasurementSettings

    override suspend fun doWork(): Result = try {
        // Inject repository via custom DI
        CoreInjector.inject(this)

        if (settings.signalMeasurementIsRunning) {
            Timber.d("CoverageSyncWorker not started because of running measurement")
            Result.retry()
        } else {
            Timber.d("CoverageSyncWorker started")
            withContext(Dispatchers.IO) {
                try {
                    repository.registerNotRegisteredMeasurementsWithSomeFences()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: NoConnectionException) {
                    Timber.w("No connection for session retry registering — retry later")
                    return@withContext Result.retry()
                } catch (e: Exception) {
                    Timber.e(e, "Error retried registering measurement")
                }

                try {
                    repository.retrySendFences()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: NoConnectionException) {
                    Timber.w("No connection for session retry sending — retry later")
                    return@withContext Result.retry()
                } catch (e: Exception) {
                    Timber.e(e, "Error retried sending fences")
                }

                // Always clean old data if deletable
                repository.removeOldFencesAndSessions()
            }

            Timber.d("CoverageSyncWorker finished successfully")
            Result.success()
        }

    } catch (e: CancellationException) {
        throw e

    } catch (e: NoConnectionException) {
        Timber.w("CoverageSyncWorker — no connection, retry triggered")
        Result.retry()

    } catch (e: Exception) {
        Timber.e(e, "CoverageSyncWorker failed")
        Result.failure()
    }
}
