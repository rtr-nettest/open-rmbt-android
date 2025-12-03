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

        val coverageLocalMeasurementId = inputData.getString(KEY_COVERAGE_MEASUREMENT_LOCAL_ID) ?: throw DataMissingException("coverage sessionId is missing in worker")

        var result = Result.failure()
        repository.registerCoverageMeasurement(localMeasurementId = coverageLocalMeasurementId)
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

        private const val KEY_COVERAGE_MEASUREMENT_LOCAL_ID = "KEY_COVERAGE_MEASUREMENT_LOCAL_ID"

        fun getData(coverageLocalMeasurementId: String) = Data.Builder().apply {
            putString(KEY_COVERAGE_MEASUREMENT_LOCAL_ID, coverageLocalMeasurementId)
        }.build()
    }
}