package at.specure.worker.request

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import at.rmbt.util.exception.NoConnectionException
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.di.CoreInjector
import at.specure.util.exception.DataMissingException
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import javax.inject.Inject

class CoverageMeasurementWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    @Inject
    lateinit var repository: SignalMeasurementRepository

    override suspend fun doWork(): Result {
        CoreInjector.inject(this)

        val coverageSessionId = inputData.getString(KEY_SESSION_ID) ?: throw DataMissingException("coverage sessionId is missing in worker")

        var result = Result.failure()
        repository.registerCoverageMeasurement(coverageSessionId)
            .catch { e ->
                if (e is NoConnectionException) {
                    emit(false)
                } else {
                    Timber.e(e, "Getting coverage record error")
                    emit(true)
                }
            }
            .collect {
                result = if (it) Result.success() else Result.retry()
            }

        return result
    }

    companion object {

        private const val KEY_SESSION_ID = "KEY_SESSION_ID"

        fun getData(coverageSessionId: String) = Data.Builder().apply {
            putString(KEY_SESSION_ID, coverageSessionId)
        }.build()
    }
}